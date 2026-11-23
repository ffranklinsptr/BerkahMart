
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.UUID;
import javax.swing.JOptionPane;

public class VoucherManager {

    /**
     * 1. Dipanggil saat transaksi selesai untuk cek ulang tahun & terbitkan voucher (Expired 1 Tahun, 1x pakai)
     */
    public static void cekDanBuatVoucherUltah(Connection conn, String idPelanggan, LocalDate tglLahir) {
        if (tglLahir == null) return;

        LocalDate hariIni = LocalDate.now();
        // Cek apakah tanggal dan bulan lahir sama dengan hari ini
        if (MonthDay.from(tglLahir).equals(MonthDay.from(hariIni))) {
            String kodeVoucher = "HBD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            LocalDate tglExpired = hariIni.plusYears(1); // Masa berlaku 1 tahun
            double nominalDiskon = 25000.0; // Sesuaikan nominal diskon

            String sql = "INSERT INTO voucher (kode_voucher, id_pelanggan, nominal, tgl_terbit, tgl_expired, status) VALUES (?, ?, ?, ?, ?, 'AKTIF')";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, kodeVoucher);
                pst.setString(2, idPelanggan);
                pst.setDouble(3, nominalDiskon);
                pst.setDate(4, java.sql.Date.valueOf(hariIni));
                pst.setDate(5, java.sql.Date.valueOf(tglExpired));
                pst.executeUpdate();

                JOptionPane.showMessageDialog(null, 
                    "Selamat Ulang Tahun!\nPelanggan mendapat voucher 1x diskon untuk transaksi berikutnya:\n" +
                    "Kode: " + kodeVoucher + "\nBerlaku sampai: " + tglExpired);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Gagal membuat voucher: " + e.getMessage());
            }
        }
    }

    /**
     * 2. Dipanggil saat kasir menginput kode voucher di transaksi berikutnya (Hanya bisa 1x pakai)
     */
    public static double validasiDanGunakanVoucher(Connection conn, String kodeVoucher, String idPelanggan) {
        String sql = "SELECT * FROM voucher WHERE kode_voucher = ? AND id_pelanggan = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, kodeVoucher);
            pst.setString(2, idPelanggan);
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Kode voucher tidak valid atau bukan milik pelanggan ini!");
                return 0;
            }

            String status = rs.getString("status");
            LocalDate tglExpired = rs.getDate("tgl_expired").toLocalDate();
            double nominal = rs.getDouble("nominal");

            // Validasi 1x pakai
            if ("TERPAKAI".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(null, "Voucher ini sudah pernah dipakai (maksimal 1x diskon)!");
                return 0;
            }

            // Validasi masa berlaku 1 tahun
            if (LocalDate.now().isAfter(tglExpired)) {
                JOptionPane.showMessageDialog(null, "Voucher sudah kedaluwarsa pada " + tglExpired);
                return 0;
            }

            // Update status voucher menjadi TERPAKAI agar tidak bisa digunakan lagi
            String updateSql = "UPDATE voucher SET status = 'TERPAKAI' WHERE kode_voucher = ?";
            try (PreparedStatement updatePst = conn.prepareStatement(updateSql)) {
                updatePst.setString(1, kodeVoucher);
                updatePst.executeUpdate();
            }

            return nominal;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error validasi voucher: " + e.getMessage());
            return 0;
        }
    }
}