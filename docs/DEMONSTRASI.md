# Demonstrasi dan pengujian manual

## Presentasi sekitar 10 menit

1. **Masalah dan solusi (1 menit):** tugas, modul, dan umpan balik perlu terstruktur. RetroQuest mengaitkan progres RPG dengan kegiatan yang sudah diverifikasi dosen.
2. **Masuk sebagai Demo mahasiswa (1 menit):** tampilkan dashboard, quest, kartu NPC, profil 0 EXP, dan leaderboard. Akun Rifqi demo memiliki 150 EXP dari contoh persetujuan.
3. **Tunjukkan tugas menunggu tinjauan (1 menit):** buka Riwayat Tugas. Contoh pertama sudah disediakan, sehingga presentasi tidak bergantung pada file picker.
4. **Keluar dan pilih Demo dosen (2 menit):** buka Verification Center, baca instruksi dan rubrik, pratinjau berkas, isi nilai dan catatan, lalu ACC. AI tidak wajib.
5. **Masuk kembali sebagai mahasiswa (1 menit):** EXP menjadi 150, level 2, badge FIRST_QUEST terbuka, dan leaderboard berubah. Persetujuan tidak dapat menambah reward dua kali.
6. **Skenario revisi (1 menit):** kumpulkan tugas pada quest polimorfisme dengan contoh berkas yang relevan. Sebagai dosen, minta revisi. Sebagai mahasiswa, unggah lagi dan tunjukkan versi 1 serta versi 2.
7. **Workshop dosen (2 menit):** buat materi manual, buat quest DRAFT, tinjau bobot rubrik 100%, lalu terbitkan. Tunjukkan draf tidak terlihat oleh mahasiswa.
8. **OOP dan validasi (1 menit):** tunjukkan User, DocumentExtractor, AvatarFactory, serta pengujian approval bersamaan dan rollback.

Data demo bersifat persisten. Jika langkah ACC sudah dijalankan, tampilkan hasilnya atau gunakan akun/kelas baru untuk mengulang alur dari nol.

## Matriks pemeriksaan manual

| Skenario | Hasil yang harus terlihat |
|---|---|
| Daftar dengan email tidak valid / password pendek | Pesan validasi, akun tidak dibuat |
| Login salah | Pesan email atau password salah |
| Kode guild salah | Gabung gagal tanpa keanggotaan baru |
| Membuka draf sebagai mahasiswa | Draf tidak muncul |
| Materi PDF/DOCX biasa | Teks dapat dipratinjau dan kartu dapat ditulis |
| PDF pindai tanpa Tesseract | Dapat lanjut menulis materi secara manual |
| Bobot rubrik 90% | Penyimpanan ditolak |
| Terbitkan quest bertenggat lewat | Ditolak |
| Unggah format salah / kosong / lebih dari 20 MB | Ditolak, tidak tercipta submission |
| Kirim ulang saat tugas masih pending | Ditolak |
| Minta revisi tanpa catatan | Ditolak |
| Unggah setelah permintaan revisi | Versi baru muncul, versi lama tetap tersimpan |
| AI tidak dikonfigurasi | Kegagalan tercatat dan penilaian manual tersedia |
| Nilai akhir 101 | Ditolak |
| Setujui tugas | EXP bertambah sekali; skor dan catatan dosen tersimpan |
| EXP 300 / 700 / 1000 | Tier menjadi 2 / 3 / 4 sesuai pengujian otomatis |
| Bergabung ke kelas kedua | Progres dimulai dari 0 untuk kelas baru |
| Tutup lalu buka aplikasi | Akun, kelas, berkas, tugas, dan progres tetap ada |

## Menjalankan layanan eksternal

Siapkan konfigurasi MySQL, endpoint AI, dan Tesseract sebelum presentasi jika ingin mendemonstrasikan ketiganya. Uji dengan dokumen contoh non-sensitif. Pastikan bahasa OCR yang dipilih sudah terpasang dan endpoint AI menerima `response_format: json_object`. Jika layanan gagal, jelaskan jalur manual yang tersedia; jangan menyebut rekomendasi AI sebagai nilai final.

## Menyiapkan laporan akhir

Gunakan `RANCANGAN.md` untuk bab analisis/perancangan, source untuk bab implementasi OOP, dan hasil `target/surefire-reports` untuk bab pengujian. Tambahkan screenshot dari aplikasi yang berjalan, bukti pengujian MySQL/AI/OCR pada lingkungan tim, serta penjelasan kontribusi nyata setiap anggota. Dokumen laporan asli dipertahankan agar tim dapat menyusun isi final sesuai format kampus.
