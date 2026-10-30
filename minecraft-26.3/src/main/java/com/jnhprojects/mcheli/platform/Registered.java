package com.jnhprojects.mcheli.platform;

import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * A registry entry the shared code declares and the loader module registers, like Forge's
 * {@code RegistryObject}. {@link #get()} works once the loader has registered it.
 */
public final class Registered<T> implements Supplier<T> {
    private final Identifier id;
    private volatile T value;

    Registered(Identifier id) {
        this.id = id;
    }

    public Identifier id() {
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
