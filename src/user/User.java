package user;

import config.dbConnect;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class User {

    dbConnect db = new dbConnect();

    public Map<String, Object> login(Scanner sc) {
        Map<String, Object> user = null;
        while (user == null) {
            System.out.print("\nEmail: ");
            String email = sc.nextLine();

            System.out.print("Password: ");
            String password = sc.nextLine();

            String sql = "SELECT * FROM users WHERE email=? AND password=?";
            List<Map<String, Object>> users = db.fetchRecords(sql, email, password);

            if (users.isEmpty()) {
                System.out.println("❌ Invalid email or password. Please try again.");
            } else {
                user = users.get(0);
                System.out.println("✅ Login successful. Welcome, " + user.get("username") + " (" + user.get("role") + ")!");
            }
        }
        return user;
    }

    public void addUser(Scanner sc) {
        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Email: ");
        String email = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        String role;
        while (true) {
            System.out.print("Role (Admin/Staff): ");
            role = sc.nextLine().trim();
            if (role.equalsIgnoreCase("Admin") || role.equalsIgnoreCase("Staff")) {
                break;
            }
            System.out.println("❌ Invalid role. Please enter either 'Admin' or 'Staff'.");
        }

        String sql = "INSERT INTO users(username, email, password, role) VALUES(?,?,?,?)";
        db.addRecord(sql, username, email, password, role);

        System.out.println("✅ User added successfully!");
    }

    public boolean isAdmin(Map<String, Object> user) {
        if (user == null) return false;
        String role = user.get("role").toString();
        return role.equalsIgnoreCase("Admin");
    }
}
