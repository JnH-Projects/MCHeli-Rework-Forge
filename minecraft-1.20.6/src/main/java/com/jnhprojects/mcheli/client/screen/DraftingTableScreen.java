package com.jnhprojects.mcheli.client.screen;

import com.jnhprojects.mcheli.MCHeli;
import com.jnhprojects.mcheli.client.workshop.WorkshopHomeScreen;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.WeaponGroup;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.menu.DraftingTableMenu;
import com.jnhprojects.mcheli.network.DraftingTablePacket;
import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.registry.ModDataComponents;
import com.jnhprojects.mcheli.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The drafting table's recipe browser, {@code MCH_DraftingTableGui}. Pick a list (helicopters,
 * planes, tanks, ground vehicles or equipment), step through its recipes, and Create makes the one
 * shown into the output slot. The right page shows the original's illustrated description for
 * equipment and a vehicle's numbers for vehicles; the original turned a 3D model there instead.
 *
 * <p>The recipes come from the client's recipe manager, which the server fills on join.
 */
public class DraftingTableScreen extends AbstractContainerScreen<DraftingTableMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(MCHeli.MOD_ID, "textures/gui/drafting_table.png");
    private static final int TEXTURE_SIZE = 512;
    /** The description pages take this part of their 512 square texture. */
    private static final int PAGE_WIDTH = 340;
    private static final int PAGE_HEIGHT = 380;
    private static final int PAGE_X = 210;
    private static final int PAGE_Y = 20;
    private static final int GRID_X = 121;
    private static final int GRID_Y = 34;
    private static final int TEXT = 0xFF6A3A00;
    private static final int WHITE = 0xFFFFFFFF;
    private static final String[] LISTS = {"helicopter", "plane", "vehicle", "tank", "item"};

    private final List<List<RecipeHolder<CraftingRecipe>>> lists = new ArrayList<>();
    private int list;
    private int index;
    private int page;
    private int pageCount;
    private @Nullable Button create;
    private @Nullable Button previous;
    private @Nullable Button next;
    private @Nullable Button previousPage;
    private @Nullable Button nextPage;

    public DraftingTableScreen(DraftingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 400;
        imageHeight = 240;
        inventoryLabelX = 30;
        inventoryLabelY = 130;
    }

    @Override
    protected void init() {
        super.init();
        collectRecipes();
        for (int i = 0; i < LISTS.length; i++) {
            int which = i;
            addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.drafting_table.list." + LISTS[i]), b -> selectList(which))
                    .bounds(leftPos + 20, topPos + 20 + i * 20, 90, 20).build());
        }
        create = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.drafting_table.create"), b -> create())
                .bounds(leftPos + 120, topPos + 89, 50, 20).build());
        previous = addRenderableWidget(Button.builder(Component.literal("<<"), b -> step(-1)).bounds(leftPos + 120, topPos + 111, 36, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">>"), b -> step(1)).bounds(leftPos + 155, topPos + 111, 35, 20).build());
        previousPage = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.drafting_table.previous_page"), b -> turnPage(-1))
                .bounds(leftPos + 210, topPos + 210, 60, 20).build());
        nextPage = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.drafting_table.next_page"), b -> turnPage(1))
                .bounds(leftPos + 270, topPos + 210, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.drafting_table.workshop"), b -> openWorkshop())
                .tooltip(Tooltip.create(Component.translatable("gui.mcheli.drafting_table.workshop_hint")))
                .bounds(leftPos + 334, topPos + 210, 56, 20).build());
        selectList(list);
    }

    /** The workshop is a screen of its own and needs no block, so the table's menu closes first. */
    private void openWorkshop() {
        minecraft.player.closeContainer();
        minecraft.setScreen(new WorkshopHomeScreen());
    }

    /** Every MCHeli crafting recipe, sorted into the five lists by what it makes. */
    private void collectRecipes() {
        lists.clear();
        for (int i = 0; i < LISTS.length; i++) {
            lists.add(new ArrayList<>());
        }
        for (RecipeHolder<CraftingRecipe> holder : minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = resultOf(holder);
            if (holder.id().getNamespace().equals(MCHeli.MOD_ID) && !result.isEmpty()
                    && BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(MCHeli.MOD_ID)) {
                lists.get(listOf(result.getItem())).add(holder);
            }
        }
        for (List<RecipeHolder<CraftingRecipe>> entries : lists) {
            entries.sort(Comparator.comparing(e -> resultOf(e).getHoverName().getString()));
        }
    }

    private ItemStack resultOf(RecipeHolder<CraftingRecipe> holder) {
        return holder.value().getResultItem(minecraft.level.registryAccess());
    }

    private static int listOf(Item item) {
        if (item == ModItems.HELICOPTER.get()) {
            return 0;
        }
        if (item == ModItems.PLANE.get()) {
            return 1;
        }
        if (item == ModItems.VEHICLE.get()) {
            return 2;
        }
        if (item == ModItems.TANK.get()) {
            return 3;
        }
        return 4;
    }

    private void selectList(int which) {
        list = which;
        index = 0;
        showRecipe();
    }

    private void step(int direction) {
        int size = lists.get(list).size();
        if (size > 0) {
            index = Math.floorMod(index + direction, size);
            showRecipe();
        }
    }

    private void turnPage(int direction) {
        page = Math.clamp(page + direction, 0, Math.max(pageCount - 1, 0));
        updateButtons();
    }

    private void showRecipe() {
        page = 0;
        ItemStack result = result();
        pageCount = descriptionPages(result);
        updateButtons();
    }

    private @Nullable RecipeHolder<CraftingRecipe> current() {
        List<RecipeHolder<CraftingRecipe>> entries = lists.get(list);
        return entries.isEmpty() ? null : entries.get(Math.min(index, entries.size() - 1));
    }

    private ItemStack result() {
        RecipeHolder<CraftingRecipe> entry = current();
        return entry != null ? resultOf(entry) : ItemStack.EMPTY;
    }

    /** The original's description pages for equipment: {@code drafting_table_desc/<item>_<n>.png}. */
    private int descriptionPages(ItemStack result) {
        if (result.isEmpty() || AircraftItem.vehicleOf(result) != null) {
            return 0;
        }
        String name = descriptionName(result);
        int pages = 0;
        while (pages < 10 && minecraft.getResourceManager().getResource(pageTexture(name, pages)).isPresent()) {
            pages++;
        }
        return pages;
    }

    private static String descriptionName(ItemStack result) {
        if (result.getItem() == ModItems.THROWABLE.get()) {
            String throwable = result.get(ModDataComponents.THROWABLE.get());
            return throwable != null ? throwable : "throwable";
        }
        return BuiltInRegistries.ITEM.getKey(result.getItem()).getPath();
    }

    private static ResourceLocation pageTexture(String name, int page) {
        return new ResourceLocation(MCHeli.MOD_ID, "textures/drafting_table_desc/" + name + "_" + page + ".png");
    }

    private boolean canCreate() {
        RecipeHolder<CraftingRecipe> entry = current();
        if (entry == null || menu.getSlot(menu.slots.size() - 1).hasItem() && !minecraft.player.getAbilities().instabuild) {
            return false;
        }
        if (minecraft.player.getAbilities().instabuild) {
            return true;
        }
        StackedContents contents = new StackedContents();
        minecraft.player.getInventory().fillStackedContents(contents);
        return contents.canCraft(entry.value(), null);
    }

    private void create() {
        RecipeHolder<CraftingRecipe> entry = current();
        if (entry != null) {
            ModNetwork.sendToServer(new DraftingTablePacket(entry.id()));
        }
    }

    private void updateButtons() {
        if (create == null || previous == null || next == null || previousPage == null || nextPage == null) {
            return;
        }
        boolean any = current() != null;
        create.active = any && canCreate();
        previous.active = lists.get(list).size() > 1;
        next.active = lists.get(list).size() > 1;
        previousPage.visible = pageCount > 1;
        nextPage.visible = pageCount > 1;
        previousPage.active = page > 0;
        nextPage.active = page < pageCount - 1;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        if (pageCount > 0) {
            String name = descriptionName(result());
            graphics.blit(pageTexture(name, page), leftPos + PAGE_X, topPos + PAGE_Y, PAGE_WIDTH / 2, PAGE_HEIGHT / 2, 0.0F, 0.0F,
                    PAGE_WIDTH, PAGE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        RecipeHolder<CraftingRecipe> entry = current();
        if (entry == null) {
            graphics.drawString(font, Component.translatable("gui.mcheli.drafting_table.empty"), 120, 20, TEXT, false);
            return;
        }
        ItemStack result = result();
        graphics.drawString(font, font.plainSubstrByWidth(result.getHoverName().getString(), 85), 120, 20, WHITE, true);
        graphics.drawString(font, (index + 1) + " / " + lists.get(list).size(), 120, 134, TEXT, false);
        ingredients(graphics, entry.value(), mouseX, mouseY);
        graphics.renderItem(result, 178, 52);
        if (pageCount == 0) {
            details(graphics, result);
        }
    }

    /** {@code drawItemRecipe}: the 3x3 grid, each slot cycling through what it accepts once a second. */
    private void ingredients(GuiGraphics graphics, CraftingRecipe recipe, int mouseX, int mouseY) {
        List<Ingredient> slots = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int tick = (int) (minecraft.level.getGameTime() / 20);
        for (int i = 0; i < slots.size() && i < 9; i++) {
            ItemStack[] options = slots.get(i).getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack stack = options[tick % options.length];
            int x = GRID_X + (i % width) * 18;
            int y = GRID_Y + (i / width) * 18;
            graphics.renderItem(stack, x, y);
            if (mouseX - leftPos >= x && mouseX - leftPos < x + 16 && mouseY - topPos >= y && mouseY - topPos < y + 16) {
                graphics.renderTooltip(font, stack, mouseX - leftPos, mouseY - topPos);
            }
        }
    }

    /** The vehicle's numbers, where the original turned its model. */
    private void details(GuiGraphics graphics, ItemStack result) {
        String name = AircraftItem.vehicleOf(result);
        AircraftDefinition def = name != null ? MCHeli.content().any(name) : null;
        if (def == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.mcheli.drafting_table.hp", def.maxHp));
        lines.add(Component.translatable("gui.mcheli.drafting_table.speed", "%.2f".formatted(def.speed)));
        lines.add(Component.translatable("gui.mcheli.drafting_table.seats", def.seatCount()));
        lines.add(Component.translatable("gui.mcheli.drafting_table.fuel", def.maxFuel));
        lines.add(Component.translatable("gui.mcheli.drafting_table.weapons"));
        for (WeaponGroup group : def.weaponGroups) {
            WeaponDefinition weapon = MCHeli.content().weapon(group.weaponType());
            String weaponName = weapon != null && weapon.displayName != null ? weapon.displayName : group.weaponType();
            lines.add(Component.literal(" " + weaponName + (group.mounts().size() > 1 ? " x" + group.mounts().size() : "")));
        }
        int y = PAGE_Y + 20;
        for (Component line : lines) {
            if (y > PAGE_Y + 180) {
                break;
            }
            graphics.drawString(font, font.split(line, 170).getFirst(), PAGE_X, y, TEXT, false);
            y += 10;
        }
    }
}
