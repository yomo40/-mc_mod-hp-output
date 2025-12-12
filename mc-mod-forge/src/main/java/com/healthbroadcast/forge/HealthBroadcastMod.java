package com.healthbroadcast.forge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HealthBroadcastMod.MODID)
public class HealthBroadcastMod {
    
    public static final String MODID = "hp_output";
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthBroadcastMod.class);
    
    private float lastHealth = -1;
    private float lastMaxHealth = -1;
    private int tickCounter = 0;
    private static final int FORCE_UPDATE_INTERVAL = 100;
    
    public HealthBroadcastMod() {
        // Register config
        Config.register();
        
        // Register config screen (游戏内配置界面)
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (mc, parent) -> new ConfigScreen(parent)
            )
        );
        
        // Register mod event bus listener
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        
        // Register game event handlers
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("[HealthBroadcast] Mod constructor completed!");
    }
    
    private void onClientSetup(FMLClientSetupEvent event) {
        // Apply port from config
        HealthBroadcaster.getInstance().setPort(Config.getPort());
        LOGGER.info("[HealthBroadcast] Client setup complete! UDP port: " + Config.getPort());
        
        // Send test data
        event.enqueueWork(() -> {
            HealthBroadcaster.getInstance().forceHealth(20.0f, 20.0f);
            LOGGER.info("[HealthBroadcast] Sent initial test health data");
        });
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (lastHealth != -1) {
                HealthBroadcaster.getInstance().reset();
                lastHealth = -1;
                lastMaxHealth = -1;
                tickCounter = 0;
            }
            return;
        }
        
        // Update port from config
        HealthBroadcaster.getInstance().setPort(Config.getPort());
        
        Player player = mc.player;
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        
        tickCounter++;
        
        // 定期强制发送
        if (tickCounter >= FORCE_UPDATE_INTERVAL) {
            tickCounter = 0;
            HealthBroadcaster.getInstance().forceHealth(health, maxHealth);
            lastHealth = health;
            lastMaxHealth = maxHealth;
            return;
        }
        
        // 首次进入游戏
        if (lastHealth < 0) {
            HealthBroadcaster.getInstance().forceHealth(health, maxHealth);
            lastHealth = health;
            lastMaxHealth = maxHealth;
            return;
        }
        
        // 通过tick检测血量变化
        float healthDiff = health - lastHealth;
        
        if (healthDiff < -0.01f) {
            // 受伤了
            float damage = -healthDiff;
            String source = "unknown";
            try {
                if (player.getLastDamageSource() != null) {
                    source = player.getLastDamageSource().getMsgId();
                }
            } catch (Exception e) {
                // 忽略
            }
            HealthBroadcaster.getInstance().sendDamage(damage, health, source);
            
            // 检测死亡
            if (health <= 0 && lastHealth > 0) {
                HealthBroadcaster.getInstance().sendDeath(source);
            }
        } else if (healthDiff > 0.01f) {
            // 治疗了
            HealthBroadcaster.getInstance().sendHeal(healthDiff, health);
        } else {
            // 普通更新
            HealthBroadcaster.getInstance().sendHealth(health, maxHealth);
        }
        
        lastHealth = health;
        lastMaxHealth = maxHealth;
    }
    
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        if (event.getEntity() == mc.player) {
            String source = "unknown";
            try {
                source = event.getSource().getMsgId();
            } catch (Exception e) {
                // Ignore
            }
            
            float healthAfter = mc.player.getHealth() - event.getAmount();
            LOGGER.info("[HealthBroadcast] LivingDamageEvent: {} damage from {}", event.getAmount(), source);
            HealthBroadcaster.getInstance().sendDamage(
                event.getAmount(),
                Math.max(0, healthAfter),
                source
            );
        }
    }
    
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        if (event.getEntity() == mc.player) {
            String source = "unknown";
            try {
                source = event.getSource().getMsgId();
            } catch (Exception e) {
                // Ignore
            }
            
            LOGGER.info("[HealthBroadcast] LivingDeathEvent: {}", source);
            HealthBroadcaster.getInstance().sendDeath(source);
        }
    }
    
    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        if (event.getEntity() == player) {
            float healthAfter = player.getHealth() + event.getAmount();
            LOGGER.info("[HealthBroadcast] LivingHealEvent: +{}", event.getAmount());
            HealthBroadcaster.getInstance().sendHeal(
                event.getAmount(),
                Math.min(healthAfter, player.getMaxHealth())
            );
        }
    }
}
