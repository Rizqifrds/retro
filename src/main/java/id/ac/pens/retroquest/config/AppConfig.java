package id.ac.pens.retroquest.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class AppConfig {
  private static final AppConfig INSTANCE = new AppConfig();
  private final Properties values = new Properties();

  private AppConfig() {
    Path path = baseDirectory().resolve("config.properties");
    if (Files.exists(path))
      try (var in = Files.newInputStream(path)) {
        values.load(in);
      } catch (IOException e) {
        throw new IllegalStateException("Konfigurasi tidak dapat dibaca", e);
      }
  }

  public static AppConfig get() {
    return INSTANCE;
  }

  public String value(String key, String fallback) {
    String env = System.getenv("RETROQUEST_" + key.toUpperCase().replace('.', '_'));
    return env != null ? env : values.getProperty(key, fallback);
  }

  private Path baseDirectory() {
    String packaged = System.getProperty("jpackage.app-path");
    return packaged == null
        ? Path.of("").toAbsolutePath()
        : Path.of(packaged).toAbsolutePath().getParent();
  }

  public Path dataDirectory() {
    String packaged = System.getProperty("jpackage.app-path");
    String fallback =
        packaged == null
            ? "./data"
            : Path.of(
                    System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("user.home")),
                    "RetroQuest",
                    "data")
                .toString();
    Path configured = Path.of(value("data.directory", fallback));
    return (configured.isAbsolute() ? configured : baseDirectory().resolve(configured)).normalize();
  }
}
