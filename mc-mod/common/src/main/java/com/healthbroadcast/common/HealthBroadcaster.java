package com.healthbroadcast.common;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * UDP 广播器 - 平台无关的共享代码
 * 支持 MC 1.12 ~ 最新版本
 */
public class HealthBroadcaster {
    
    private static final String UDP_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 39571;
    
    private static HealthBroadcaster instance;
    
    private DatagramSocket socket;
    private InetAddress address;
    private int port = DEFAULT_PORT;
    private boolean debugMode = true; // 调试模式
    
    // 状态缓存 - 避免重复发送
    private float lastHealth = -1;
    private float lastMaxHealth = -1;
    
    private HealthBroadcaster() {
        initSocket();
    }
    
    private void initSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = new DatagramSocket();
            address = InetAddress.getByName(UDP_HOST);
            log("UDP socket created successfully, target: " + UDP_HOST + ":" + port);
        } catch (Exception e) {
            System.err.println("[HealthBroadcast] Failed to create UDP socket: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static synchronized HealthBroadcaster getInstance() {
        if (instance == null) {
            instance = new HealthBroadcaster();
        }
        return instance;
    }
    
    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    private void log(String message) {
        if (debugMode) {
            System.out.println("[HealthBroadcast] " + message);
        }
    }
    
    /**
     * 设置 UDP 端口
     */
    public void setPort(int port) {
        if (port > 0 && port <= 65535) {
            if (this.port != port) {
                this.port = port;
                log("UDP port changed to: " + port);
                // 重新初始化 socket
                initSocket();
            }
        }
    }
    
    /**
     * 获取当前端口
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 强制发送血量数据（忽略缓存）
     */
    public boolean forceHealthUpdate(float health, float maxHealth) {
        lastHealth = health;
        lastMaxHealth = maxHealth;
        
        HealthData data = new HealthData();
        data.type = "health";
        data.health = health;
        data.maxHealth = maxHealth;
        data.percentage = maxHealth > 0 ? health / maxHealth : 0;
        data.timestamp = System.currentTimeMillis();
        
        log("Force sending health: " + health + "/" + maxHealth);
        return send(data.toJson());
    }
    
    /**
     * 发送血量数据 (仅在变化时发送)
     * @return true 如果发送了数据
     */
    public boolean sendHealthUpdate(float health, float maxHealth) {
        // 使用阈值比较，避免浮点数精度问题
        if (Math.abs(health - lastHealth) < 0.01f && Math.abs(maxHealth - lastMaxHealth) < 0.01f) {
            return false;
        }
        
        lastHealth = health;
        lastMaxHealth = maxHealth;
        
        HealthData data = new HealthData();
        data.type = "health";
        data.health = health;
        data.maxHealth = maxHealth;
        data.percentage = maxHealth > 0 ? health / maxHealth : 0;
        data.timestamp = System.currentTimeMillis();
        
        log("Sending health update: " + health + "/" + maxHealth);
        return send(data.toJson());
    }
    
    /**
     * 发送受伤事件
     */
    public boolean sendDamageEvent(float damage, float healthAfter, float maxHealth, String source) {
        DamageData data = new DamageData();
        data.type = "damage";
        data.damage = damage;
        data.health = healthAfter;
        data.maxHealth = maxHealth;
        data.percentage = maxHealth > 0 ? healthAfter / maxHealth : 0;
        data.source = source != null ? source : "unknown";
        data.timestamp = System.currentTimeMillis();
        
        // 更新缓存
        lastHealth = healthAfter;
        lastMaxHealth = maxHealth;
        
        log("Sending damage event: " + damage + " from " + source + ", health now: " + healthAfter);
        return send(data.toJson());
    }
    
    /**
     * 发送死亡事件
     */
    public boolean sendDeathEvent(String source) {
        DeathData data = new DeathData();
        data.type = "death";
        data.source = source != null ? source : "unknown";
        data.timestamp = System.currentTimeMillis();
        
        lastHealth = 0;
        
        log("Sending death event, source: " + source);
        return send(data.toJson());
    }
    
    /**
     * 发送治疗事件
     */
    public boolean sendHealEvent(float amount, float healthAfter, float maxHealth) {
        HealData data = new HealData();
        data.type = "heal";
        data.amount = amount;
        data.health = healthAfter;
        data.maxHealth = maxHealth;
        data.percentage = maxHealth > 0 ? healthAfter / maxHealth : 0;
        data.timestamp = System.currentTimeMillis();
        
        lastHealth = healthAfter;
        lastMaxHealth = maxHealth;
        
        log("Sending heal event: +" + amount + ", health now: " + healthAfter);
        return send(data.toJson());
    }
    
    /**
     * 重置状态 (玩家断开/重生时调用)
     */
    public void reset() {
        lastHealth = -1;
        lastMaxHealth = -1;
        log("State reset");
    }
    
    private boolean send(String json) {
        if (socket == null || socket.isClosed()) {
            log("Socket is null or closed, attempting to reinitialize...");
            initSocket();
            if (socket == null || socket.isClosed()) {
                log("Failed to reinitialize socket");
                return false;
            }
        }
        
        try {
            byte[] buffer = json.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
            log("Sent UDP packet to " + UDP_HOST + ":" + port + " - " + json);
            return true;
        } catch (Exception e) {
            log("Failed to send UDP packet: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            log("Socket closed");
        }
    }
    
    // ========== 数据类 ==========
    
    public static class HealthData {
        public String type;
        public float health;
        public float maxHealth;
        public float percentage;
        public long timestamp;
        
        public String toJson() {
            return String.format(
                "{\"type\":\"%s\",\"health\":%.1f,\"maxHealth\":%.1f,\"percentage\":%.3f,\"timestamp\":%d}",
                type, health, maxHealth, percentage, timestamp
            );
        }
    }
    
    public static class DamageData {
        public String type;
        public float damage;
        public float health;
        public float maxHealth;
        public float percentage;
        public String source;
        public long timestamp;
        
        public String toJson() {
            String safeSource = source != null ? source.replace("\"", "\\\"") : "unknown";
            return String.format(
                "{\"type\":\"%s\",\"damage\":%.1f,\"health\":%.1f,\"maxHealth\":%.1f,\"percentage\":%.3f,\"source\":\"%s\",\"timestamp\":%d}",
                type, damage, health, maxHealth, percentage, safeSource, timestamp
            );
        }
    }
    
    public static class DeathData {
        public String type;
        public String source;
        public long timestamp;
        
        public String toJson() {
            String safeSource = source != null ? source.replace("\"", "\\\"") : "unknown";
            return String.format(
                "{\"type\":\"%s\",\"source\":\"%s\",\"timestamp\":%d}",
                type, safeSource, timestamp
            );
        }
    }
    
    public static class HealData {
        public String type;
        public float amount;
        public float health;
        public float maxHealth;
        public float percentage;
        public long timestamp;
        
        public String toJson() {
            return String.format(
                "{\"type\":\"%s\",\"amount\":%.1f,\"health\":%.1f,\"maxHealth\":%.1f,\"percentage\":%.3f,\"timestamp\":%d}",
                type, amount, health, maxHealth, percentage, timestamp
            );
        }
    }
}
