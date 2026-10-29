package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /mcheli}: what content is loaded, and operator tools to hand out vehicles and reload the
 * definitions and content packs.
 * <pre>
 *   /mcheli                              content source and counts
 *   /mcheli list [helicopter|plane|...]  loaded vehicles
 *   /mcheli packs                        content packs in use, and their problems
|plane|...]  loaded vehicles
 *   /mcheli give &lt;players&gt; &lt;vehicle&gt; [count]   (operators)
 *   /mcheli reload                       re-read the definitions and packs (operators)
 * </pre>
 */
public final class McheliCommand {
    private static final List<String> KINDS = List.of("helicopter", "plane", "tank", "vehicle");
    private static final DynamicCommandExceptionType UNKNOWN_VEHICLE =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.mcheli.unknown_vehicle", name));

    private McheliCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(MCHeli.MOD_ID)
                .executes(McheliCommand::status)
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx, null))
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(KINDS, builder))
                                .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "kind")))))
                .then(Commands.literal("packs")
                        .executes(McheliCommand::packs))
                .then(Commands.literal("give")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("vehicle", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                MCHeli.content().all().stream().map(AircraftDefinition::name), builder))
                                        .executes(ctx -> give(ctx, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "count")))))))
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(McheliCommand::reload)));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ContentRegistry content = MCHeli.content();
        CommandSourceStack source = ctx.getSource();
        MCHeli.legacyContent().problem().ifPresent(source::sendFailure);
        source.sendSuccess(() -> Component.translatable("commands.mcheli.status",
                content.helicopters().size(), content.planes().size(), content.tanks().size(), content.vehicles().size(),
                content.weapons().size(), MCHeli.legacyContent().assets().toString()), false);
        List<ContentPack> packs = MCHeli.packs().active();
        source.sendSuccess(() -> Component.translatable("commands.mcheli.status.packs", packs.size(),
                packs.stream().map(ContentPack::id).collect(Collectors.joining(", "))), false);
        return content.all().size();
    }

    private static int packs(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<ContentPack> packs = MCHeli.packs().active();
        if (packs.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.mcheli.packs.none", MCHeli.packs().folder().toString()), false);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.mcheli.packs.header", packs.size(), MCHeli.packs().folder().toString()), false);
        }
        ContentRegistry content = MCHeli.content();
        for (ContentPack pack : packs) {
            long vehicles = content.all().stream().filter(def -> def.pack().equals(pack.id())).count();
            long weapons = content.weapons().stream().filter(def -> def.pack().equals(pack.id())).count();
            source.sendSuccess(() -> Component.translatable("commands.mcheli.packs.entry", pack.id(), pack.manifest().version(),
                    pack.manifest().name(), vehicles, weapons, ContentPack.shortHash(pack.hash())), false);
        }
        MCHeli.packs().reportProblems(source::sendFailure);
        return packs.size();
    }

    private static int list(CommandContext<CommandSourceStack> ctx, String kind) {
        List<AircraftDefinition> defs = MCHeli.content().all().stream()
                .filter(def -> kind == null || def.kind().equals(kind))
                .toList();
        String names = defs.stream().map(AircraftDefinition::name).collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mcheli.list", defs.size(), names), false);
        return defs.size();
    }

    private static int give(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "vehicle");
        AircraftDefinition def = MCHeli.content().any(name);
        if (def == null) {
            throw UNKNOWN_VEHICLE.create(name);
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            // Vehicle items do not stack, so each one is a separate stack.
            for (int i = 0; i < count; i++) {
                player.getInventory().placeItemBackInInventory(AircraftItem.stackOf(def), Prediction.SERVER_ONLY);
            }
        }
        Component vehicle = AircraftItem.stackOf(def).getHoverName();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.mcheli.give", count, vehicle, targets.size()), true);
        return targets.size();
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ContentRegistry fresh = ContentReload.reload(source.getServer());
        source.sendSuccess(() -> Component.translatable("commands.mcheli.reload.success",
                fresh.all().size(), fresh.weapons().size(), MCHeli.packs().active().size()), true);
        MCHeli.packs().reportProblems(source::sendFailure);
        return fresh.all().size();
    }
}
