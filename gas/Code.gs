const SPREADSHEET_ID = '1SxSUNAhbLYzelQaJIF158cou8K8om3qKH9Hvi4_Sxtw';
const TRANSAKSI_SHEET = 'Transaksi';
const BARANG_SHEET = 'Barang';
const DRIVE_FOLDER = 'E-LOG BARANG ULP EMPANG';
const PETUGAS_SECURITY = ['LENY MARLINA','HERYANDI','ASEP SUANDI','FENDY PRADANA'];

function doGet() {
  setup();
  return HtmlService.createHtmlOutputFromFile('Index')
    .setTitle('E-LOG BARANG ULP EMPANG')
    .addMetaTag('viewport', 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no');
}

function setup() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  let tx = ss.getSheetByName(TRANSAKSI_SHEET);
  if (!tx) tx = ss.insertSheet(TRANSAKSI_SHEET);
  const headers = ['ID','Timestamp','Jenis','Nama Barang','Nama Pengambil Barang','Jumlah','Satuan','Petugas Security','Keterangan','Foto URL'];
  if (tx.getLastRow() === 0) tx.appendRow(headers);
  else tx.getRange(1,1,1,headers.length).setValues([headers]);
  let br = ss.getSheetByName(BARANG_SHEET);
  if (!br) br = ss.insertSheet(BARANG_SHEET);
  if (br.getLastRow() === 0) br.appendRow(['Kode Barang','Nama Barang','Stok','Satuan','Terakhir Diperbarui']);
  getPhotoFolder_();
  return 'OK';
}

function getDashboard() {
  setup();
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const rows = ss.getSheetByName(BARANG_SHEET).getDataRange().getValues();
  const items = rows.slice(1).filter(r => r[0] || r[1]).map(r => ({
    kode: String(r[0]), nama: String(r[1]), stok: Number(r[2]) || 0,
    satuan: String(r[3] || '')
  }));
  const tx = ss.getSheetByName(TRANSAKSI_SHEET).getDataRange().getValues();
  const history = tx.slice(1).filter(r => r[0]).reverse().slice(0,100).map(r => ({
    id:String(r[0]), waktu:r[1] instanceof Date ? Utilities.formatDate(r[1], Session.getScriptTimeZone(), 'dd/MM/yyyy HH:mm:ss') : String(r[1]),
    jenis:String(r[2]), nama:String(r[3]), pengambil:String(r[4]), jumlah:Number(r[5])||0,
    satuan:String(r[6]), petugas:String(r[7]), keterangan:String(r[8]), foto:String(r[9]||'')
  }));
  return {items, history, totalJenis:items.length, totalTransaksi:Math.max(0,tx.length-1), petugas:PETUGAS_SECURITY};
}

function saveTransaction(data) {
  setup();
  if (!data || data.jenis !== 'KELUAR') throw new Error('Aplikasi hanya menerima transaksi barang keluar.');
  const nama = String(data.nama || '').trim();
  const pengambil = String(data.pengambil || '').trim();
  const jumlah = Number(data.jumlah);
  const satuan = String(data.satuan || '').trim();
  const petugas = String(data.petugas || '').trim();
  if (!nama || !pengambil || !Number.isFinite(jumlah) || jumlah <= 0 || !satuan) throw new Error('Nama barang, nama pengambil, jumlah, dan satuan wajib diisi.');
  if (!PETUGAS_SECURITY.includes(petugas)) throw new Error('Pilih nama petugas Security dari daftar.');
  const lock = LockService.getScriptLock();
  lock.waitLock(20000);
  try {
    const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    const sh = ss.getSheetByName(BARANG_SHEET);
    const values = sh.getDataRange().getValues();
    const idx = values.findIndex((r,i) => i > 0 && String(r[1]).trim().toLowerCase() === nama.toLowerCase());
    if (idx < 0) throw new Error('Barang belum terdaftar pada daftar stok. Tambahkan data stok barang terlebih dahulu.');
    const kode = String(values[idx][0]);
    const stok = Number(values[idx][2]) || 0;
    if (jumlah > stok) throw new Error('Stok tidak cukup. Stok tersedia: ' + stok + ' ' + String(values[idx][3] || satuan));
    const next = stok - jumlah;
    sh.getRange(idx+1,3).setValue(next);
    sh.getRange(idx+1,5).setValue(new Date());
    let photoUrl = '';
    if (data.foto && data.foto.data) {
      const bytes = Utilities.base64Decode(data.foto.data);
      const blob = Utilities.newBlob(bytes, data.foto.mimeType || 'image/jpeg', data.foto.name || ('foto-' + Date.now() + '.jpg'));
      photoUrl = getPhotoFolder_().createFile(blob).getUrl();
    }
    const id = Utilities.getUuid();
    ss.getSheetByName(TRANSAKSI_SHEET).appendRow([id,new Date(),'KELUAR',nama,pengambil,jumlah,satuan,petugas,String(data.keterangan||''),photoUrl]);
    return {ok:true,id,stok:next};
  } finally { lock.releaseLock(); }
}

function getPhotoFolder_() {
  const folders = DriveApp.getFoldersByName(DRIVE_FOLDER);
  return folders.hasNext() ? folders.next() : DriveApp.createFolder(DRIVE_FOLDER);
}
