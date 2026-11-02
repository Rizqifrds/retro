# RetroQuest

Aplikasi desktop gamifikasi pembelajaran dan pengelolaan tugas praktikum PBO. Dibangun dari proposal RetroQuest dalam folder ini, dengan Java 21, JavaFX 21, Maven, JDBC, dan arsitektur MVC + Service + DAO.

## Jalankan di Windows

Jika paket Windows sudah tersedia, buka **`dist/RetroQuest/RetroQuest.exe`**. Runtime Java sudah disertakan, sehingga tidak memerlukan instalasi Java atau Maven. Mode paket menyimpan data default di `%LOCALAPPDATA%/RetroQuest/data`. `config.properties` opsional diletakkan di sebelah executable.

1. Gunakan **JDK 21**. Jika sudah tersedia di `.tools/jdk21/`, skrip memilihnya secara otomatis. Tidak perlu mengubah Java global komputer.
2. Klik dua kali **`run.cmd`**. Maven dan dependency diunduh pada pemakaian pertama, sehingga perlu internet.
3. Klik **Demo mahasiswa** atau **Demo dosen** untuk mencoba data contoh. Anda juga dapat mendaftarkan akun sendiri.

Data lokal disimpan permanen di `data/`. Tutup aplikasi sebelum menyalin folder ini untuk cadangan. Maven tidak perlu dipasang secara global.

```powershell
.\mvnw.cmd test          # Pengujian aturan bisnis dan integrasi JDBC
.\mvnw.cmd package       # JAR beserta library di target/lib
java -jar target/retroquest-1.0.0.jar
.\scripts\package-windows.ps1 # Buat ulang paket Windows dengan runtime Java
```

JAR harus disertai direktori `lib/` di sebelahnya. JavaFX memerlukan lingkungan desktop. Hasil build Windows membawa library JavaFX Windows; build ulang di OS lain untuk library OS tersebut.

## Alur dosen

1. Daftar sebagai **Dosen / Game Master**, masuk, lalu **Buat kelas**.
2. Bagikan kode guild kepada mahasiswa.
3. **Materi → Unggah / tulis materi**: pilih PDF/DOCX, periksa teks, lalu tulis kartu belajar. Jika AI dikonfigurasi, tombol AI dapat menyusun draf kartu dan quest.
4. **Quest Board → Buat quest** atau **Tinjau draf**: lengkapi instruksi, tenggat, format berkas, EXP, dan rubrik dengan total bobot 100%.
5. Centang pernyataan tinjauan untuk menerbitkan. Tanpa centang, quest tetap DRAFT dan tidak terlihat mahasiswa.
6. **Verification Center**: baca berkas asli, pratinjau teks, rubrik, dan rekomendasi AI bila tersedia. Minta revisi dengan catatan atau tetapkan nilai 0–100 dan setujui.

Tombol **Buka berkas asli** untuk PDF menampilkan dokumen langsung dalam pembaca RetroQuest, dengan navigasi halaman, zoom, dan **Simpan salinan PDF**. Berkas sumber tidak diubah. Pembaca PDF tidak bergantung pada asosiasi browser Windows.

## Alur mahasiswa

1. Daftar sebagai **Mahasiswa / Player**, gabung menggunakan kode kelas, dan pilih arketipe.
2. Pelajari kartu NPC di **Materi**, lalu buka quest dan rubriknya.
3. Unggah tugas sesuai format dan sebelum tenggat. Batas ukuran 20 MB.
4. Pantau **Riwayat Tugas**. EXP tetap 0 sampai dosen menyetujui. Rekomendasi AI dapat diminta secara opsional.
5. Jika dosen meminta revisi, unggah lagi sebagai versi baru. Versi lama tetap tersimpan.
6. Lihat **Profil** dan **Leaderboard**. Level = `1 + EXP / 100` (pembagian bulat), dengan tier 1: level 1–3; tier 2: 4–7; tier 3: 8–10; tier 4: ≥11.

## Akun demonstrasi

| Peran | Email | Password |
|---|---|---|
| Dosen | `dosen@retroquest.demo` | `RetroQuest123!` |
| Mahasiswa | `mahasiswa@retroquest.demo` | `RetroQuest123!` |
| Anggota pembanding | `rifqi@retroquest.demo` | `RetroQuest123!` |

Akun dibuat hanya ketika tombol demo dipilih. Data mencakup dua quest terbit, satu draf, satu contoh tugas menunggu tinjauan, dan satu contoh tugas yang telah disetujui. Akun mahasiswa utama mulai dari 0 EXP. Gunakan akun pribadi untuk data sebenarnya.

## MySQL, AI, dan OCR

Salin `config.example.properties` menjadi `config.properties`, kemudian sesuaikan. Rahasia konfigurasi dikecualikan dari Git. Semua properti dapat diatur melalui environment dengan awalan `RETROQUEST_`, misalnya `RETROQUEST_AI_KEY`, `RETROQUEST_DB_URL`, dan `RETROQUEST_OCR_COMMAND`.

- **Database default:** H2 persisten, tersedia langsung untuk demonstrasi. **MySQL:** buat database kosong terlebih dahulu; skema dibuat otomatis pada startup. Contoh JDBC dan akun ada di konfigurasi.
- **AI:** isi endpoint lengkap layanan kompatibel Chat Completions, model, dan API key bila diperlukan. Aplikasi mengirim JSON dengan `messages` dan `response_format: json_object`. Dukungan format ini bergantung pada penyedia. Teks dokumen, instruksi, dan rubrik hanya dikirim ketika pengguna memilih fitur AI dan menyetujui dialog pengiriman. Konten keluaran selalu berupa draf/rekomendasi.
- **OCR:** pasang Tesseract secara terpisah, lalu isi path executable dan bahasa yang terpasang. PDF tanpa teks dirender per halaman untuk OCR. Gambar PNG/JPG/JPEG juga didukung. Maksimal 40 halaman untuk PDF OCR, timeout 60 detik per halaman.
- **Berkas tugas:** PDF, DOCX, Java, TXT, PNG, JPG, JPEG sesuai format quest. Kode Java dibaca sebagai teks; tidak dikompilasi atau dijalankan oleh aplikasi.
- **Jika layanan gagal:** pesan kegagalan evaluasi disimpan; tugas tetap dapat diperiksa dan dinilai manual.

Untuk beberapa komputer, gunakan MySQL yang sama dan direktori `data.directory` yang dapat diakses semua klien melalui path bersama yang konsisten. Database menyimpan path lampiran, sehingga koneksi MySQL saja belum cukup untuk berbagi berkas. H2 lokal untuk satu proses aplikasi; berpindah peran dapat dilakukan dengan Keluar lalu masuk akun lain.

## Isi proyek

| Lokasi | Isi |
|---|---|
| `src/main/java/id/ac/pens/retroquest/model` | User abstrak, Lecturer, Student, Progress, Rubric |
| `.../dao` | Database, transaksi JDBC, prepared-statement DAO |
| `.../service` | Autentikasi, quest, EXP, dokumen, OCR, AI, dan data demo |
| `.../ui` | Controller JavaFX dan factory avatar pixel |
| `src/main/resources/ui` | Shell FXML dan tema CSS |
| `src/main/resources/db/schema.sql` | Sembilan tabel beserta foreign key, check, dan unique constraint |
| `src/test/java` | Pengujian positif, negatif, konkurensi, dan rollback |
| `docs/RANCANGAN.md` | Sketsa layar, alur, ERD, dan penerapan OOP |
| `docs/DEMONSTRASI.md` | Panduan presentasi dan skenario uji manual |
| `docs/STATUS.md` | Cakupan yang selesai dan batas verifikasi |
| `docs/screenshots` | Screenshot scene JavaFX dari pengujian antarmuka |
| `examples/BankAccount.java` | Contoh berkas tugas |

## Aturan penting

Persetujuan, transaksi EXP, perubahan total EXP, dan achievement berada dalam **satu transaksi database**. Unique constraint pasangan anggota–quest dan row lock mencegah reward ganda, termasuk saat persetujuan bersamaan. Hanya versi terbaru yang dapat dinilai. Quest terbit dibekukan agar instruksi, reward, dan rubrik yang dilihat mahasiswa tetap konsisten. Mahasiswa tidak dapat membuka submission mahasiswa lain.

Password di-hash dengan PBKDF2-HMAC-SHA256, salt acak, dan 210.000 iterasi. Registrasi dosen terbuka untuk kebutuhan proyek kampus; deployment institusi memerlukan proses pemberian peran dosen oleh administrator. Aplikasi ini belum dilengkapi server distribusi berkas, pemulihan password, migrasi database lintas versi, atau installer bertanda tangan.

## Rujukan implementasi

Integrasi Maven/JavaFX mengikuti [dokumentasi resmi OpenJFX](https://openjfx.io/openjfx-docs/maven). Pembacaan PDF menggunakan API `Loader` sesuai [panduan PDFBox 3](https://pdfbox.apache.org/3.0/migration.html). Proposal PDF di folder utama menjadi acuan kebutuhan; dokumen sumber tidak diubah.
