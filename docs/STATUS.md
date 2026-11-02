# Status implementasi

Tanggal pemeriksaan: 2 November 2026. Acuan utama: proposal RetroQuest dalam folder proyek.

## Tersedia

- Registrasi dan login dua peran, password PBKDF2, akun demo opsional.
- Kelas/guild, kode undangan, keanggotaan dan pembatasan akses.
- Unggah PDF/DOCX, ekstraksi PDFBox/POI, penulisan dan perbaikan materi manual.
- Integrasi REST AI untuk draf kartu/quest/rubrik dan rekomendasi nilai, dengan validasi keluaran JSON dan konfirmasi sebelum pengiriman teks.
- Integrasi Tesseract untuk gambar dan PDF tanpa teks; jalur manual ketika gagal.
- Kartu micro-learning dengan NPC, editor quest/rubrik, draf dan publikasi dosen.
- Tenggat, validasi format/ukuran, salinan lampiran, versi submission dan revisi.
- Verification Center: pratinjau statis, berkas asli, riwayat rekomendasi, nilai dan catatan dosen.
- PDF asli dibaca langsung melalui viewer internal dengan halaman, zoom, dan ekspor salinan byte asli; tidak perlu membuka browser eksternal.
- Transaksi approval + reward sekali per quest, level/tier, tiga arketipe avatar pixel, badge, leaderboard per kelas.
- Task latar belakang, notifikasi progres, refresh 15 detik, database H2 lokal dan konfigurasi MySQL.
- Source Maven, SQL, sketsa, diagram, panduan demo, dan screenshot hasil render pengujian JavaFX.

## Bukti pengujian

Perintah lengkap: `mvnw.cmd -q -Dretroquest.guiTest=true test package` melalui PowerShell (kutip argumen `'-Dretroquest.guiTest=true'`).

| Kelompok | Jumlah kasus |
|---|---:|
| Aturan bisnis, akses, autentikasi, JDBC, revisi, rollback, konkurensi | 22 |
| Batas EXP / level / tier | 12 |
| Ekstraksi PDF/DOCX, pembacaan kode statis, validasi rekomendasi | 4 |
| PDF viewer: render, halaman, karakter path, berkas hilang/rusak, batas ukuran halaman | 5 |
| Integrasi antarmuka JavaFX (dua peran, halaman dan dialog) | 1 |
| **Total** | **44** |

Hasil aktual per eksekusi tersedia di `target/surefire-reports/`. Pengujian antarmuka bersifat opt-in karena memerlukan desktop. Screenshot di `docs/screenshots/` berasal dari snapshot scene JavaFX selama tes, bukan gambar mockup. Pengujian menggunakan H2 dengan skema yang sama, terpisah dari akun pengguna pada database persisten.

Build menggunakan target bytecode Java 21. Runtime Java 21 LTS Microsoft tersedia secara lokal di `.tools/jdk21/` dan dipilih skrip tanpa mengubah instalasi Java global.

## Batas verifikasi dan pengembangan

- MySQL disediakan sebagai konfigurasi dan driver; pengujian terhadap server MySQL nyata belum dilakukan pada lingkungan ini.
- Tidak ada kredensial layanan AI atau executable Tesseract yang dikonfigurasi. Respons layanan nyata dan kualitas OCR/penilaian belum diverifikasi. Jalur kegagalan AI serta penilaian manual telah diuji.
- Masalah jendela putih pada startup telah diperbaiki dengan memasang scene ke stage. Pengujian GUI kini memanggil startup App yang sebenarnya dan memeriksa scene serta visibilitas jendela. Halaman login paket Windows telah diverifikasi melalui tangkapan jendela native, selain snapshot internal.
- Pembuatan akun dosen masih terbuka untuk demonstrasi kampus; belum tersedia administrator institusi dan pengaturan peran tertutup.
- Pembaruan antarklien memakai polling, belum WebSocket. H2 lokal digunakan satu proses. Beberapa komputer memerlukan MySQL dan direktori lampiran bersama.
- Materi serta quest yang telah terbit tidak dapat diubah. Revisi mengikuti tenggat quest yang sama. Perpanjangan khusus mahasiswa belum tersedia.
- Nilai akhir dimasukkan sebagai satu skor 0–100 dengan rubrik sebagai pedoman; aplikasi belum memiliki formulir skor otomatis per kriteria.
- Integrasi AI mengirim maksimal 60.000 karakter per permintaan, belum memecah dokumen panjang otomatis atau melakukan retrieval.
- Laporan DOCX dan PDF proposal asli tidak ditimpa. Dokumentasi implementasi disediakan sebagai Markdown untuk bahan laporan akhir tim.
- Paket app-image Windows belum berupa installer MSI bertanda tangan. Source dan JAR tetap dapat dibangun ulang melalui Maven.

Rincian kebutuhan operasional dan langkah menjalankan ada di README. Daftar ini membedakan fitur yang telah dibuat dari layanan eksternal atau proses deployment yang masih memerlukan konfigurasi tim.
