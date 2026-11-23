package id.ac.pens.retroquest.service;

import static id.ac.pens.retroquest.dao.JdbcDao.*;

import id.ac.pens.retroquest.config.AppConfig;
import id.ac.pens.retroquest.dao.*;
import id.ac.pens.retroquest.dao.JdbcDao.Row;
import id.ac.pens.retroquest.model.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

public final class AuthService {
  private final Database db;
  private final Map<String, String> lecturerIds;
  private final String campusDomain;

  public AuthService(Database db) {
    this(
        db,
        AppConfig.get().value("auth.lecturers", ""),
        AppConfig.get().value("auth.email.domain", ""));
  }

  public AuthService(Database db, String lecturerList, String campusDomain) {
    this.db = db;
    this.campusDomain = normalize(campusDomain);
    var entries = new HashMap<String, String>();
    for (String entry : lecturerList.split(",")) {
      if (entry.isBlank()) continue;
      String[] pair = entry.split("=", 2);
      if (pair.length != 2
          || pair[1].isBlank()
          || entries.putIfAbsent(normalizeEmail(pair[0]), pair[1].strip().toUpperCase(Locale.ROOT))
              != null) {
        throw new IllegalArgumentException(
            "Atur auth.lecturers dengan pasangan email=ID yang unik.");
      }
    }
    lecturerIds = Map.copyOf(entries);
  }

  public User register(
      String name, String email, String password, boolean lecturer, String academicId) {
    String address = normalizeEmail(email);
    String role = lecturer ? "LECTURER" : "STUDENT";
    String identity = validateIdentity(address, role, academicId);
    return db.transaction(
        c -> {
          User user = createUser(c, name, address, password, role);
          saveIdentity(c, user.id(), role, identity);
          return user;
        });
  }

  User registerDemo(String name, String email, String password, boolean lecturer) {
    return db.transaction(
        c -> createUser(c, name, email, password, lecturer ? "LECTURER" : "STUDENT"));
  }

  private User createUser(Connection c, String name, String email, String password, String role)
      throws SQLException {
    if (name == null || name.isBlank() || name.strip().length() > 120)
      throw new IllegalArgumentException("Nama wajib diisi, maksimal 120 karakter.");
    if (password == null || password.length() < 8 || password.length() > 128)
      throw new IllegalArgumentException("Password harus 8-128 karakter.");
    try {
      long id =
          insert(
              c,
              "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)",
              name.strip(),
              email,
              Passwords.hash(password),
              role);
      return user(id, name.strip(), email, role);
    } catch (SQLIntegrityConstraintViolationException e) {
      throw new IllegalArgumentException("Email sudah terdaftar.", e);
    }
  }

  public User login(String email, String password) {
    return db.read(
        c -> {
          Row account = authenticate(c, email, password, false);
          String address = account.text("email");
          String role = account.text("role");
          if (DemoData.matchesAccount(address, role, password))
            return user(account.id("id"), account.text("name"), address, role);
          var identities =
              list(
                  c,
                  "SELECT academic_id FROM account_identities WHERE user_id=?",
                  account.id("id"));
          if (identities.isEmpty()
              || !identityIsValid(address, role, identities.getFirst().text("academic_id")))
            throw new IdentityRequiredException();
          return user(account.id("id"), account.text("name"), address, role);
        });
  }

  public User completeIdentity(String oldEmail, String password, String email, String academicId) {
    String address = normalizeEmail(email);
    return db.transaction(
        c -> {
          Row account = authenticate(c, oldEmail, password, true);
          long id = account.id("id");
          String role = account.text("role");
          if (DemoData.matchesAccount(account.text("email"), role, password))
            throw new IllegalArgumentException("Pembaruan identitas ini khusus akun lama.");
          var identities =
              list(c, "SELECT academic_id FROM account_identities WHERE user_id=?", id);
          if (identities.isEmpty()) {
            saveIdentity(c, id, role, validateIdentity(address, role, academicId));
          } else {
            String existingId = identities.getFirst().text("academic_id");
            if (identityIsValid(account.text("email"), role, existingId))
              throw new IllegalArgumentException("Identitas akun ini sudah sesuai.");
            if (!existingId.equals(validateIdentity(address, role, academicId)))
              throw new IllegalArgumentException("Masukkan NIM atau ID dosen yang sudah terdaftar pada akun ini.");
          }
          try {
            update(c, "UPDATE users SET email=? WHERE id=?", address, id);
          } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Email sudah terdaftar.", e);
          }
          return user(id, account.text("name"), address, role);
        });
  }

  private static Row authenticate(Connection c, String email, String password, boolean lock)
      throws SQLException {
    var rows =
        list(
            c, "SELECT * FROM users WHERE email=?" + (lock ? " FOR UPDATE" : ""), normalize(email));
    if (rows.isEmpty()
        || password == null
        || !Passwords.verify(password, rows.getFirst().text("password_hash")))
      throw new IllegalArgumentException("Email atau password salah.");
    return rows.getFirst();
  }

  private boolean identityIsValid(String email, String role, String academicId) {
    try {
      validateIdentity(email, role, academicId);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public String emailExample(boolean lecturer) {
    String baseDomain = campusDomain.isEmpty() ? "kampus.ac.id" : campusDomain;
    return "nama@" + (lecturer ? baseDomain : "student." + baseDomain);
  }

  private String validateIdentity(String email, String role, String academicId) {
    String domain = email.substring(email.indexOf('@') + 1);
    if (domain.equals("retroquest.demo"))
      throw new IllegalArgumentException("Alamat retroquest.demo khusus akun demo.");
    boolean lecturer = role.equals("LECTURER");
    boolean validDomain;
    if (lecturer) {
      validDomain = !Arrays.asList(domain.split("\\.")).contains("student")
          && (campusDomain.isEmpty() || domain.equals(campusDomain));
    } else {
      validDomain = campusDomain.isEmpty() ? domain.matches("student\\.[^.]+(?:\\.[^.]+)+")
          : domain.equals("student." + campusDomain);
    }
    if (!validDomain)
      throw new IllegalArgumentException("Gunakan pola email " + emailExample(lecturer) + ".");
    String id = academicId == null ? "" : academicId.strip().toUpperCase(Locale.ROOT);
    if (lecturer) {
      if (!id.matches("[A-Z0-9-]{4,32}") || !id.equals(lecturerIds.get(email)))
        throw new IllegalArgumentException(
            "Pasangan email dan ID dosen belum terdaftar pada daftar dosen.");
    } else if (!id.matches("[0-9]{10}")) {
      throw new IllegalArgumentException("NIM mahasiswa harus terdiri dari 10 angka.");
    }
    return id;
  }

  private static void saveIdentity(Connection c, long userId, String role, String id)
      throws SQLException {
    try {
      update(
          c,
          "INSERT INTO account_identities(user_id,academic_id,role) VALUES(?,?,?)",
          userId,
          id,
          role);
    } catch (SQLIntegrityConstraintViolationException e) {
      throw new IllegalArgumentException("NIM atau ID dosen sudah digunakan.", e);
    }
  }

  private static String normalize(String value) {
    return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
  }

  private static String normalizeEmail(String email) {
    String address = normalize(email);
    if (address.length() > 180 || !address.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalArgumentException("Alamat email tidak valid.");
    return address;
  }

  private static User user(long id, String name, String email, String role) {
    return role.equals("LECTURER") ? new Lecturer(id, name, email) : new Student(id, name, email);
  }

  public static final class IdentityRequiredException extends IllegalArgumentException {
    private IdentityRequiredException() {
      super("Lengkapi identitas akademik untuk akun lama ini.");
    }
  }
}
