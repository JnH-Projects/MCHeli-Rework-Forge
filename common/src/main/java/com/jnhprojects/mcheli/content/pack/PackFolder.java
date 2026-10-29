package com.jnhprojects.mcheli.content.pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Finds the content packs in a folder, {@code <game dir>/mcheli/packs} in the game: every zip file
 * and every folder in it. Other files (a readme, a stray image) are ignored.
 *
 * <p>A pack that cannot be used is reported and left out; the scan itself never fails. Two packs
 * with the same id cannot both load, so the second in file name order is reported and left out.
 */
public final class PackFolder {
    private PackFolder() {
    }

    /**
     * @param packs the open packs, sorted by id; the caller closes them
     * @param problems one entry per pack that was left out
     */
    public record Scan(List<ContentPack> packs, List<PackProblem> problems) {
    }

    public static Scan scan(Path folder) {
        List<Path> entries;
        try (Stream<Path> list = Files.list(folder)) {
            entries = list.filter(PackFolder::isCandidate).sorted().toList();
        } catch (NoSuchFileException e) {
            return new Scan(List.of(), List.of());
        } catch (IOException e) {
            return new Scan(List.of(), List.of(new PackProblem(folder.getFileName().toString(), "could not be listed: " + e)));
        }

        Map<String, ContentPack> byId = new TreeMap<>();
        List<PackProblem> problems = new ArrayList<>();
        for (Path entry : entries) {
            String fileName = entry.getFileName().toString();
            try {
                ContentPack pack = ContentPack.open(entry);
                ContentPack clash = byId.get(pack.id());
                if (clash != null) {
                    problems.add(new PackProblem(fileName, "left out, %s already uses the id \"%s\""
                            .formatted(clash.source().getFileName(), pack.id())));
                    pack.close();
                } else {
                    byId.put(pack.id(), pack);
                }
            } catch (PackException e) {
                problems.add(new PackProblem(fileName, e.getMessage()));
            } catch (IOException | RuntimeException e) {
                problems.add(new PackProblem(fileName, "could not be read: " + e));
            }
        }
        return new Scan(List.copyOf(byId.values()), List.copyOf(problems));
    }

    private static boolean isCandidate(Path entry) {
        String name = entry.getFileName().toString();
        if (name.startsWith(".")) {
            return false;
        }
        return Files.isDirectory(entry) || (name.toLowerCase(Locale.ROOT).endsWith(".zip") && Files.isRegularFile(entry));
    }
}
