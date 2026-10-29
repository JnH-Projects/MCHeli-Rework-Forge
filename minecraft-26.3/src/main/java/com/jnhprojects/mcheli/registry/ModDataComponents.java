package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.platform.Registered;
import com.jnhprojects.mcheli.platform.Registration;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {
    /**
     * The definition a vehicle item places, such as {@code ah-64}. Saved with the stack and sent
     * to clients; the item itself only says which kind of vehicle it is.
     */
    public static final Registered<DataComponentType<String>> VEHICLE = Registration.register(Registries.DATA_COMPONENT_TYPE, "vehicle",
            key -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ModDataComponents() {
    }

    /** Declares the entries; the loader module registers them. */
    public static void init() {
    }
}
