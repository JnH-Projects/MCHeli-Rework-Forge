package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.HelicopterDefinition;
import com.jnhprojects.mcheli.content.aircraft.PlaneDefinition;
import com.jnhprojects.mcheli.content.aircraft.VehicleDefinition;
import com.jnhprojects.mcheli.content.aircraft.part.CameraPart;
import com.jnhprojects.mcheli.content.aircraft.part.CrawlerTrack;
import com.jnhprojects.mcheli.content.aircraft.part.LandingGearPart;
import com.jnhprojects.mcheli.content.aircraft.part.MovingPart;
import com.jnhprojects.mcheli.content.aircraft.part.PropellerPart;
import com.jnhprojects.mcheli.content.aircraft.part.RotatingPart;
import com.jnhprojects.mcheli.content.aircraft.part.ThrottlePart;
import com.jnhprojects.mcheli.content.aircraft.part.TrackRoller;
import com.jnhprojects.mcheli.content.aircraft.part.VehiclePart;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponBayPart;
import com.jnhprojects.mcheli.content.aircraft.part.WeaponPart;
import com.jnhprojects.mcheli.content.aircraft.part.WheelPart;
import com.jnhprojects.mcheli.content.aircraft.part.WingPart;
import com.jnhprojects.mcheli.content.aircraft.RotorDefinition;
import com.jnhprojects.mcheli.model.Mesh;
import com.jnhprojects.mcheli.model.MeshGroup;
import com.jnhprojects.mcheli.model.ObjLoader;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Checks a model file before the workshop takes it in, and says what is wrong in terms its author
 * can act on. A file that cannot be used is an error; something that loads but will probably look
 * wrong in game is a warning.
 *
 * @param warnings what the author may want to fix
 * @param min      the smallest corner of the box around the model
 * @param max      the largest corner of the box around the model
 */
public record ModelImport(Mesh mesh, List<String> warnings, float[] min, float[] max) {
    /** What servers allow for a whole pack by default ({@code packs.maxPackSize}). */
    private static final long SERVER_PACK_LIMIT = 8L << 20;
    private static final long MAX_FILE_SIZE = 256L << 20;
    private static final int SNIFF_BYTES = 4096;

    /**
     * @throws WorkshopException when the file cannot be used; the message is meant for the player
     */
    public static ModelImport check(Path file) throws WorkshopException {
        if (!Files.isRegularFile(file)) {
            throw new WorkshopException("No file at " + file);
        }
        String name = file.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".obj")) {
            int dot = lower.lastIndexOf('.');
            throw new WorkshopException("%s is not an OBJ file. Export the model from Blockbench or Blender as Wavefront OBJ (%s files cannot be read)."
                    .formatted(name, dot >= 0 ? lower.substring(dot) : "other"));
        }
        Mesh mesh;
        long size;
        try {
            size = Files.size(file);
            if (size > MAX_FILE_SIZE) {
                throw new WorkshopException("%s is %d MB, more than the %d MB a model may be.".formatted(name, size >> 20, MAX_FILE_SIZE >> 20));
            }
            if (isBinary(file)) {
                throw new WorkshopException(name + " is not a text file, so it is not really an OBJ. Export it again as Wavefront OBJ.");
            }
            try (InputStream in = Files.newInputStream(file)) {
                mesh = ObjLoader.load(name, in);
            }
        } catch (IOException e) {
            throw new WorkshopException(e.getMessage() != null ? e.getMessage() : "Could not read " + name);
        }
        if (mesh.triangleCount() == 0) {
            throw new WorkshopException(name + " has no faces, so there is nothing to draw.");
        }

        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        boolean mapped = false;
        for (MeshGroup group : mesh.groups()) {
            float[] p = group.positions();
            for (int i = 0; i < p.length; i++) {
                min[i % 3] = Math.min(min[i % 3], p[i]);
                max[i % 3] = Math.max(max[i % 3], p[i]);
            }
            for (float uv : group.uvs()) {
                // The loader flips V, so an unmapped corner reads 0 only in U.
                mapped |= uv != 0.0F;
            }
        }

        List<String> warnings = new ArrayList<>();
        if (!mesh.contains("$body")) {
            warnings.add("No $body object: the whole model is drawn as one piece and nothing animates. See the modeling guide.");
        }
        if (!mapped) {
            warnings.add("The model has no texture coordinates, so a texture will not show properly.");
        }
        float largest = Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2]));
        if (largest > 150.0F) {
            warnings.add("The model is %d blocks long. One unit is one block; Blockbench models often need scaling by 1/16.".formatted(Math.round(largest)));
        } else if (largest < 1.0F) {
            warnings.add("The model is less than a block long. One unit is one block; check the export scale.");
        }
        if (size > SERVER_PACK_LIMIT) {
            warnings.add("The file is %d MB. Servers allow 8 MB per pack unless their config says otherwise.".formatted(size >> 20));
        }
        return new ModelImport(mesh, List.copyOf(warnings), min, max);
    }

    private static boolean isBinary(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            for (byte b : in.readNBytes(SNIFF_BYTES)) {
                if (b == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The texture that goes with a model: the PNG the {@code .mtl} next to it names, or one with the
     * same name as the model.
     *
     * @return the file, or {@code null} if there is none
     */
    public static @Nullable Path findTexture(Path obj) {
        String name = obj.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        Path folder = obj.toAbsolutePath().getParent();
        Path mtl = folder.resolve(base + ".mtl");
        if (Files.isRegularFile(mtl)) {
            try {
                for (String line : Files.readAllLines(mtl, StandardCharsets.ISO_8859_1)) {
                    String t = line.trim();
                    if (t.startsWith("map_Kd ")) {
                        Path texture = folder.resolve(t.substring(7).trim());
                        if (Files.isRegularFile(texture) && texture.toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                            return texture;
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                // A material file we cannot read only means looking for the texture by name.
            }
        }
        Path sameName = folder.resolve(base + ".png");
        return Files.isRegularFile(sameName) ? sameName : null;
    }

    /**
     * The {@code $} objects a definition animates that the model does not have. Such a part is not
     * an error, since it can also come as a separate model file, but usually it is a typo or an
     * object the author forgot to name.
     */
    public static List<String> missingParts(AircraftDefinition def, Mesh mesh) {
        Set<String> wanted = new LinkedHashSet<>();
        if (def instanceof HelicopterDefinition heli) {
            for (RotorDefinition rotor : heli.rotors) {
                wanted.add(rotor.part());
            }
        } else if (def instanceof PlaneDefinition plane) {
            for (MovingPart part : plane.nozzles) {
                wanted.add(part.part());
            }
            for (WingPart wing : plane.wings) {
                wanted.add(wing.part());
                for (MovingPart pylon : wing.pylons()) {
                    wanted.add(pylon.part());
                }
            }
            for (PropellerPart rotor : plane.propellers) {
                wanted.add(rotor.part());
                for (PropellerPart.Blade blade : rotor.blades()) {
                    wanted.add(blade.part());
                }
            }
        } else if (def instanceof VehicleDefinition vehicle) {
            for (VehiclePart part : vehicle.parts) {
                wanted.add(part.part());
            }
        }
        def.hatches.forEach(p -> wanted.add(p.part()));
        // The first canopy is registered under two names, and a model only needs one of them.
        for (MovingPart canopy : def.canopies) {
            if (!canopy.part().equals("canopy")) {
                wanted.add(canopy.part());
            }
        }
        for (WeaponBayPart bay : def.weaponBays) {
            wanted.add(bay.door().part());
        }
        def.landingGear.forEach((LandingGearPart p) -> wanted.add(p.part()));
        def.throttleParts.forEach((ThrottlePart p) -> wanted.add(p.part()));
        def.rotatingParts.forEach((RotatingPart p) -> wanted.add(p.part()));
        def.cameraParts.forEach((CameraPart p) -> wanted.add(p.part()));
        def.wheels.forEach((WheelPart p) -> wanted.add(p.part()));
        def.steeringWheels.forEach((WheelPart p) -> wanted.add(p.part()));
        def.trackRollers.forEach((TrackRoller p) -> wanted.add(p.part()));
        def.crawlerTracks.forEach((CrawlerTrack p) -> wanted.add(p.part()));
        def.weaponParts.forEach((WeaponPart p) -> wanted.add(p.part()));

        List<String> missing = new ArrayList<>();
        for (String part : wanted) {
            String object = "$" + part;
            boolean found = mesh.contains(object) || part.equals("canopy0") && mesh.contains("$canopy");
            if (!found) {
                missing.add(object);
            }
        }
        return missing;
    }
}
