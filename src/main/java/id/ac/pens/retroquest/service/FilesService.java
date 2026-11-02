package id.ac.pens.retroquest.service;

import id.ac.pens.retroquest.config.AppConfig;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class FilesService {
  public static final long MAX_BYTES = 20L * 1024 * 1024;

  public static String extension(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1);
  }

  public static void validate(Path path, Set<String> types) throws IOException {
    if (!Files.isRegularFile(path) || Files.size(path) == 0 || Files.size(path) > MAX_BYTES)
      throw new IllegalArgumentException("Berkas harus berisi data dan maksimal 20 MB.");
    if (!types.contains(extension(path)))
      throw new IllegalArgumentException("Format berkas diizinkan: " + String.join(", ", types));
  }

  public Path store(Path source, Set<String> types) throws IOException {
    validate(source, types);
    Path folder = AppConfig.get().dataDirectory().resolve("uploads");
    Files.createDirectories(folder);
    Path dest = folder.resolve(UUID.randomUUID() + "." + extension(source));
    Files.copy(source, dest);
    return dest;
  }
}
