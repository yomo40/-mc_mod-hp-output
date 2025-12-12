package com.healthbroadcast.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    public static final ForgeConfigSpec.IntValue UDP_PORT;
    
    static {
        BUILDER.push("Health Broadcast Settings");
        
        UDP_PORT = BUILDER
            .comment("UDP port for broadcasting health data (1-65535)")
            .defineInRange("udpPort", 39571, 1, 65535);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "health-broadcast-client.toml");
    }
    
    public static int getPort() {
        return UDP_PORT.get();
    }
}
