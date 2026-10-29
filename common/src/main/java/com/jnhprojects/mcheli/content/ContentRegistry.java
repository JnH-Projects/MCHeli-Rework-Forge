package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import com.jnhprojects.mcheli.content.weapon.WeaponDefinition;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Loaded vehicle and weapon definitions, keyed by file name.
 *
 * <p>The registry can change while the game runs: a definition can be added or replaced
 * ({@link #add}), and the whole set can be swapped for a freshly loaded one ({@link #replaceWith})
 * when an operator reloads the content. Every change bumps {@link #generation()}, which is how
 * vehicles already in the world notice that their definition object is stale.
 *
 * <p>Readers never lock. The maps live in an immutable snapshot that writers copy, change and
 * publish in one volatile write, so the render thread and the server thread of a single player
 * game always see a consistent set, even halfway through a reload.
 */
public final class ContentRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentRegistry.class);

    private volatile Snapshot snapshot;
    private volatile int generation;

    private ContentRegistry(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    /** A registry with nothing in it, for when no content could be found. */
    public static ContentRegistry empty() {
        return new ContentRegistry(new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));
    }

    /**
     * @param assetRoot the {@code assets/mcheli} folder; when it does not exist, nothing is loaded
     */
    public static ContentRegistry load(Path assetRoot) throws IOException {
        Map<String, Set<String>> unhandled = new TreeMap<>();
        Map<String, WeaponDefinition> weapons = loadFolder(assetRoot.resolve(WeaponDefinition.FOLDER), WeaponDefinition::new, unhandled);
        Map<String, HelicopterDefinition> helicopters =
                loadFolder(assetRoot.resolve(HelicopterDefinition.FOLDER), HelicopterDefinition::new, unhandled);
        Map<String, PlaneDefinition> planes = loadFolder(assetRoot.resolve(PlaneDefinition.FOLDER), PlaneDefinition::new, unhandled);
        Map<String, TankDefinition> tanks = loadFolder(assetRoot.resolve(TankDefinition.FOLDER), TankDefinition::new, unhandled);
        Map<String, VehicleDefinition> vehicles = loadFolder(assetRoot.resolve(VehicleDefinition.FOLDER), VehicleDefinition::new, unhandled);

        for (Map<String, ? extends AircraftDefinition> kind : List.of(helicopters, planes, tanks, vehicles)) {
            for (AircraftDefinition def : kind.values()) {
                dropUnknownWeapons(def, weapons);
            }
        }

        LOGGER.info("Loaded {} helicopters, {} planes, {} tanks, {} vehicles and {} weapons",
                helicopters.size(), planes.size(), tanks.size(), vehicles.size(), weapons.size());
        if (!unhandled.isEmpty()) {
            LOGGER.debug("Definition keys not implemented yet: {}", unhandled.keySet());
        }
        return new ContentRegistry(new Snapshot(helicopters, planes, tanks, vehicles, weapons, unhandled));
    }

    private static <T extends Definition> Map<String, T> loadFolder(
            Path folder, Function<String, T> factory, Map<String, Set<String>> unhandled) throws IOException {
        Map<String, T> out = new LinkedHashMap<>();
        if (!Files.isDirectory(folder)) {
            return out;
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(folder)) {
            files = stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .sorted()
                    .toList();
        }

        for (Path file : files) {
            String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
            String name = fileName.substring(0, fileName.length() - 4);
            DefinitionReader.Result<T> result = DefinitionReader.read(file, factory.apply(name));
            for (String problem : result.problems()) {
                LOGGER.warn("{}/{}: {}", folder.getFileName(), fileName, problem);
            }
            for (String key : result.unhandledKeys()) {
                unhandled.computeIfAbsent(key, k -> new TreeSet<>()).add(name);
            }
            if (result.isValid()) {
                out.put(name, result.definition());
            }
        }
        return out;
    }

    /**
     * The original silently skipped AddWeapon lines naming a weapon it had not loaded. The shipped
     * AH-64, KV-2 and M26 do that, so it is only worth a debug line.
     */
    private static void dropUnknownWeapons(AircraftDefinition def, Map<String, WeaponDefinition> weapons) {
        List<String> dropped = def.retainWeapons(weapons.keySet());
        if (!dropped.isEmpty()) {
            LOGGER.debug("{}/{}: unknown weapons {}", def.folder(), def.name(), dropped);
        }
    }

    // Changes

    /**
     * Adds a vehicle definition, or replaces the one with the same name. Names are unique across
     * kinds, so a definition that changed kind leaves its old kind. Weapons the definition names
     * must already be registered; unknown ones are dropped, as on load.
     */
    public synchronized void add(AircraftDefinition def) {
        dropUnknownWeapons(def, snapshot.weapons);
        Snapshot s = snapshot.without(def.name());
        snapshot = switch (def) {
            case HelicopterDefinition h -> s.withHelicopters(put(s.helicopters, h));
            case PlaneDefinition p -> s.withPlanes(put(s.planes, p));
            case TankDefinition t -> s.withTanks(put(s.tanks, t));
            case VehicleDefinition v -> s.withVehicles(put(s.vehicles, v));
            default -> throw new IllegalArgumentException("Unknown vehicle kind " + def.kind());
        };
        generation++;
    }

    /** Adds a weapon definition, or replaces the one with the same name. */
    public synchronized void addWeapon(WeaponDefinition weapon) {
        snapshot = snapshot.withWeapons(put(snapshot.weapons, weapon));
        generation++;
    }

    /** Removes the vehicle definition with this name, of whatever kind. Returns whether one existed. */
    public synchronized boolean remove(String name) {
        if (any(name) == null) {
            return false;
        }
        snapshot = snapshot.without(name);
        generation++;
        return true;
    }

    /** Takes over everything {@code other} holds, as one change. Used by the content reload. */
    public synchronized void replaceWith(ContentRegistry other) {
        snapshot = other.snapshot;
        generation++;
    }

    /**
     * Counts the changes made to this registry. Anything that caches a definition object compares
     * this to notice a reload; the value means nothing on its own.
     */
    public int generation() {
        return generation;
    }

    private static <T extends Definition> Map<String, T> put(Map<String, T> map, T def) {
        Map<String, T> copy = new LinkedHashMap<>(map);
        copy.put(def.name(), def);
        return copy;
    }

    // Lookups

    public Collection<HelicopterDefinition> helicopters() {
        return snapshot.helicopters.values();
    }

    public @Nullable HelicopterDefinition helicopter(String name) {
        return snapshot.helicopters.get(name);
    }

    public Collection<PlaneDefinition> planes() {
        return snapshot.planes.values();
    }

    public @Nullable PlaneDefinition plane(String name) {
        return snapshot.planes.get(name);
    }

    public Collection<TankDefinition> tanks() {
        return snapshot.tanks.values();
    }

    public @Nullable TankDefinition tank(String name) {
        return snapshot.tanks.get(name);
    }

    public Collection<VehicleDefinition> vehicles() {
        return snapshot.vehicles.values();
    }

    public @Nullable VehicleDefinition vehicle(String name) {
        return snapshot.vehicles.get(name);
    }

    public Collection<WeaponDefinition> weapons() {
        return snapshot.weapons.values();
    }

    public @Nullable WeaponDefinition weapon(String name) {
        return snapshot.weapons.get(name);
    }

    /** Every definition of every kind: helicopters, planes, tanks, then vehicles. */
    public List<AircraftDefinition> all() {
        Snapshot s = snapshot;
        List<AircraftDefinition> all = new ArrayList<>(s.helicopters.size() + s.planes.size() + s.tanks.size() + s.vehicles.size());
        all.addAll(s.helicopters.values());
        all.addAll(s.planes.values());
        all.addAll(s.tanks.values());
        all.addAll(s.vehicles.values());
        return all;
    }

    /** Looks a definition up by name in any kind. Names are unique across kinds. */
    public @Nullable AircraftDefinition any(String name) {
        return snapshot.any(name);
    }

    /** Keys found in the files that no handler understood, mapped to the files that use them. */
    public Map<String, Set<String>> unhandledKeys() {
        return snapshot.unhandledKeys;
    }

    /** One consistent state of the registry. Never changed once published. */
    private record Snapshot(Map<String, HelicopterDefinition> helicopters, Map<String, PlaneDefinition> planes,
                            Map<String, TankDefinition> tanks, Map<String, VehicleDefinition> vehicles,
                            Map<String, WeaponDefinition> weapons, Map<String, Set<String>> unhandledKeys) {
        Snapshot {
            helicopters = Collections.unmodifiableMap(helicopters);
            planes = Collections.unmodifiableMap(planes);
            tanks = Collections.unmodifiableMap(tanks);
            vehicles = Collections.unmodifiableMap(vehicles);
            weapons = Collections.unmodifiableMap(weapons);
            unhandledKeys = Collections.unmodifiableMap(unhandledKeys);
        }

        @Nullable AircraftDefinition any(String name) {
            AircraftDefinition def = helicopters.get(name);
            if (def == null) {
                def = planes.get(name);
            }
            if (def == null) {
                def = tanks.get(name);
            }
            if (def == null) {
                def = vehicles.get(name);
            }
            return def;
        }

        Snapshot without(String name) {
            return new Snapshot(remove(helicopters, name), remove(planes, name), remove(tanks, name), remove(vehicles, name),
                    weapons, unhandledKeys);
        }

        Snapshot withHelicopters(Map<String, HelicopterDefinition> map) {
            return new Snapshot(map, planes, tanks, vehicles, weapons, unhandledKeys);
        }

        Snapshot withPlanes(Map<String, PlaneDefinition> map) {
            return new Snapshot(helicopters, map, tanks, vehicles, weapons, unhandledKeys);
        }

        Snapshot withTanks(Map<String, TankDefinition> map) {
            return new Snapshot(helicopters, planes, map, vehicles, weapons, unhandledKeys);
        }

        Snapshot withVehicles(Map<String, VehicleDefinition> map) {
            return new Snapshot(helicopters, planes, tanks, map, weapons, unhandledKeys);
        }

        Snapshot withWeapons(Map<String, WeaponDefinition> map) {
            return new Snapshot(helicopters, planes, tanks, vehicles, map, unhandledKeys);
        }

        private static <T> Map<String, T> remove(Map<String, T> map, String name) {
            if (!map.containsKey(name)) {
                return map;
            }
            Map<String, T> copy = new LinkedHashMap<>(map);
            copy.remove(name);
            return copy;
        }
    }
}
