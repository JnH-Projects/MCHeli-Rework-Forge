package com.jnhprojects.mcheli.workshop;

import com.jnhprojects.mcheli.content.pack.ContentPack;
import com.jnhprojects.mcheli.content.pack.PackException;
import com.jnhprojects.mcheli.content.pack.PackFolder;
import com.jnhprojects.mcheli.content.pack.PackManifest;
import com.jnhprojects.mcheli.content.pack.PackProblem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** What the package browser shows: the packs a player has, who made them and what is in them. */
public final class PackCatalog {
    private static final String WEAPONS = "assets/mcheli/weapons/";

    private PackCatalog() {
    }

    /**
     * One pack as the browser lists it, with nothing left open.
     *
     * @param vehicles the vehicle names it holds
     * @param weapons  the weapon names it holds
     * @param size     its size as a zip, in bytes
     */
    public record Info(PackManifest manifest, Path source, boolean zipped, long size, String hash,
                       List<String> vehicles, List<String> weapons) {
    }

    /**
     * @param packs    the packs that can be used, by id
     * @param problems the ones that cannot, with the reason
     */
    public record Listing(List<Info> packs, List<PackProblem> problems) {
    }

    /** Reads every pack in {@code mcheli/packs}. */
    public static Listing read(Path folder) {
        PackFolder.Scan scan = PackFolder.scan(folder);
        List<Info> infos = new ArrayList<>();
        List<PackProblem> problems = new ArrayList<>(scan.problems());
        for (ContentPack pack : scan.packs()) {
            try (pack) {
                infos.add(info(pack));
            } catch (IOException e) {
                problems.add(new PackProblem(pack.id(), "could not be read: " + e));
            }
        }
        return new Listing(List.copyOf(infos), List.copyOf(problems));
    }

    /** Describes an open pack, such as one a server sent. */
    public static Info info(ContentPack pack) throws IOException {
        List<String> vehicles = new ArrayList<>();
        List<String> weapons = new ArrayList<>();
        for (String file : pack.files()) {
            if (!file.endsWith(".txt")) {
                continue;
            }
            for (WorkshopKind kind : WorkshopKind.values()) {
                addName(file, "assets/mcheli/" + kind.folder() + "/", vehicles);
            }
            addName(file, WEAPONS, weapons);
        }
        return new Info(pack.manifest(), pack.source(), !Files.isDirectory(pack.source()), pack.archiveSize(), pack.hash(),
                List.copyOf(vehicles), List.copyOf(weapons));
    }

    private static void addName(String file, String prefix, List<String> names) {
        if (file.startsWith(prefix) && file.indexOf('/', prefix.length()) < 0) {
            names.add(file.substring(prefix.length(), file.length() - 4));
        }
    }

    /**
     * Copies a zip someone gave the player into {@code mcheli/packs}, after checking it the way the
     * game will. A pack with the same id is left alone, since the player may have changes in it.
     *
     * @return the pack's manifest
     * @throws WorkshopException when it is not a usable pack or the id is taken
     */
    public static PackManifest install(Path zip, Path folder) throws WorkshopException {
        PackManifest manifest;
        try (ContentPack pack = ContentPack.open(zip)) {
            manifest = pack.manifest();
        } catch (PackException e) {
            throw new WorkshopException(zip.getFileName() + ": " + e.getMessage());
        } catch (IOException | RuntimeException e) {
            throw new WorkshopException("Could not read %s: %s".formatted(zip.getFileName(), e));
        }
        for (Info existing : read(folder).packs()) {
            if (existing.manifest().id().equals(manifest.id())) {
                throw new WorkshopException("You already have a pack called %s (%s). Remove it first to replace it."
                        .formatted(manifest.id(), existing.source().getFileName()));
            }
        }
        Path target = folder.resolve(manifest.id() + ".zip");
        if (Files.exists(target)) {
            throw new WorkshopException("mcheli/packs already holds a file called %s.".formatted(target.getFileName()));
        }
        try {
            Files.createDirectories(folder);
            Files.copy(zip, target);
        } catch (IOException e) {
            throw new WorkshopException("Could not copy the pack: " + e);
        }
        return manifest;
    }
}
