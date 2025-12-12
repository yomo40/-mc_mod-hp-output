package com.healthbroadcast.forge;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP Broadcaster - Sends player health data via UDP
 * 支持 MC 1.12 ~ 最新版本
 */
public class HealthBroadcaster {
    
    private static final HealthBroadcaster INSTANCE = new HealthBroadcaster();
    private static final String UDP_HOST = "127.0.0.1";
    
    private DatagramSocket socket;
    private InetAddress address;
    private int port = 39571;
    private boolean debugMode = true;
    
    // State cache
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
    
    public static HealthBroadcaster getInstance() {
        return INSTANCE;
    }
    
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    private void log(String message) {
        if (debugMode) {
            System.out.println("[HealthBroadcast] " + message);
        }
    }
    
    public void setPort(int newPort) {
        if (newPort > 0 && newPort < 65536) {
            if (this.port != newPort) {
                this.port = newPort;
                log("UDP port changed to: " + newPort);
                initSocket();
            }
        }
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * Force send health data (ignore cache)
     */
    public boolean forceHealth(float health, float maxHealth) {
        lastHealth = health;
        lastMaxHealth = maxHealth;
        
        String json = String.format(
            "{\"type\":\"health\",\"health\":%.1f,\"maxHealth\":%.1f,\"percentage\":%.3f,\"timestamp\":%d}",
            health, maxHealth, maxHealth > 0 ? health / maxHealth : 0, System.currentTimeMillis()
        );
        
        log("Force sending health: " + health + "/" + maxHealth);
        return send(json);
    }
    
    /**
     * Send health data (only when changed)
     */
    public boolean sendHealth(float health, float maxHealth) {
        if (Math.abs(health - lastHealth) < 0.01f && Math.abs(maxHealth - lastMaxHealth) < 0.01f) {
            return false;
        }
        
        lastHealth = health;
        lastMaxHealth = maxHealth;
        
        String json = String.format(
            "{\"type\":\"health\",\"health\":%.1f,\"maxHealth\":%.1f,\"percentage\":%.3f,\"timestamp\":%d}",
            health, maxHealth, maxHealth > 0 ? health / maxHealth : 0, System.currentTimeMillis()
        );
        
        log("Sending health update: " + health + "/" + maxHealth);
        return send(json);
    }
    
    /**
     * Send damage event
     */
    public void sendDamage(float damage, float healthAfter, String source) {
        String safeSource = source != null ? source.replace("\"", "\\\"") : "unknown";
        float maxHealth = lastMaxHealth > 0 ? lastMaxHealth : 20.0f;
        String json = String.format(
            "{\"type\":\"damage\",\"damage\":%.1f,\"health\":%.1f,\"maxHealth\":%.1f,\"percentage\":%.3f,\"source\":\"%s\",\"timestamp\":%d}",
            damage, healthAfter, maxHealth, maxHealth > 0 ? healthAfter / maxHealth : 0, safeSource, System.currentTimeMillis()
        );
        log("Sending damage event: " + damage + " from " + source);
        send(json);
        
        lastHealth = healthAfter;
    }
    
    /**
     * Send death event
     */
    public void sendDeath(String source) {
        String safeSource = source != null ? source.replace("\"", "\\\"") : "unknown";
        String json = String.format(
            "{\"type\":\"death\",\"source\":\"%s\",\"timestamp\":%d}",
            safeSource, System.currentTimeMillis()
        );
        log("Sending death event: " + source);
        send(json);
        reset();
    }
    
    /**
     * Send heal event
     */
    public void sendHeal(float amount, float healthAfter) {
        float maxHealth = lastMaxHealth > 0 ? lastMaxHealth : 20.0f;
        String json = String.format(
            "{\"type\":\"heal\",\"amount\":%.1f,\"health\":%.1f,\"maxHealth\":%.1f,\"percentage\":%.3f,\"timestamp\":%d}",
            amount, healthAfter, maxHealth, maxHealth > 0 ? healthAfter / maxHealth : 0, System.currentTimeMillis()
        );
        log("Sending heal event: +" + amount);
        send(json);
        
        lastHealth = healthAfter;
    }
    
    /**
     * Reset state
     */
    public void reset() {
        lastHealth = -1;
        lastMaxHealth = -1;
        log("State reset");
    }
    
    private boolean send(String message) {
        if (socket == null || socket.isClosed()) {
            log("Socket is null or closed, reinitializing...");
            initSocket();
            if (socket == null || socket.isClosed()) {
                log("Failed to reinitialize socket");
                return false;
            }
        }
        
        try {
            byte[] data = message.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            log("Sent UDP packet to " + UDP_HOST + ":" + port + " - " + message);
            return true;
        } catch (Exception e) {
            log("Failed to send UDP packet: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
