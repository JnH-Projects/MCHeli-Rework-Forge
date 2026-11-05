package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.TankDefinition;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/** The kinds of vehicle the workshop can build, with the folder each keeps its files in. */
public enum WorkshopKind {
    HELICOPTER("helicopter", HelicopterDefinition.FOLDER, HelicopterDefinition::new),
    PLANE("plane", PlaneDefinition.FOLDER, PlaneDefinition::new),
    TANK("tank", TankDefinition.FOLDER, TankDefinition::new),
    VEHICLE("vehicle", VehicleDefinition.FOLDER, VehicleDefinition::new);

    private final String id;
    private final String folder;
    private final Function<String, AircraftDefinition> factory;

    WorkshopKind(String id, String folder, Function<String, AircraftDefinition> factory) {
        this.id = id;
        this.folder = folder;
        this.factory = factory;
    }

    public String id() {
        return id;
    }

    public String folder() {
        return folder;
    }

    /** A definition with nothing read into it yet, holding the defaults of this kind. */
    public AircraftDefinition create(String name) {
        return factory.apply(name);
    }

    public static @Nullable WorkshopKind byFolder(String folder) {
        for (WorkshopKind kind : values()) {
            if (kind.folder.equals(folder)) {
                return kind;
            }
        }
        return null;
    }
}
