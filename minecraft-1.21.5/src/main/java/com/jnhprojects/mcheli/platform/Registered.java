package com.jnhprojects.mcheli.platform;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A registry entry the shared code declares and the loader module registers, like Forge's
 * {@code RegistryObject}. {@link #get()} works once the loader has registered it.
 */
public final class Registered<T> implements Supplier<T> {
    private final ResourceLocation id;
    private volatile T value;

    Registered(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    @Override
    public T get() {
        T v = value;
        if (v == null) {
            throw new IllegalStateException("Registry entry " + id + " is not registered yet");
        }
        return v;
    }

    void bind(T value) {
        this.value = value;
    }
}
