package id.ac.pens.retroquest.dao;

import id.ac.pens.retroquest.config.AppConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;

public final class Database {
  private final String url, user, password;

  public Database() {
    this(
        AppConfig.get()
            .value(
                "db.url",
                "jdbc:h2:"
                    + AppConfig.get()
                        .dataDirectory()
                        .resolve("retroquest")
                        .toString()
                        .replace('\\', '/')
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE"),
        AppConfig.get().value("db.user", "sa"),
        AppConfig.get().value("db.password", ""));
  }

  public Database(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public Connection connect() throws SQLException {
    return DriverManager.getConnection(url, user, password);
  }

  public void initialize() {
    try {
      Files.createDirectories(AppConfig.get().dataDirectory());
      try (var in = Database.class.getResourceAsStream("/db/schema.sql");
          var c = connect();
          var s = c.createStatement()) {
        if (in == null) throw new IllegalStateException("Skema database tidak ditemukan");
        for (String sql : new String(in.readAllBytes(), StandardCharsets.UTF_8).split(";"))
          if (!sql.isBlank()) s.execute(sql);
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "Database tidak dapat disiapkan. Periksa konfigurasi JDBC: " + e.getMessage(), e);
    }
  }

  @FunctionalInterface
  public interface Work<T> {
    T run(Connection c) throws Exception;
  }

  public <T> T read(Work<T> work) {
    try (var c = connect()) {
      return work.run(c);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Operasi database gagal: " + e.getMessage(), e);
    }
  }

  public <T> T transaction(Work<T> work) {
    return read(
        c -> {
          c.setAutoCommit(false);
          try {
            T result = work.run(c);
            c.commit();
            return result;
          } catch (Exception e) {
            c.rollback();
            throw e;
          }
        });
  }
}
