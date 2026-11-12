# E-LOG BARANG ULP EMPANG

Aplikasi Android APK tanpa login untuk pencatatan barang masuk/keluar dan pemantauan stok.

## Komponen
- Android APK (WebView shell) untuk dipasang langsung di HP.
- Google Apps Script sebagai antarmuka dan backend.
- Google Spreadsheet sebagai database.
- Foto transaksi disimpan ke Google Drive.

## Setup database
Spreadsheet ID sudah diarahkan ke:
`1SxSUNAhbLYzelQaJIF158cou8K8om3qKH9Hvi4_Sxtw`

1. Buka https://script.google.com/ dan buat project Apps Script.
2. Salin isi `gas/Code.gs` ke editor Apps Script.
3. Jalankan fungsi `setup` satu kali dan berikan izin yang diminta.
4. Deploy > New deployment > Web app.
5. Execute as: Me. Who has access: Anyone.
6. Salin URL deployment yang berakhiran `/exec`.
7. Buka APK, masukkan URL deployment saat diminta.

## Build APK
GitHub Actions akan membangun APK debug setiap ada push ke branch `main`. Ambil file APK dari tab Actions > workflow terbaru > Artifacts.

Catatan: akses aplikasi terbuka tanpa akun. Siapa pun yang memiliki APK dan URL web app dapat mencatat transaksi. Batasi penyebaran URL dan lakukan pencadangan spreadsheet secara berkala.
