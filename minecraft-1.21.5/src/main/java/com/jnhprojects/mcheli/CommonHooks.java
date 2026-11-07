package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.entity.AircraftEntity;
import com.jnhprojects.mcheli.item.ThrowableItem;
import com.jnhprojects.mcheli.network.PackSync;
import com.jnhprojects.mcheli.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;

/**
 * What the mod does on game events that both sides see. Each loader module calls these from its
 * own events; nothing here knows which loader that is.
 */
public final class CommonHooks {
    private CommonHooks() {
    }

    /** Once the registries are frozen, on the main thread: what vanilla keeps in its own tables. */
    public static void commonSetup() {
        DispenserBlock.registerBehavior(ModItems.THROWABLE.get(), new ThrowableItem.DispenseBehavior());
    }

    /**
     * Adds the imported original content to the resource packs (client) and data packs (server).
     * Without the original zip there is nothing to add.
     */
    public static RepositorySource packSource() {
        return onLoad -> {
            // Fabric builds some pack repositories before our initializer has run.
            LegacyContent content = MCHeli.legacyContent();
            if (content != null) {
                content.pack().ifPresent(onLoad);
            }
        };
    }

    /** Before the worlds load: read the world's server config, then load the content packs it allows. */
    public static void serverAboutToStart(MinecraftServer server) {
        ServerConfig.load(server.getWorldPath(LevelResource.ROOT));
        MCHeli.content().replaceWith(MCHeli.packs().load(true));
    }

    public static void serverStopped() {
        ServerConfig.unload();
    }

    public static void serverTickEnd(MinecraftServer server) {
        PackSync.pump(server);
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        McheliCommand.register(dispatcher);
    }

    /**
     * The vehicle recipes live in {@code data/mcheli/recipe} of the imported content pack. Every
     * MCHeli recipe goes into a player's recipe book when they join: the drafting table lists them
     * from there, and they can be found in the crafting table's book too.
     */
    public static void playerLoggedIn(ServerPlayer player) {
        PackSync.onLogin(player);
        // A remote pilot whose drone did not come back with them is shown and made mortal again.
        if (!(player.getVehicle() instanceof AircraftEntity uav && uav.isRemotePilot(player))) {
            AircraftEntity.clearRemotePilot(player);
        }
        List<RecipeHolder<?>> ours = player.level().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().location().getNamespace().equals(MCHeli.MOD_ID))
                .toList();
        player.awardRecipes(ours);
    }

    public static void playerLoggedOut(ServerPlayer player) {
        PackSync.onLogout(player);
    }
}
