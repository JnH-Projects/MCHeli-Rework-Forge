package com.jnhprojects.mcheli.registry;

import com.jnhprojects.mcheli.MCHeli;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MCHeli.MOD_ID);

    /**
     * The definition a vehicle item places, such as {@code ah-64}. Saved with the stack and sent
     * to clients; the item itself only says which kind of vehicle it is.
     */
    public static final RegistryObject<DataComponentType<String>> VEHICLE = COMPONENTS.register("vehicle",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ModDataComponents() {
    }

    public static void register(BusGroup modBus) {
        COMPONENTS.register(modBus);
    }
}
