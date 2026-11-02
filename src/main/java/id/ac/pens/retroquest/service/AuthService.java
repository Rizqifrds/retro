package id.ac.pens.retroquest.service;

import static id.ac.pens.retroquest.dao.JdbcDao.*;

import id.ac.pens.retroquest.dao.*;
import id.ac.pens.retroquest.model.*;
import java.util.Locale;

public final class AuthService {
  private final Database db;

  public AuthService(Database db) {
    this.db = db;
  }

  public User register(String name, String email, String password, boolean lecturer) {
    if (name == null || name.isBlank() || name.strip().length() > 120)
      throw new IllegalArgumentException("Nama wajib diisi, maksimal 120 karakter.");
    if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || email.length() > 180)
      throw new IllegalArgumentException("Alamat email tidak valid.");
    if (password == null || password.length() < 8 || password.length() > 128)
      throw new IllegalArgumentException("Password harus 8-128 karakter.");
    String normalized = email.strip().toLowerCase(Locale.ROOT);
    return db.transaction(
        c -> {
          if (!list(c, "SELECT id FROM users WHERE email=?", normalized).isEmpty())
            throw new IllegalArgumentException("Email sudah terdaftar.");
          long id =
              insert(
                  c,
                  "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)",
                  name.strip(),
                  normalized,
                  Passwords.hash(password),
                  lecturer ? "LECTURER" : "STUDENT");
          return lecturer
              ? new Lecturer(id, name.strip(), normalized)
              : new Student(id, name.strip(), normalized);
        });
  }

  public User login(String email, String password) {
    return db.read(
        c -> {
          var rows =
              list(c, "SELECT * FROM users WHERE email=?", email.strip().toLowerCase(Locale.ROOT));
          if (rows.isEmpty() || !Passwords.verify(password, rows.getFirst().text("password_hash")))
            throw new IllegalArgumentException("Email atau password salah.");
          var r = rows.getFirst();
          return r.text("role").equals("LECTURER")
              ? new Lecturer(r.id("id"), r.text("name"), r.text("email"))
              : new Student(r.id("id"), r.text("name"), r.text("email"));
        });
  }
}
