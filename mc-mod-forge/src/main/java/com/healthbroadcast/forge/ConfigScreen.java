package com.healthbroadcast.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Forge 配置界面 - 允许玩家在游戏中修改 UDP 端口
 */
public class ConfigScreen extends Screen {
    
    private final Screen parent;
    private EditBox portField;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    
    public ConfigScreen(Screen parent) {
        super(Component.literal("Health Broadcast Settings"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 端口输入框
        portField = new EditBox(
            this.font,
            centerX - 50,
            centerY - 20,
            100,
            20,
            Component.literal("UDP Port")
        );
        portField.setValue(String.valueOf(Config.getPort()));
        portField.setMaxLength(5);
        this.addRenderableWidget(portField);
        
        // 保存按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("Save"),
            button -> saveConfig()
        ).bounds(centerX - 100, centerY + 20, 95, 20).build());
        
        // 返回按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            button -> onClose()
        ).bounds(centerX + 5, centerY + 20, 95, 20).build());
        
        // 重置按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("Reset (39571)"),
            button -> {
                portField.setValue("39571");
                statusMessage = "Reset to default";
                statusColor = 0xFFFF00;
            }
        ).bounds(centerX - 100, centerY + 50, 200, 20).build());
    }
    
    private void saveConfig() {
        try {
            int port = Integer.parseInt(portField.getValue().trim());
            
            if (port < 1 || port > 65535) {
                statusMessage = "Port must be 1-65535";
                statusColor = 0xFF5555;
                return;
            }
            
            // 更新配置并应用
            Config.UDP_PORT.set(port);
            HealthBroadcaster.getInstance().setPort(port);
            
            statusMessage = "Saved! Port: " + port;
            statusColor = 0x55FF55;
            
        } catch (NumberFormatException e) {
            statusMessage = "Invalid port number";
            statusColor = 0xFF5555;
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        
        // 标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 端口标签
        graphics.drawString(this.font, "UDP Port:", this.width / 2 - 100, this.height / 2 - 15, 0xAAAAAA);
        
        // 状态消息
        if (!statusMessage.isEmpty()) {
            graphics.drawCenteredString(this.font, statusMessage, this.width / 2, this.height / 2 + 80, statusColor);
        }
        
        // 说明
        graphics.drawCenteredString(this.font, "Data sent to 127.0.0.1:<port>", this.width / 2, this.height / 2 - 50, 0x888888);
        
        super.render(graphics, mouseX, mouseY, delta);
    }
    
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
