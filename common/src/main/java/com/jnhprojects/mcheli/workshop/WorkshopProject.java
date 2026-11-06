package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.DefinitionReader;
import com.jnhprojects.mcheli.content.DefinitionValues;
import com.jnhprojects.mcheli.content.aircraft.AircraftDefinition;
import com.jnhprojects.mcheli.content.aircraft.WeaponGroup;
import com.jnhprojects.mcheli.content.aircraft.WeaponSound;
import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.PackException;
import com.jnhprojects.mcheli.content.pack.PackFolder;
import com.jnhprojects.mcheli.content.pack.PackManifest;
import com.jnhprojects.mcheli.model.PartTextures;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * One vehicle open in the workshop: a definition file and a model in a content pack.
 *
 * <p>What the workshop edits and saves is an ordinary pack folder in {@code mcheli/packs}, so the
 * game loads it like any other, a server sends it to its players, and anything written by hand
 * can be opened and changed further. A pack that is a zip is never touched: opening one makes a
 * folder copy with {@value #COPY_SUFFIX} added to its id, whose definitions win over the original's
 * because packs load in id order. Zips also stay open while in use, and overwriting one could not
 * work on Windows.
 */
public final class WorkshopProject {
    /** Added to the id of a zip pack's folder copy. */
    public static final String COPY_SUFFIX = "-edit";

    private static final Pattern VEHICLE_NAME = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
    private static final Pattern FILE_SAFE = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final int PLAIN_TEXTURE_GREY = 0xB0;
    private static final String PART_TEXTURE = "PartTexture";
    private static final String WEAPON_SOUND = "WeaponSound";

    private final Path root;
    private final WorkshopKind kind;
    private final String vehicle;
    private final DefinitionDocument document;
    private PackManifest manifest;
    private final ModelImport model;
    private AircraftDefinition definition;
    private List<String> problems = List.of();
    private List<String> saved;

    /** A vehicle a pack holds, as the workshop lists it. */
    public record Entry(String pack, Path source, boolean zipped, WorkshopKind kind, String vehicle) {
    }

    private WorkshopProject(Path root, PackManifest manifest, WorkshopKind kind, String vehicle, String text, ModelImport model) {
        this.root = root;
        this.manifest = manifest;
        this.kind = kind;
        this.vehicle = vehicle;
        this.document = new DefinitionDocument(text);
        this.model = model;
        this.saved = document.snapshot();
        this.definition = kind.create(vehicle);
        refresh();
    }

    /**
     * Starts a vehicle: makes the pack folder, takes in the model and its texture and writes a first
     * definition with a pilot seat, so there is something to fly from the start.
     *
     * @param packs      {@code mcheli/packs}
     * @param objFile    the model to take in
     * @param texture    its texture, or {@code null} to start with plain grey
     * @throws WorkshopException when something is wrong with the names or the model
     */
    public static WorkshopProject create(Path packs, String packId, WorkshopKind kind, String vehicle, String author,
                                         Path objFile, @Nullable Path texture) throws WorkshopException {
        if (!PackManifest.isValidId(packId)) {
            throw new WorkshopException("A pack id is 1 to 64 lower case letters, digits, '-', '_' and '.'; \"%s\" will not do.".formatted(packId));
        }
        if (!VEHICLE_NAME.matcher(vehicle).matches()) {
            throw new WorkshopException("A vehicle name is 1 to 64 lower case letters, digits, '-' and '_'; \"%s\" will not do.".formatted(vehicle));
        }
        Path root = packs.resolve(packId);
        if (Files.exists(root) || Files.exists(packs.resolve(packId + ".zip"))) {
            throw new WorkshopException("There is already a pack called %s. Open it from the list or pick another id.".formatted(packId));
        }
        ModelImport model = ModelImport.check(objFile);

        PackManifest manifest = new PackManifest(packId, vehicle, author, "1.0.0", "");
        String text = "// Made with the MCHeli Rework workshop. The keys are explained in docs/content/aircraft.md.\n"
                + "DisplayName = " + vehicle + "\n"
                + "AddSeat = 0, 1, 0\n";
        try {
            Path assets = root.resolve("assets/mcheli");
            Path modelFile = assets.resolve("models").resolve(kind.folder()).resolve(vehicle + ".obj");
            Path textureFile = assets.resolve("textures").resolve(kind.folder()).resolve(vehicle + ".png");
            Files.createDirectories(modelFile.getParent());
            Files.createDirectories(textureFile.getParent());
            Files.copy(objFile, modelFile);
            if (texture != null) {
                Files.copy(texture, textureFile);
            } else {
                Files.write(textureFile, plainTexture());
            }
            WorkshopProject project = new WorkshopProject(root, manifest, kind, vehicle, text, model);
            project.write();
            return project;
        } catch (IOException e) {
            throw new WorkshopException("Could not write the pack: " + e);
        }
    }

    /**
     * Opens a vehicle from the list. A vehicle in a zip pack opens from a folder copy, made the first
     * time and reused after that.
     *
     * @param packs {@code mcheli/packs}
     */
    public static WorkshopProject open(Path packs, Entry entry) throws WorkshopException {
        try {
            Path root = entry.source();
            String id = entry.pack();
            if (entry.zipped()) {
                id = id + COPY_SUFFIX;
                root = packs.resolve(id);
                if (!Files.exists(root)) {
                    unpack(entry.source(), root, id);
                }
            }
            PackManifest manifest;
            try (ContentPack pack = ContentPack.open(root)) {
                manifest = pack.manifest();
            }
            Path definitionFile = root.resolve("assets/mcheli").resolve(entry.kind().folder()).resolve(entry.vehicle() + ".txt");
            String text = DefinitionReader.decode(Files.readAllBytes(definitionFile));
            Path modelFile = root.resolve("assets/mcheli/models").resolve(entry.kind().folder()).resolve(entry.vehicle() + ".obj");
            if (!Files.isRegularFile(modelFile)) {
                throw new WorkshopException("%s has no OBJ model in its pack. The workshop only edits OBJ models; an MQO has to be converted first.".formatted(entry.vehicle()));
            }
            return new WorkshopProject(root, manifest, entry.kind(), entry.vehicle(), text, ModelImport.check(modelFile));
        } catch (IOException e) {
            throw new WorkshopException("Could not open %s: %s".formatted(entry.vehicle(), e));
        } catch (PackException e) {
            throw new WorkshopException(entry.pack() + ": " + e.getMessage());
        }
    }

    /** Every vehicle in {@code mcheli/packs}, by pack and name. Packs that cannot be read are left out. */
    public static List<Entry> list(Path packs) {
        List<Entry> entries = new ArrayList<>();
        PackFolder.Scan scan = PackFolder.scan(packs);
        for (ContentPack pack : scan.packs()) {
            try (pack) {
                boolean zipped = !Files.isDirectory(pack.source());
                for (WorkshopKind kind : WorkshopKind.values()) {
                    String prefix = "assets/mcheli/" + kind.folder() + "/";
                    for (String file : pack.files()) {
                        if (file.startsWith(prefix) && file.endsWith(".txt") && file.indexOf('/', prefix.length()) < 0) {
                            entries.add(new Entry(pack.id(), pack.source(), zipped, kind,
                                    file.substring(prefix.length(), file.length() - 4)));
                        }
                    }
                }
            } catch (IOException e) {
                // Listing only reads names, so a pack that will not close costs nothing here.
            }
        }
        entries.sort(Comparator.comparing(Entry::pack).thenComparing(Entry::vehicle));
        return entries;
    }

    private static void unpack(Path zip, Path to, String id) throws IOException, PackException {
        Path temp = to.resolveSibling(to.getFileName() + ".tmp");
        deleteTree(temp);
        try (ContentPack pack = ContentPack.open(zip)) {
            Path top = pack.assets().getParent().getParent();
            for (String file : pack.files()) {
                Path target = temp.resolve(file);
                Files.createDirectories(target.getParent());
                try (InputStream in = Files.newInputStream(top.resolve(file))) {
                    Files.copy(in, target);
                }
            }
            PackManifest old = pack.manifest();
            PackManifest copy = new PackManifest(id, old.name(), old.author(), old.version(), old.description());
            Files.writeString(temp.resolve(PackManifest.FILE), copy.text());
        }
        Files.move(temp, to);
    }

    private static void deleteTree(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    /** The pack folder. */
    public Path root() {
        return root;
    }

    /** The pack's {@code assets/mcheli} folder, for the runtime assets. */
    public Path assets() {
        return root.resolve("assets/mcheli");
    }

    public WorkshopKind kind() {
        return kind;
    }

    public String vehicle() {
        return vehicle;
    }

    public DefinitionDocument document() {
        return document;
    }

    public PackManifest manifest() {
        return manifest;
    }

    public void setManifest(PackManifest manifest) {
        this.manifest = manifest;
    }

    public ModelImport model() {
        return model;
    }

    /** The definition as the game would read it now. After an edit it could not use, the last good one. */
    public AircraftDefinition definition() {
        return definition;
    }

    /** What is wrong with the definition as it stands; empty when the game takes it without complaint. */
    public List<String> problems() {
        return problems;
    }

    /** Reads the document again. Call after every edit. */
    public void refresh() {
        List<String> found = new ArrayList<>();
        try {
            DefinitionReader.Result<AircraftDefinition> result = DefinitionReader.read(document.text(), kind.create(vehicle));
            found.addAll(result.problems());
            if (result.isValid()) {
                definition = result.definition();
                for (String part : ModelImport.missingParts(definition, model.mesh())) {
                    found.add("the model has no " + part + " object, so that part will not show");
                }
            }
        } catch (IOException e) {
            found.add("could not read the definition: " + e);
        }
        problems = List.copyOf(found);
    }

    /** Parts of a definition, in file order, for this vehicle kind. */
    public List<WorkshopPart> parts() {
        List<WorkshopPart> parts = new ArrayList<>();
        for (DefinitionDocument.Entry entry : document.entries()) {
            PartType type = PartType.byKey(entry.key());
            if (type != null && type.appliesTo(kind)) {
                parts.add(new WorkshopPart(type, entry.line(), entry.key(), entry.value()));
            }
        }
        return parts;
    }

    public @Nullable WorkshopPart partAt(int line) {
        if (line < 0 || line >= document.size()) {
            return null;
        }
        DefinitionDocument.Entry entry = document.entry(line);
        PartType type = entry != null ? PartType.byKey(entry.key()) : null;
        return type != null && type.appliesTo(kind) ? new WorkshopPart(type, line, entry.key(), entry.value()) : null;
    }

    /**
     * Adds a part where the parser expects it: a part with a parent right behind the given parent
     * and its other children, anything else behind the last line of its own type, or at the end.
     *
     * @param parentLine the selected line, used to find the parent of a part that needs one
     * @return the line of the part, an existing one for a type there can only be one of, or -1
     *         when it needs a parent and there is none
     */
    public int addPart(PartType type, int parentLine, String value) {
        List<WorkshopPart> parts = parts();
        if (type.isSingle()) {
            for (WorkshopPart part : parts) {
                if (part.type() == type) {
                    return part.line();
                }
            }
        }
        int at;
        if (type.parent() != null) {
            at = afterParent(type, parts, parentLine);
            if (at < 0) {
                return -1;
            }
        } else {
            at = document.size();
            for (WorkshopPart part : parts) {
                if (part.type() == type) {
                    at = part.line() + 1;
                }
            }
        }
        document.insert(at, type.key(), value);
        return at;
    }

    private static int afterParent(PartType type, List<WorkshopPart> parts, int selected) {
        int parent = -1;
        for (WorkshopPart part : parts) {
            if (part.type() == type.parent() && part.line() <= selected) {
                parent = part.line();
            }
        }
        if (parent < 0) {
            // Nothing selected on or under a parent: the last parent there is.
            for (WorkshopPart part : parts) {
                if (part.type() == type.parent()) {
                    parent = part.line();
                }
            }
        }
        if (parent < 0) {
            return -1;
        }
        int at = parent + 1;
        for (WorkshopPart part : parts) {
            if (part.line() >= at && part.type() == type) {
                at = part.line() + 1;
            } else if (part.line() >= at && part.type() != type) {
                break;
            }
        }
        return at;
    }

    // Textures

    /** The objects of the model that can have a texture of their own, by part name. */
    public List<String> texturableParts() {
        return PartTextures.names(model.mesh());
    }

    /** The name of the texture a part has of its own, or {@code null} if it wears the vehicle's paint. */
    public @Nullable String partTexture(String part) {
        String value = document.keyed(PART_TEXTURE, part);
        String[] values = value != null ? value.split(",") : new String[0];
        return values.length == 2 ? DefinitionValues.lower(values[1].trim()) : null;
    }

    /** The vehicle's own texture, the one a freshly placed vehicle wears. */
    public String mainTexture() {
        return definition.textures().getFirst();
    }

    /**
     * Copies a PNG in as a part's texture, or as the vehicle's own with {@code part} {@code null}. The
     * file replaces the one in the pack straight away, which is what makes the preview live. The
     * definition line is a normal edit: it can be undone, and needs saving.
     *
     * @return the texture's name and what its author may want to fix
     * @throws WorkshopException when the file cannot be used or copied
     */
    public ImportedTexture importTexture(@Nullable String part, Path png) throws WorkshopException {
        AssetImport.Texture checked = AssetImport.texture(png);
        String name = part != null ? vehicle + "_" + fileSafe(part) : mainTexture();
        copyIn(png, assets().resolve("textures").resolve(kind.folder()).resolve(name + ".png"));
        if (part != null) {
            document.setKeyed(PART_TEXTURE, part, part + ", " + name);
        }
        return new ImportedTexture(name, checked);
    }

    /** A texture now in the pack. */
    public record ImportedTexture(String name, AssetImport.Texture checked) {
    }

    /** Puts a part back in the vehicle's paint. The PNG stays in the pack, for undo. */
    public void clearPartTexture(String part) {
        document.setKeyed(PART_TEXTURE, part, null);
    }

    // Sounds

    /** What a sound belongs to: the engine, the rotor, or one of the vehicle's weapons. */
    public enum SoundKind {
        ENGINE, ROTOR, WEAPON
    }

    /** @param weapon the weapon's name, for a {@link SoundKind#WEAPON} */
    public record SoundSlot(SoundKind kind, @Nullable String weapon) {
    }

    /** Everything on this vehicle that can have a sound. Tanks and cars have no rotor. */
    public List<SoundSlot> soundSlots() {
        List<SoundSlot> slots = new ArrayList<>();
        slots.add(new SoundSlot(SoundKind.ENGINE, null));
        if (kind == WorkshopKind.HELICOPTER || kind == WorkshopKind.PLANE) {
            slots.add(new SoundSlot(SoundKind.ROTOR, null));
        }
        Set<String> weapons = new LinkedHashSet<>();
        for (WeaponGroup group : definition.weaponGroups) {
            weapons.add(group.weaponType());
        }
        for (String weapon : weapons) {
            slots.add(new SoundSlot(SoundKind.WEAPON, weapon));
        }
        return slots;
    }

    /** The name of the sound set for a slot in the definition, or {@code null} if none is. */
    public @Nullable String sound(SoundSlot slot) {
        return switch (slot.kind()) {
            case ENGINE -> document.value("Sound");
            case ROTOR -> document.value("RotorSound");
            case WEAPON -> {
                WeaponSound own = definition.weaponSounds.get(slot.weapon());
                yield own != null ? own.sound() : null;
            }
        };
    }

    /**
     * Copies an OGG in as a slot's sound, replacing the one in the pack straight away, and sets the
     * definition line, which is a normal edit.
     *
     * @return the sound's name
     * @throws WorkshopException when the file cannot be used or copied
     */
    public ImportedSound importSound(SoundSlot slot, Path ogg) throws WorkshopException {
        List<String> warnings = AssetImport.sound(ogg);
        String name = vehicle + "_" + switch (slot.kind()) {
            case ENGINE -> "engine";
            case ROTOR -> "rotor";
            case WEAPON -> fileSafe(slot.weapon());
        };
        copyIn(ogg, assets().resolve("sounds").resolve(name + ".ogg"));
        setSound(slot, name);
        return new ImportedSound(name, warnings);
    }

    /** A sound now in the pack. */
    public record ImportedSound(String name, List<String> warnings) {
    }

    /** Sets or, with {@code null}, clears a slot's sound. A file already in the pack stays there. */
    public void setSound(SoundSlot slot, @Nullable String name) {
        switch (slot.kind()) {
            case ENGINE -> document.set("Sound", name);
            case ROTOR -> document.set("RotorSound", name);
            case WEAPON -> {
                WeaponSound own = definition.weaponSounds.get(slot.weapon());
                Float pitch = own != null ? own.pitch() : null;
                document.setKeyed(WEAPON_SOUND, slot.weapon(), name == null ? null
                        : slot.weapon() + ", " + name + (pitch != null ? ", " + WorkshopPart.number(pitch) : ""));
            }
        }
    }

    private static String fileSafe(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    /** A crash halfway through leaves the old file. Dropping a file from the pack itself changes nothing. */
    private static void copyIn(Path from, Path to) throws WorkshopException {
        try {
            Files.createDirectories(to.getParent());
            if (Files.exists(to) && Files.isSameFile(from, to)) {
                return;
            }
            Path temp = to.resolveSibling(to.getFileName() + ".tmp");
            Files.copy(from, temp, StandardCopyOption.REPLACE_EXISTING);
            move(temp, to);
        } catch (IOException e) {
            throw new WorkshopException("Could not copy %s: %s".formatted(from.getFileName(), e));
        }
    }

    /** Whether the document differs from what is on disk. */
    public boolean isDirty() {
        return !document.snapshot().equals(saved);
    }

    /** Writes the definition and the manifest to the pack folder. */
    public void save() throws WorkshopException {
        try {
            write();
            saved = document.snapshot();
        } catch (IOException e) {
            throw new WorkshopException("Could not save: " + e);
        }
    }

    private void write() throws IOException {
        Path file = assets().resolve(kind.folder()).resolve(vehicle + ".txt");
        Files.createDirectories(file.getParent());
        writeAtomically(file, document.text());
        writeAtomically(root.resolve(PackManifest.FILE), manifest.text());
    }

    /** A crash halfway through a save leaves the old file rather than half a new one. */
    private static void writeAtomically(Path file, String text) throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, text, StandardCharsets.UTF_8);
        move(temp, file);
    }

    private static void move(Path temp, Path file) throws IOException {
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Saves, checks the result the way the game will and zips it up for sharing.
     *
     * @param folder where the zip goes
     * @return the zip file
     */
    public Path export(Path folder) throws WorkshopException {
        save();
        try (ContentPack pack = ContentPack.open(root)) {
            Files.createDirectories(folder);
            String name = FILE_SAFE.matcher(manifest.id() + "-" + manifest.version()).replaceAll("_").toLowerCase(Locale.ROOT) + ".zip";
            Path zip = folder.resolve(name);
            Files.write(zip, pack.archive());
            return zip;
        } catch (IOException e) {
            throw new WorkshopException("Could not export: " + e);
        } catch (PackException e) {
            throw new WorkshopException("The pack would not load: " + e.getMessage());
        }
    }

    /** A small plain PNG, so that a model without a texture of its own still draws. */
    static byte[] plainTexture() throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflate = new DeflaterOutputStream(raw, new Deflater())) {
            // One row of one RGBA pixel, after its filter byte.
            deflate.write(new byte[]{0, (byte) PLAIN_TEXTURE_GREY, (byte) PLAIN_TEXTURE_GREY, (byte) PLAIN_TEXTURE_GREY, (byte) 0xFF});
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(png)) {
            out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            try (DataOutputStream h = new DataOutputStream(header)) {
                h.writeInt(1);
                h.writeInt(1);
                h.write(new byte[]{8, 6, 0, 0, 0});
            }
            chunk(out, "IHDR", header.toByteArray());
            chunk(out, "IDAT", raw.toByteArray());
            chunk(out, "IEND", new byte[0]);
        }
        return png.toByteArray();
    }

    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.writeInt(data.length);
        out.write(name);
        out.write(data);
        out.writeInt((int) crc.getValue());
    }
}
