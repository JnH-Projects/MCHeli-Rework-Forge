package com.jnhprojects.mcheli.client.screen;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.weapon.RoundItem;
import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.entity.VehicleInventory;
import com.jnhprojects.mcheli.menu.VehicleMenu;
import com.jnhprojects.mcheli.weapon.LegacyItems;
import com.jnhprojects.mcheli.weapon.VehicleWeapons;
import com.jnhprojects.mcheli.weapon.WeaponSet;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The vehicle screen, {@code MCH_AircraftGui}: fuel and parachute slots on the left, the weapon
 * panel on the right with its reload cost and button, and a button into the cargo chest for
 * vehicles that have one. The panel is drawn from the chest texture so it looks like vanilla.
 */
public class VehicleScreen extends AbstractContainerScreen<VehicleMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    /** Height of the part above the player inventory. */
    private static final int TOP = 88;
    private static final int PANEL_X = 72;
    private static final int PANEL_WIDTH = 96;
    private static final int LABEL = 0xFF404040;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RED = 0xFFFF5555;
    private static final int GREEN = 0xFF55FF55;

    private int weapon;
    private boolean cargoButton;
    private @Nullable Button reload;
    private @Nullable Button previous;
    private @Nullable Button next;

    public VehicleScreen(VehicleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, TOP + 96);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> step(-1))
                .bounds(leftPos + PANEL_X, topPos + 68, 16, 16).build());
        reload = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.vehicle.reload"), b -> press(weapon))
                .bounds(leftPos + PANEL_X + 18, topPos + 68, PANEL_WIDTH - 36, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> step(1))
                .bounds(leftPos + PANEL_X + PANEL_WIDTH - 16, topPos + 68, 16, 16).build());
        AircraftEntity vehicle = menu.vehicle();
        AircraftDefinition def = vehicle != null ? vehicle.definition() : null;
        cargoButton = def != null && def.cargoRows() > 0;
        if (cargoButton) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.vehicle.cargo"), b -> press(VehicleMenu.BUTTON_CARGO))
                    .bounds(leftPos + imageWidth - 52, topPos + 3, 46, 13).build());
        }
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private int weaponCount() {
        AircraftEntity vehicle = menu.vehicle();
        VehicleWeapons weapons = vehicle != null ? vehicle.weapons() : null;
        return weapons != null ? Math.min(weapons.sets().size(), VehicleMenu.MAX_WEAPONS) : 0;
    }

    private void step(int direction) {
        int count = weaponCount();
        if (count > 0) {
            weapon = Math.floorMod(weapon + direction, count);
            updateButtons();
        }
    }

    private void press(int button) {
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private void updateButtons() {
        if (reload == null || previous == null || next == null) {
            return;
        }
        int count = weaponCount();
        reload.visible = count > 0;
        previous.visible = count > 1;
        next.visible = count > 1;
        reload.active = menu.canReload(weapon);
        AircraftEntity vehicle = menu.vehicle();
        boolean grounded = vehicle != null && vehicle.canSupply();
        reload.setTooltip(grounded ? null : Tooltip.create(Component.translatable("gui.mcheli.vehicle.reload_hint")));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = leftPos;
        int y = topPos;
        // The chest's title bar, its plain middle stretched down to the inventory, then the inventory.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y, 0.0F, 0.0F, imageWidth, 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + 17, 0.0F, 12.0F, imageWidth, TOP - 17, imageWidth, 1, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + TOP, 0.0F, 126.0F, imageWidth, 96, 256, 256);
        for (int i = 0; i < VehicleInventory.FUEL_SLOTS; i++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x + VehicleMenu.FUEL_X + i * 18 - 1, y + VehicleMenu.FUEL_Y - 1, 18, 18);
        }
        for (int i = 0; i < menu.parachuteSlots(); i++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x + VehicleMenu.PARACHUTE_X + i * 18 - 1, y + VehicleMenu.PARACHUTE_Y - 1, 18, 18);
        }
        graphics.fill(x + PANEL_X - 2, y + 17, x + PANEL_X + PANEL_WIDTH + 2, y + 66, 0xFF373737);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        AircraftEntity vehicle = menu.vehicle();
        // Long names only give way to the Cargo button, where there is one.
        int titleWidth = cargoButton ? imageWidth - 64 : imageWidth - 16;
        graphics.text(font, font.plainSubstrByWidth(title.getString(), titleWidth), titleLabelX, titleLabelY, LABEL, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        if (vehicle == null) {
            return;
        }
        if (vehicle.maxFuel() > 0) {
            // A percentage and a gauge: the exact units would run into the weapon panel.
            int percent = Math.round(vehicle.fuelRatio() * 100.0F);
            graphics.text(font, Component.translatable("gui.mcheli.vehicle.fuel", percent), VehicleMenu.FUEL_X, VehicleMenu.FUEL_Y - 11, LABEL, false);
            int gaugeY = VehicleMenu.FUEL_Y + 18;
            int width = VehicleInventory.FUEL_SLOTS * 18 - 2;
            graphics.fill(VehicleMenu.FUEL_X, gaugeY, VehicleMenu.FUEL_X + width, gaugeY + 3, 0xFF373737);
            graphics.fill(VehicleMenu.FUEL_X, gaugeY, VehicleMenu.FUEL_X + width * percent / 100, gaugeY + 3, percent <= 10 ? RED : GREEN);
        }
        if (menu.parachuteSlots() > 0) {
            graphics.text(font, Component.translatable("gui.mcheli.vehicle.parachute"), VehicleMenu.PARACHUTE_X, VehicleMenu.PARACHUTE_Y - 11, LABEL, false);
        }
        weaponPanel(graphics, vehicle);
    }

    /** Name, rounds carried against the maximum, and what one reload costs. */
    private void weaponPanel(GuiGraphicsExtractor graphics, AircraftEntity vehicle) {
        VehicleWeapons weapons = vehicle.weapons();
        WeaponSet set = weapons != null && weaponCount() > 0 ? weapons.set(Math.min(weapon, weaponCount() - 1)) : null;
        if (set == null) {
            graphics.text(font, Component.translatable("gui.mcheli.vehicle.no_weapons"), PANEL_X, 20, WHITE, false);
            return;
        }
        graphics.text(font, font.plainSubstrByWidth(set.displayName(), PANEL_WIDTH), PANEL_X, 20, WHITE, false);
        int rounds = menu.rounds(weapon);
        int max = set.maxAmmo();
        int color = rounds == 0 && max > 0 ? RED : rounds >= max ? GREEN : WHITE;
        graphics.text(font, rounds + " / " + max, PANEL_X, 31, color, false);
        int itemX = PANEL_X;
        for (RoundItem cost : set.definition().roundItems) {
            Item item = LegacyItems.item(cost.item(), cost.damage());
            if (item == null) {
                continue;
            }
            graphics.item(new ItemStack(item), itemX, 42);
            graphics.text(font, String.valueOf(cost.count()), itemX + 17, 47, WHITE, false);
            itemX += 32;
            if (itemX > PANEL_X + PANEL_WIDTH - 16) {
                break;
            }
        }
    }
}
