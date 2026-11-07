package com.jnhprojects.mcheli.network;

import com.jnhprojects.mcheli.workshop.TestFlight;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** The workshop's Test flight button: fly the vehicle just saved. The server checks who is asking. */
public record TestFlightPacket(String vehicle) implements CustomPacketPayload {
    public static final Type<TestFlightPacket> TYPE = ModNetwork.type("test_flight");
    public static final StreamCodec<RegistryFriendlyByteBuf, TestFlightPacket> STREAM_CODEC = StreamCodec.ofMember(TestFlightPacket::encode, TestFlightPacket::decode);
    private static final int MAX_NAME = 64;

    public static void encode(TestFlightPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.vehicle, MAX_NAME);
    }

    public static TestFlightPacket decode(FriendlyByteBuf buf) {
        return new TestFlightPacket(buf.readUtf(MAX_NAME));
    }

    public static void handle(TestFlightPacket msg, ServerPlayer sender) {
        if (sender != null) {
            TestFlight.start(sender, msg.vehicle);
        }
    }

    @Override
    public Type<TestFlightPacket> type() {
        return TYPE;
    }
}
