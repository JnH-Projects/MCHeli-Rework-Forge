package com.jnhprojects.mcheli;

import com.jnhprojects.mcheli.content.ContentRegistry;
import com.jnhprojects.mcheli.content.Definition;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.PackException;
import com.jnhprojects.mcheli.content.pack.PendingPacks;
import com.jnhprojects.mcheli.item.AircraftItem;
import com.jnhprojects.mcheli.network.PackSubmissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * {@code /mcheli}: what content is loaded, and operator tools to hand out vehicles and reload the
 * definitions and content packs.
 * <pre>
 *   /mcheli                              content source and counts
 *   /mcheli list [helicopter|plane|...]  loaded vehicles
 *   /mcheli packs                        content packs in use, and their problems
 *   /mcheli give &lt;players&gt; &lt;vehicle&gt; [count]   (operators)
 *   /mcheli reload                       re-read the definitions and packs (operators)
 *   /mcheli pending                      packs players have sent, waiting (operators)
 *   /mcheli approve &lt;pack&gt;               put a waiting pack in use (operators)
 *   /mcheli reject &lt;pack&gt;                throw a waiting pack away (operators)
 * </pre>
 */
public final class McheliCommand {
    private static final List<String> KINDS = List.of("helicopter", "plane", "tank", "vehicle");
    private static final DynamicCommandExceptionType UNKNOWN_VEHICLE =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.mcheli.unknown_vehicle", name));
    private static final DynamicCommandExceptionType UNKNOWN_PENDING =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.mcheli.pending.unknown", name));

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
                        .executes(McheliCommand::reload))
                .then(Commands.literal("pending")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(McheliCommand::pending))
                .then(Commands.literal("approve")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("pack", StringArgumentType.word())
                                .suggests(McheliCommand::suggestPending)
                                .executes(McheliCommand::approve)))
                .then(Commands.literal("reject")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("pack", StringArgumentType.word())
                                .suggests(McheliCommand::suggestPending)
                                .executes(McheliCommand::reject))));
    }

    private static CompletableFuture<Suggestions> suggestPending(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.suggest(MCHeli.packs().pending().list().stream().map(PendingPacks.Submission::id), builder);
        } catch (IOException e) {
            return builder.buildFuture();
        }
    }

    private static int pending(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<PendingPacks.Submission> waiting;
        try {
            waiting = MCHeli.packs().pending().list();
        } catch (IOException e) {
            source.sendFailure(Component.translatable("commands.mcheli.pending.failed", e.toString()));
            return 0;
        }
        if (waiting.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.mcheli.pending.none"), false);
        }
        for (PendingPacks.Submission submission : waiting) {
            source.sendSuccess(() -> Component.translatable("commands.mcheli.pending.entry", submission.id(), submission.manifest().version(),
                    submission.manifest().author(), submission.submitter(), "%.1f".formatted(submission.size() / 1048576.0),
                    String.join(", ", submission.definitions())), false);
            List<String> replaced = replaced(submission);
            if (!replaced.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.mcheli.pending.replaces", String.join(", ", replaced)), false);
            }
        }
        return waiting.size();
    }

    /**
     * What going live would take the place of: a pack with the same id, and vehicles or weapons of
     * another pack with the same file name. That is allowed, so it is the operator's call to make,
     * but it should not come as a surprise.
     */
    private static List<String> replaced(PendingPacks.Submission submission) {
        List<String> names = new ArrayList<>();
        for (ContentPack pack : MCHeli.packs().active()) {
            if (pack.id().equals(submission.id())) {
                names.add("pack " + pack.id());
            }
        }
        ContentRegistry content = MCHeli.content();
        for (String path : submission.definitions()) {
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".txt".length());
            Definition existing = path.startsWith("weapons/") ? content.weapon(name) : content.any(name);
            if (existing != null && !existing.pack().equals(submission.id())) {
                names.add(name + " (" + existing.pack() + ")");
            }
        }
        return names;
    }

    private static PendingPacks.Submission waiting(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "pack");
        PendingPacks.Submission submission;
        try {
            submission = MCHeli.packs().pending().find(id);
        } catch (IOException e) {
            submission = null;
        }
        if (submission == null) {
            throw UNKNOWN_PENDING.create(id);
        }
        return submission;
    }

    private static int approve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        PendingPacks.Submission submission = waiting(ctx);
        try {
            if (PackSubmissions.install(source.getServer(), submission)) {
                source.sendSuccess(() -> Component.translatable("commands.mcheli.approve.success", submission.id(), submission.submitter()), true);
                return 1;
            }
            source.sendFailure(Component.translatable("commands.mcheli.approve.left_out", submission.id()));
            MCHeli.packs().reportProblems(source::sendFailure);
        } catch (IOException | PackException e) {
            source.sendFailure(Component.translatable("commands.mcheli.pending.failed", e.getMessage()));
        }
        return 0;
    }

    private static int reject(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        PendingPacks.Submission submission = waiting(ctx);
        try {
            PackSubmissions.reject(source.getServer(), submission);
            source.sendSuccess(() -> Component.translatable("commands.mcheli.reject.success", submission.id(), submission.submitter()), true);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.translatable("commands.mcheli.pending.failed", e.toString()));
            return 0;
        }
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
            if (!pack.manifest().author().isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.mcheli.packs.author", pack.manifest().author()), false);
            }
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
                player.getInventory().placeItemBackInInventory(AircraftItem.stackOf(def));
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
