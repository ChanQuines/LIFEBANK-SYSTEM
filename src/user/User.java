package user;

import config.dbConnect;
import java.util.Scanner;
import java.util.Map;
import java.util.List;

public class User {
    private dbConnect db;

    public User(dbConnect dbConnection) {
        this.db = dbConnection;
        this.initializeUsersTable();
    }
public static void displayAdminMenu() {
    System.out.println("\n===== ADMIN MENU =====");
    System.out.println("1. Add New User");
    System.out.println("2. Add New Seed Stock");
    System.out.println("3. View Current Inventory");
    System.out.println("4. Update Seed Quantity");
    System.out.println("5. Delete Seed");
    System.out.println("6. Create New Location");
    System.out.println("7. View Transaction Log");
    System.out.println("0. Logout");
    System.out.println("======================");
}

public static void displayUserMenu() {
    System.out.println("\n===== USER MENU =====");
    System.out.println("1. Add New Seed Stock");
    System.out.println("2. View Current Inventory");
    System.out.println("3. Update Seed Quantity");
    System.out.println("4. Delete Seed");
    System.out.println("0. Logout");
    System.out.println("=====================");
}
    private void initializeUsersTable() {
        String createUserTable = "CREATE TABLE IF NOT EXISTS users (" +
                                 "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                 "username TEXT NOT NULL," +
                                 "email TEXT UNIQUE NOT NULL," +
                                 "password TEXT NOT NULL," +
                                 "role TEXT NOT NULL DEFAULT 'User')";
        
        this.db.addRecord(createUserTable);
    }
    
    public boolean isAdmin(Map<String, Object> user) {
        if (user == null || user.get("role") == null) {
            return false;
        }
        return "Admin".equalsIgnoreCase((String) user.get("role"));
    }

    public Map<String, Object> login(Scanner sc) {
        System.out.println("\n========= LOGIN =========");

        while (true) {
            System.out.print("Enter Email: ");
            String email = sc.nextLine();

            System.out.print("Enter Password: ");
            String plainPassword = sc.nextLine(); 

            String sql = "SELECT user_id, username, email, password, role FROM users WHERE email = ?";
            List<Map<String, Object>> records = this.db.fetchRecords(sql, email);

            if (!records.isEmpty()) {
                Map<String, Object> user = records.get(0);
                String storedPassword = (String) user.get("password");

                if (plainPassword.equals(storedPassword)) {
                    System.out.println("✅ Login Successful! Welcome, " + user.get("username") + " (" + user.get("role") + ")");
                    return user;
                }
            }

            System.out.println("❌ Invalid email or password. Try again.");
        }
    }

    public void addUser(Scanner sc) {
        System.out.println("\n--- ADD NEW USER ---");

        System.out.print("Enter New Username: ");
        String username = sc.nextLine();

        System.out.print("Enter New Email (must be unique): ");
        String email = sc.nextLine();
        
        String plainPassword;
        do {
            System.out.print("Enter New Password: ");
            plainPassword = sc.nextLine();
            if (plainPassword.trim().isEmpty()) {
                System.out.println("Password cannot be empty.");
            }
        } while (plainPassword.trim().isEmpty());


        System.out.print("Enter Role (Admin/User) [Default: User]: ");
        String role = sc.nextLine();
        if (role.trim().isEmpty()) role = "User";

        String sql = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";

        try {
            this.db.addRecord(sql, username, email, plainPassword, role);
            System.out.println("✅ User '" + username + "' added successfully! (Stored as plain text)");
        } catch (Exception e) {
            System.out.println("❌ Failed to add user. Email may already exist or DB error occurred.");
        }
    }
    
    private String readPasswordMasked() {
        return new Scanner(System.in).nextLine();
    }
}