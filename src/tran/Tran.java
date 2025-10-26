package tran;

public class Tran {

    public void recordTransaction(String seed, int qty, String action, String username) {
        System.out.println("📜 Transaction: " + action + " | Seed: " + seed + " | Qty: " + qty + " | By: " + username);
    }
}
