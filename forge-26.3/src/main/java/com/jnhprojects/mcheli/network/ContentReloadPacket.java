package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.entity.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server to every player: the definitions were reloaded with {@code /mcheli reload}. Clients
 * reload theirs too (unless they share the server's, in single player) and drop cached models and
 * textures so edited files show up.
 */
public record ContentReloadPacket() {

    public static void encode(ContentReloadPacket msg, FriendlyByteBuf buf) {
    }

    public static ContentReloadPacket decode(FriendlyByteBuf buf) {
        return new ContentReloadPacket();
    }

    public static void handle(ContentReloadPacket msg, CustomPayloadEvent.Context ctx) {
        ClientAccess.contentReloaded();
    }
}
