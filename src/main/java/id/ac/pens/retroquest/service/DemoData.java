package id.ac.pens.retroquest.service;

import static id.ac.pens.retroquest.dao.JdbcDao.*;

import id.ac.pens.retroquest.dao.*;
import id.ac.pens.retroquest.model.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public final class DemoData {
  public static final String PASSWORD = "RetroQuest123!";

  private DemoData() {}

  public static synchronized void seed(Database db) throws Exception {
    if (db.read(
        c -> !list(c, "SELECT id FROM users WHERE email=?", "dosen@retroquest.demo").isEmpty()))
      return;
    var auth = new AuthService(db);
    var service = new QuestService(db);
    User teacher = auth.register("Bu Yunia · Demo", "dosen@retroquest.demo", PASSWORD, true);
    User student = auth.register("Rizqi · Demo", "mahasiswa@retroquest.demo", PASSWORD, false);
    User other = auth.register("Rifqi · Demo", "rifqi@retroquest.demo", PASSWORD, false);
    long room = service.createClass(teacher, "Praktikum PBO · Guild Java");
    String code =
        db.read(c -> one(c, "SELECT join_code FROM classes WHERE id=?", room).text("join_code"));
    service.join(student, code, "Code Warrior");
    service.join(other, code, "Byte Mage");
    var cards =
        List.of(
            new AiService.Card(
                "Objek adalah petualangmu",
                "Class mendefinisikan atribut dan perilaku. Objek adalah instance yang dibuat dari"
                    + " class. Pisahkan tanggung jawab agar setiap objek mudah diuji."),
            new AiService.Card(
                "Enkapsulasi menjaga state",
                "Gunakan atribut private dan metode yang memvalidasi perubahan. Contoh: saldo tidak"
                    + " dapat berubah menjadi negatif melalui withdraw()."),
            new AiService.Card(
                "Polimorfisme dalam aksi",
                "Gunakan interface untuk kontrak perilaku. Pemanggil bekerja dengan"
                    + " DocumentExtractor, sementara PdfExtractor dan DocxExtractor menyediakan"
                    + " implementasinya."));
    long module =
        service.saveModule(
            teacher,
            room,
            "Chapter 01 · Fondasi Object-Oriented",
            "Materi contoh untuk demonstrasi lokal.",
            cards,
            null);
    long first =
        service.saveQuest(
            teacher,
            0,
            module,
            "Misi pertama: Enkapsulasi",
            "Buat class BankAccount dengan atribut private, constructor, deposit, dan withdraw."
                + " Tolak nilai negatif serta penarikan melebihi saldo. Sertakan penjelasan"
                + " desain.",
            LocalDateTime.now().plusDays(7),
            "java,pdf,docx",
            150,
            Rubric.standard(),
            true);
    service.saveQuest(
        teacher,
        0,
        module,
        "Bangun party dengan polimorfisme",
        "Buat interface Character dengan attack(). Implementasikan Warrior dan Mage. Tunjukkan"
            + " pemanggilan polimorfik serta dokumentasikan desainnya.",
        LocalDateTime.now().plusDays(10),
        "java,pdf,docx",
        100,
        Rubric.standard(),
        true);
    service.saveQuest(
        teacher,
        0,
        module,
        "Boss battle: Sistem perpustakaan",
        "Rancang sistem peminjaman buku dengan abstraksi, pewarisan, dan penanganan exception."
            + " Sertakan diagram class dan laporan pengujian.",
        LocalDateTime.now().plusDays(14),
        "java,pdf,docx",
        200,
        Rubric.standard(),
        false);
    Path sample = Files.createTempFile("retroquest-demo-", ".java");
    try {
      Files.writeString(
          sample,
          "// Contoh tugas demo, dibaca secara statis.\n"
              + "class BankAccount {\n"
              + " private long balance;\n"
              + " public BankAccount(long initial) { if(initial<0) throw new"
              + " IllegalArgumentException(); balance=initial; }\n"
              + " public long getBalance() { return balance; }\n"
              + " public void deposit(long amount) { if(amount<=0) throw new"
              + " IllegalArgumentException(); balance=Math.addExact(balance,amount); }\n"
              + " public void withdraw(long amount) { if(amount<=0 || amount>balance) throw new"
              + " IllegalArgumentException(); balance-=amount; }\n"
              + "}\n");
      long sub = service.submit(other, first, sample);
      service.review(
          teacher,
          sub,
          88,
          "Contoh demo: enkapsulasi dan validasi saldo telah diterapkan. Tambahkan pengujian"
              + " kondisi batas.",
          false);
      service.submit(student, first, sample);
    } finally {
      Files.deleteIfExists(sample);
    }
  }
}
