package com.jnhprojects.mcheli.client.screen;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.entity.UavStationEntity;
import com.jnhprojects.mcheli.menu.UavStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The UAV station screen, {@code MCH_GuiUavStation}: the slot for the drone, the launch offset
 * per axis with its buttons, and Continue to take the last drone flown from here again.
 */
public class UavStationScreen extends AbstractContainerScreen<UavStationMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, "textures/gui/uav_station.png");
    private static final String[] AXES = {"X", "Y", "Z"};
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RED = 0xFFFF0000;

    private @Nullable Button continueButton;

    public UavStationScreen(UavStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = width / 2 - 5;
        int y = height / 2 - 76;
        for (int axis = 0; axis < 3; axis++) {
            for (int step = 0; step < UavStationMenu.STEPS.length; step++) {
                int s = UavStationMenu.STEPS[step];
                int id = axis * 4 + step;
                addRenderableWidget(Button.builder(Component.literal((s > 0 ? "+" : "") + s), b -> press(id))
                        .bounds(x + step * 20, y + axis * 22, 20, 20).build());
            }
        }
        continueButton = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.uav_station.continue"), b -> press(UavStationMenu.BUTTON_CONTINUE))
                .bounds(x - 77, y + 44, 50, 20).build());
        continueButton.active = menu.canContinue();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (continueButton != null) {
            continueButton.active = menu.canContinue();
        }
    }

    private void press(int button) {
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        graphics.blit(RenderType::guiTextured, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        UavStationEntity station = menu.station();
        ItemStack stack = menu.getSlot(0).getItem();
        AircraftDefinition def = stack.isEmpty() ? null : UavStationEntity.definitionOf(stack);
        Component label;
        int color = WHITE;
        if (!stack.isEmpty() && (def == null || !def.uav)) {
            label = Component.translatable("gui.mcheli.uav_station.not_uav");
            color = RED;
        } else if (station != null && station.kind() == UavStationEntity.PORTABLE && def != null && !def.smallUav) {
            label = Component.translatable("gui.mcheli.uav_station.small_only");
            color = RED;
        } else {
            label = title;
        }
        graphics.drawString(font, label, 8, 6, color, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, WHITE, false);
        for (int axis = 0; axis < 3; axis++) {
            graphics.drawString(font, String.format("%s.%+d", AXES[axis], menu.offset(axis)), 58, 15 + axis * 22, WHITE, false);
        }
    }
}
