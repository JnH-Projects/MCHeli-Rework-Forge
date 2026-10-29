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
 * Loaded vehicle definitions, keyed by file name.
 *
 * <p>Definitions have to exist before Forge freezes its registries, because every vehicle gets its
 * own item. The mod constructor therefore loads them straight from the folder the original zip was
 * imported into (see {@link LegacyImporter}), long before the resource manager is available. That
 * also means a resource pack cannot add new vehicles; content packs will get their own loading
 * path later.
 */
public final class ContentRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentRegistry.class);

    private final Map<String, HelicopterDefinition> helicopters;
    private final Map<String, PlaneDefinition> planes;
    private final Map<String, TankDefinition> tanks;
    private final Map<String, VehicleDefinition> vehicles;
    private final Map<String, WeaponDefinition> weapons;
    private final Map<String, Set<String>> unhandledKeys;

    private ContentRegistry(Map<String, HelicopterDefinition> helicopters, Map<String, PlaneDefinition> planes,
                            Map<String, TankDefinition> tanks, Map<String, VehicleDefinition> vehicles,
                            Map<String, WeaponDefinition> weapons, Map<String, Set<String>> unhandledKeys) {
        this.helicopters = Collections.unmodifiableMap(helicopters);
        this.planes = Collections.unmodifiableMap(planes);
        this.tanks = Collections.unmodifiableMap(tanks);
        this.vehicles = Collections.unmodifiableMap(vehicles);
        this.weapons = Collections.unmodifiableMap(weapons);
        this.unhandledKeys = Collections.unmodifiableMap(unhandledKeys);
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

        // The original silently skipped AddWeapon lines naming a weapon it had not loaded. The
        // shipped AH-64, KV-2 and M26 do that, so it is only worth a debug line.
        for (Map<String, ? extends AircraftDefinition> kind : List.of(helicopters, planes, tanks, vehicles)) {
            for (AircraftDefinition def : kind.values()) {
                List<String> dropped = def.retainWeapons(weapons.keySet());
                if (!dropped.isEmpty()) {
                    LOGGER.debug("{}/{}: unknown weapons {}", def.folder(), def.name(), dropped);
                }
            }
        }

        LOGGER.info("Loaded {} helicopters, {} planes, {} tanks, {} vehicles and {} weapons",
                helicopters.size(), planes.size(), tanks.size(), vehicles.size(), weapons.size());
        if (!unhandled.isEmpty()) {
            LOGGER.debug("Definition keys not implemented yet: {}", unhandled.keySet());
        }
        return new ContentRegistry(helicopters, planes, tanks, vehicles, weapons, unhandled);
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

    public Collection<HelicopterDefinition> helicopters() {
        return helicopters.values();
    }

    public HelicopterDefinition helicopter(String name) {
        return helicopters.get(name);
    }

    public Collection<PlaneDefinition> planes() {
        return planes.values();
    }

    public PlaneDefinition plane(String name) {
        return planes.get(name);
    }

    public Collection<TankDefinition> tanks() {
        return tanks.values();
    }

    public TankDefinition tank(String name) {
        return tanks.get(name);
    }

    public Collection<VehicleDefinition> vehicles() {
        return vehicles.values();
    }

    public VehicleDefinition vehicle(String name) {
        return vehicles.get(name);
    }

    public Collection<WeaponDefinition> weapons() {
        return weapons.values();
    }

    public @Nullable WeaponDefinition weapon(String name) {
        return weapons.get(name);
    }

    /** Every definition of every kind: helicopters, planes, tanks, then vehicles. */
    public List<AircraftDefinition> all() {
        List<AircraftDefinition> all = new ArrayList<>(helicopters.size() + planes.size() + tanks.size() + vehicles.size());
        all.addAll(helicopters.values());
        all.addAll(planes.values());
        all.addAll(tanks.values());
        all.addAll(vehicles.values());
        return all;
    }

    /**
     * Looks a definition up by name in any kind. Names are unique across kinds in the bundled
     * content because each one is also an item id.
     */
    public AircraftDefinition any(String name) {
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

    /** Keys found in the files that no handler understood, mapped to the files that use them. */
    public Map<String, Set<String>> unhandledKeys() {
        return unhandledKeys;
    }
}
