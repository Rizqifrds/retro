package id.ac.pens.retroquest.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Prepared-statement DAO. Semua query domain ditentukan service, bukan input pengguna. */
public final class JdbcDao {
  private JdbcDao() {}

  public record Row(Map<String, Object> values) {
    public String text(String key) {
      Object v = values.get(key);
      return v == null ? "" : v.toString();
    }

    public long id(String key) {
      Object v = values.get(key);
      return v == null ? 0 : ((Number) v).longValue();
    }

    public int number(String key) {
      return (int) id(key);
    }

    public boolean has(String key) {
      return values.get(key) != null;
    }

    public LocalDateTime time(String key) {
      Object v = values.get(key);
      return v instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) v;
    }
  }

  private static PreparedStatement prepare(Connection c, String sql, Object... args)
      throws SQLException {
    var p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    for (int i = 0; i < args.length; i++)
      p.setObject(i + 1, args[i] instanceof LocalDateTime t ? Timestamp.valueOf(t) : args[i]);
    return p;
  }

  public static List<Row> list(Connection c, String sql, Object... args) throws SQLException {
    try (var p = prepare(c, sql, args);
        var r = p.executeQuery()) {
      List<Row> rows = new ArrayList<>();
      var meta = r.getMetaData();
      while (r.next()) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++)
          data.put(meta.getColumnLabel(i).toLowerCase(Locale.ROOT), r.getObject(i));
        rows.add(new Row(Collections.unmodifiableMap(data)));
      }
      return rows;
    }
  }

  public static Row one(Connection c, String sql, Object... args) throws SQLException {
    return list(c, sql, args).stream()
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Data tidak ditemukan atau akses tidak diizinkan."));
  }

  public static int update(Connection c, String sql, Object... args) throws SQLException {
    try (var p = prepare(c, sql, args)) {
      return p.executeUpdate();
    }
  }

  public static long insert(Connection c, String sql, Object... args) throws SQLException {
    try (var p = prepare(c, sql, args)) {
      p.executeUpdate();
      try (var keys = p.getGeneratedKeys()) {
        if (keys.next()) return keys.getLong(1);
        throw new SQLException("ID tidak dihasilkan");
      }
    }
  }
}
