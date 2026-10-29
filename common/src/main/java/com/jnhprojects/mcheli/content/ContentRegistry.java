package com.jnhprojects.mcheli.content;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import com.jnhprojects.mcheli.content.pack.ContentSource;
import com.jnhprojects.mcheli.content.pack.PackProblem;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Loaded vehicle and weapon definitions, keyed by file name.
 *
 * <p>Definitions come from one or more {@linkplain ContentSource sources}: the imported 1.0.3
 * content and the content packs. A definition in a later source replaces the one with the same
 * name from an earlier source, whatever its kind.
 *
 * <p>The registry can change while the game runs: a definition can be added or replaced
 * ({@link #add}), and the whole set can be swapped for a freshly loaded one ({@link #replaceWith})
 * when an operator reloads the content or a server sends its packs. Every change bumps
 * {@link #generation()}, which is how vehicles already in the world notice that their definition
 * object is stale.
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
        return new ContentRegistry(new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()));
    }

    /**
     * Loads the imported 1.0.3 content alone.
     *
     * @param assetRoot the {@code assets/mcheli} folder; when it does not exist, nothing is loaded
     */
    public static ContentRegistry load(Path assetRoot) {
        return load(List.of(new ContentSource(ContentSource.ORIGINAL, assetRoot)));
    }

    /**
     * Loads every source, in order. Weapons come first, from all sources, so a vehicle can use a
     * weapon from another pack. Never throws: a file or folder that cannot be read is logged and
     * listed in {@link #problems()}, and everything else still loads.
     */
    public static ContentRegistry load(List<ContentSource> sources) {
        Loader loader = new Loader();
        for (ContentSource source : sources) {
            loader.folder(source, WeaponDefinition.FOLDER, WeaponDefinition::new, loader::weapon);
        }
        for (ContentSource source : sources) {
            loader.folder(source, HelicopterDefinition.FOLDER, HelicopterDefinition::new, loader::vehicle);
            loader.folder(source, PlaneDefinition.FOLDER, PlaneDefinition::new, loader::vehicle);
            loader.folder(source, TankDefinition.FOLDER, TankDefinition::new, loader::vehicle);
            loader.folder(source, VehicleDefinition.FOLDER, VehicleDefinition::new, loader::vehicle);
        }
        return new ContentRegistry(loader.finish(sources.size()));
    }

    /** Collects definitions across sources; one per {@link #load(List)} call. */
    private static final class Loader {
        private final Map<String, WeaponDefinition> weapons = new LinkedHashMap<>();
        private final Map<String, AircraftDefinition> vehicles = new LinkedHashMap<>();
        private final Map<String, Set<String>> unhandled = new TreeMap<>();
        private final List<PackProblem> problems = new ArrayList<>();

        <T extends Definition> void folder(ContentSource source, String folder, Function<String, T> factory, Consumer<T> sink) {
            Path dir = source.assets().resolve(folder);
            if (!Files.isDirectory(dir)) {
                return;
            }

            List<Path> files;
            try (Stream<Path> stream = Files.list(dir)) {
                files = stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                        .sorted()
                        .toList();
            } catch (IOException e) {
                problem(source, folder + ": could not be read: " + e);
                return;
            }

            for (Path file : files) {
                String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                String name = fileName.substring(0, fileName.length() - 4);
                DefinitionReader.Result<T> result;
                try {
                    result = DefinitionReader.read(file, factory.apply(name));
                } catch (IOException e) {
                    problem(source, folder + "/" + fileName + ": could not be read: " + e);
                    continue;
                }
                for (String problem : result.problems()) {
                    LOGGER.warn("{}: {}/{}: {}", source.pack(), folder, fileName, problem);
                    // The import's own quirks are EMB4's and known; only pack authors can act on theirs.
                    if (!source.isOriginal()) {
                        problems.add(new PackProblem(source.pack(), folder + "/" + fileName + ": " + problem));
                    }
                }
                for (String key : result.unhandledKeys()) {
                    unhandled.computeIfAbsent(key, k -> new TreeSet<>()).add(name);
                }
                if (result.isValid()) {
                    T def = result.definition();
                    def.setPack(source.pack());
                    sink.accept(def);
                }
            }
        }

        void weapon(WeaponDefinition weapon) {
            replaced(weapons.put(weapon.name(), weapon), weapon, WeaponDefinition.FOLDER);
        }

        void vehicle(AircraftDefinition def) {
            replaced(vehicles.put(def.name(), def), def, def.folder());
        }

        private static void replaced(@Nullable Definition previous, Definition def, String folder) {
            if (previous != null && !previous.pack().equals(def.pack())) {
                LOGGER.info("{} replaces {}/{} from {}", def.pack(), folder, def.name(), previous.pack());
            }
        }

        private void problem(ContentSource source, String message) {
            LOGGER.warn("{}: {}", source.pack(), message);
            problems.add(new PackProblem(source.pack(), message));
        }

        Snapshot finish(int sources) {
            Map<String, HelicopterDefinition> helicopters = new LinkedHashMap<>();
            Map<String, PlaneDefinition> planes = new LinkedHashMap<>();
            Map<String, TankDefinition> tanks = new LinkedHashMap<>();
            Map<String, VehicleDefinition> other = new LinkedHashMap<>();
            for (AircraftDefinition def : vehicles.values()) {
                List<String> dropped = def.retainWeapons(weapons.keySet());
                if (!dropped.isEmpty()) {
                    // The original silently skipped AddWeapon lines naming a weapon it had not
                    // loaded, and the shipped AH-64, KV-2 and M26 do that. In a pack it is a typo.
                    if (def.pack().equals(ContentSource.ORIGINAL)) {
                        LOGGER.debug("{}/{}: unknown weapons {}", def.folder(), def.name(), dropped);
                    } else {
                        LOGGER.warn("{}: {}/{}: unknown weapons {}", def.pack(), def.folder(), def.name(), dropped);
                        problems.add(new PackProblem(def.pack(), def.folder() + "/" + def.name() + ": unknown weapons " + dropped));
                    }
                }
                switch (def) {
                    case HelicopterDefinition h -> helicopters.put(h.name(), h);
                    case PlaneDefinition p -> planes.put(p.name(), p);
                    case TankDefinition t -> tanks.put(t.name(), t);
                    case VehicleDefinition v -> other.put(v.name(), v);
                    default -> throw new IllegalArgumentException("Unknown vehicle kind " + def.kind());
                }
            }

            LOGGER.info("Loaded {} helicopters, {} planes, {} tanks, {} vehicles and {} weapons from {} sources",
                    helicopters.size(), planes.size(), tanks.size(), other.size(), weapons.size(), sources);
            if (!unhandled.isEmpty()) {
                LOGGER.debug("Definition keys not implemented yet: {}", unhandled.keySet());
            }
            return new Snapshot(helicopters, planes, tanks, other, weapons, unhandled, problems);
        }
    }

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

    /**
     * What went wrong loading the content packs: files that could not be read, rejected
     * definitions, bad lines and unknown weapons. The import's own known quirks are only logged.
     */
    public List<PackProblem> problems() {
        return snapshot.problems;
    }

    /** One consistent state of the registry. Never changed once published. */
    private record Snapshot(Map<String, HelicopterDefinition> helicopters, Map<String, PlaneDefinition> planes,
                            Map<String, TankDefinition> tanks, Map<String, VehicleDefinition> vehicles,
                            Map<String, WeaponDefinition> weapons, Map<String, Set<String>> unhandledKeys,
                            List<PackProblem> problems) {
        Snapshot {
            helicopters = Collections.unmodifiableMap(helicopters);
            planes = Collections.unmodifiableMap(planes);
            tanks = Collections.unmodifiableMap(tanks);
            vehicles = Collections.unmodifiableMap(vehicles);
            weapons = Collections.unmodifiableMap(weapons);
            unhandledKeys = Collections.unmodifiableMap(unhandledKeys);
            problems = List.copyOf(problems);
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
                    weapons, unhandledKeys, problems);
        }

        Snapshot withHelicopters(Map<String, HelicopterDefinition> map) {
            return new Snapshot(map, planes, tanks, vehicles, weapons, unhandledKeys, problems);
        }

        Snapshot withPlanes(Map<String, PlaneDefinition> map) {
            return new Snapshot(helicopters, map, tanks, vehicles, weapons, unhandledKeys, problems);
        }

        Snapshot withTanks(Map<String, TankDefinition> map) {
            return new Snapshot(helicopters, planes, map, vehicles, weapons, unhandledKeys, problems);
        }

        Snapshot withVehicles(Map<String, VehicleDefinition> map) {
            return new Snapshot(helicopters, planes, tanks, map, weapons, unhandledKeys, problems);
        }

        Snapshot withWeapons(Map<String, WeaponDefinition> map) {
            return new Snapshot(helicopters, planes, tanks, vehicles, map, unhandledKeys, problems);
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
