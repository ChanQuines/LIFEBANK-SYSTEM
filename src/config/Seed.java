package seed;

import config.dbConnect;
import tran.Tran;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Seed {

    dbConnect db = new dbConnect();
    Tran tm = new Tran();

    public void addSeed(Scanner sc, Map<String, Object> user) {
        System.out.print("Seed Name: ");
        String seed = sc.nextLine();

        System.out.print("Quantity: ");
        int qty = sc.nextInt();
        sc.nextLine();

        System.out.print("Date (YYYY-MM-DD): ");
        String date = sc.nextLine();

        String sql = "INSERT INTO seed_stock(seed_name, quantity, date) VALUES(?,?,?)";
        db.addRecord(sql, seed, qty, date);

        tm.recordTransaction(seed, qty, "ADD", user.get("username").toString());
        System.out.println("✅ Seed added!");
    }

    public void viewSeeds() {
        String sql = "SELECT * FROM seed_stock";
        List<Map<String, Object>> seeds = db.fetchRecords(sql);

        System.out.println("\n=== SEEDS INVENTORY ===");
        if (seeds.isEmpty()) {
            System.out.println("No seeds available.");
            return;
        }

        for (Map<String, Object> s : seeds) {
            System.out.println("ID: " + s.get("id") +
                    " | Name: " + s.get("seed_name") +
                    " | Qty: " + s.get("quantity") +
                    " | Date: " + s.get("date"));
        }
    }

    public void updateQty(Scanner sc, Map<String, Object> user) {
        System.out.print("Seed ID: ");
        int id = sc.nextInt();

        System.out.print("New Quantity: ");
        int qty = sc.nextInt();
        sc.nextLine();

        String sql = "UPDATE seed_stock SET quantity=? WHERE id=?";
        db.addRecord(sql, qty, id);

        String seedName = getSeedNameById(id);
        tm.recordTransaction(seedName, qty, "UPDATE", user.get("username").toString());

        System.out.println("Quantity updated!");
    }

    public void deleteSeed(Scanner sc, Map<String, Object> user) {
        System.out.print("Seed ID to delete: ");
        int id = sc.nextInt();
        sc.nextLine();

        String seedName = getSeedNameById(id);

        String sql = "DELETE FROM seed_stock WHERE id=?";
        db.addRecord(sql, id);

        tm.recordTransaction(seedName, 0, "DELETE", user.get("username").toString());

        System.out.println("✅ Seed deleted!");
    }

    private String getSeedNameById(int id) {
        String sql = "SELECT seed_name FROM seed_stock WHERE id=?";
        List<Map<String, Object>> result = db.fetchRecords(sql, id);
        if (!result.isEmpty()) {
            return result.get(0).get("seed_name").toString();
        }
        return "Unknown";
    }
}
