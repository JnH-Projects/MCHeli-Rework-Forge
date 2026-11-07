package com.jnhprojects.mcheli.platform;

import com.jnhprojects.mcheli.MCHeli;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Every vanilla registry entry the mod adds. The shared registry classes declare their entries
 * here during mod construction; the loader module then registers them its own way: Forge and
 * NeoForge from their register event, Fabric straight away.
 */
public final class Registration {
    private static final List<Pending<?, ?>> PENDING = new ArrayList<>();
    private static final Set<ResourceKey<? extends Registry<?>>> REGISTRIES = new LinkedHashSet<>();

    private Registration() {
    }

    /**
     * Declares an entry.
     *
     * @param factory makes the value; it gets the entry's key for {@code Item.Properties#setId}
     *                and the like
     */
    public static synchronized <R, T extends R> Registered<T> register(ResourceKey<? extends Registry<R>> registry, String name,
                                                                       Function<ResourceKey<R>, T> factory) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MCHeli.MOD_ID, name);
        Registered<T> entry = new Registered<>(id);
        PENDING.add(new Pending<>(registry, entry, factory));
        REGISTRIES.add(registry);
        return entry;
    }

    /** The registries with entries of ours, in the order they were first used. */
    public static synchronized List<ResourceKey<? extends Registry<?>>> registries() {
        return List.copyOf(REGISTRIES);
    }

    /**
     * A key from {@link #registries()} with its element type filled in, for the loader's generic
     * register calls. The type is whatever the caller needs; entries are only ever handed to the
     * registry they were declared for.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<Registry<T>> typed(ResourceKey<? extends Registry<?>> registry) {
        return (ResourceKey<Registry<T>>) registry;
    }

    /**
     * Creates our entries for one registry, in declaration order, and hands each to the loader.
     * Called once per registry.
     */
    public static synchronized <R> void registerAll(ResourceKey<? extends Registry<R>> registry, BiConsumer<ResourceLocation, R> sink) {
        for (Pending<?, ?> pending : PENDING) {
            if (pending.registry.equals(registry)) {
                @SuppressWarnings("unchecked")
                Pending<R, ? extends R> ours = (Pending<R, ? extends R>) pending;
                ours.create(sink);
            }
        }
    }

    private record Pending<R, T extends R>(ResourceKey<? extends Registry<R>> registry, Registered<T> entry,
                                           Function<ResourceKey<R>, T> factory) {
        @SuppressWarnings("unchecked")
        void create(BiConsumer<ResourceLocation, R> sink) {
            T value = factory.apply(ResourceKey.create((ResourceKey<? extends Registry<R>>) registry, entry.id()));
            entry.bind(value);
            sink.accept(entry.id(), value);
        }
    }
}
