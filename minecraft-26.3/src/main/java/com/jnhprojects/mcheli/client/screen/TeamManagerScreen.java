package com.jnhprojects.mcheli.client.screen;

import com.jnhprojects.mcheli.network.ModNetwork;
import com.jnhprojects.mcheli.network.TeamPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The team manager, {@code MCH_GuiScoreboard_Main} and {@code MCH_GuiScoreboard_CreateTeam}, for
 * operators setting up a match: switch PvP, shuffle everyone into the teams, make a team, send
 * everyone to their spawn point and clear away every vehicle. The teams and their players are
 * listed on the right.
 */
public class TeamManagerScreen extends Screen {
    private static final int WIDTH = 150;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFFA0A0A0;

    private @Nullable EditBox name;
    private int color = TeamColor.RED.ordinal();
    private boolean friendlyFire;
    private @Nullable Button colorButton;
    private @Nullable Button friendlyFireButton;
    private @Nullable Button createButton;

    public TeamManagerScreen() {
        super(Component.translatable("gui.mcheli.team.title"));
    }

    @Override
    protected void init() {
        int left = width / 2 - WIDTH - 10;
        int y = 40;
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.team.pvp"), b -> send(TeamPacket.Action.PVP))
                .bounds(left, y, WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.team.shuffle"), b -> send(TeamPacket.Action.SHUFFLE))
                .tooltip(Tooltip.create(Component.translatable("gui.mcheli.team.shuffle_hint")))
                .bounds(left, y + 22, WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.team.jump_spawn"), b -> send(TeamPacket.Action.JUMP_TO_SPAWN))
                .tooltip(Tooltip.create(Component.translatable("gui.mcheli.team.jump_spawn_hint")))
                .bounds(left, y + 44, WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.team.destroy_all"), b -> send(TeamPacket.Action.DESTROY_ALL))
                .tooltip(Tooltip.create(Component.translatable("gui.mcheli.team.destroy_all_hint")))
                .bounds(left, y + 66, WIDTH, 20).build());

        int create = y + 104;
        name = addRenderableWidget(new EditBox(font, left, create, WIDTH, 20, Component.translatable("gui.mcheli.team.name")));
        name.setMaxLength(16);
        name.setHint(Component.translatable("gui.mcheli.team.name"));
        name.setResponder(text -> updateButtons());
        colorButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            color = (color + 1) % TeamColor.VALUES.size();
            updateButtons();
        }).bounds(left, create + 22, WIDTH, 20).build());
        friendlyFireButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            friendlyFire = !friendlyFire;
            updateButtons();
        }).bounds(left, create + 44, WIDTH, 20).build());
        createButton = addRenderableWidget(Button.builder(Component.translatable("gui.mcheli.team.create"), b -> {
            ModNetwork.sendToServer(new TeamPacket(TeamPacket.Action.CREATE_TEAM, name.getValue(), color, friendlyFire));
            name.setValue("");
        }).bounds(left, create + 66, WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        updateButtons();
    }

    private void send(TeamPacket.Action action) {
        ModNetwork.sendToServer(new TeamPacket(action));
    }

    private void updateButtons() {
        if (colorButton == null || friendlyFireButton == null || createButton == null || name == null) {
            return;
        }
        TeamColor teamColor = TeamColor.VALUES.get(color);
        colorButton.setMessage(Component.translatable("gui.mcheli.team.color",
                Component.literal(teamColor.getSerializedName()).withColor(teamColor.textColor().getValue())));
        friendlyFireButton.setMessage(Component.translatable(friendlyFire ? "gui.mcheli.team.friendly_fire_on" : "gui.mcheli.team.friendly_fire_off"));
        String value = name.getValue().trim();
        createButton.active = !value.isEmpty() && !value.contains(" ");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 15, TEXT);
        int left = width / 2 - WIDTH - 10;
        graphics.text(font, Component.translatable("gui.mcheli.team.new_team"), left, 132, DIM);
        teams(graphics, width / 2 + 10, 40);
    }

    /** The scoreboard's teams and who is in them, as this client sees the scoreboard. */
    private void teams(GuiGraphicsExtractor graphics, int x, int y) {
        if (minecraft.level == null) {
            return;
        }
        List<PlayerTeam> teams = new ArrayList<>(minecraft.level.getScoreboard().getPlayerTeams());
        if (teams.isEmpty()) {
            graphics.text(font, Component.translatable("gui.mcheli.team.none"), x, y, DIM);
            return;
        }
        int bottom = height - 40;
        for (PlayerTeam team : teams) {
            if (y > bottom) {
                break;
            }
            graphics.text(font, team.getFormattedDisplayName(), x, y, TEXT);
            y += 11;
            for (String player : team.getPlayers()) {
                if (y > bottom) {
                    break;
                }
                graphics.text(font, "  " + player, x, y, DIM);
                y += 10;
            }
            y += 4;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
