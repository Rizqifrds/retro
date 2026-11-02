// Berkas contoh untuk demonstrasi pengumpulan; aplikasi tidak mengeksekusi kode ini.
public class BankAccount {
    private long balance;
    public BankAccount(long initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("Saldo negatif");
        balance = initialBalance;
    }
    public long getBalance() { return balance; }
    public void deposit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Jumlah harus positif");
        balance = Math.addExact(balance, amount);
    }
    public void withdraw(long amount) {
        if (amount <= 0 || amount > balance) throw new IllegalArgumentException("Penarikan tidak valid");
        balance -= amount;
    }
}
