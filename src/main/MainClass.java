package main;

import seed.Seed;
import user.User;
import config.dbConnect;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainClass {

    public static void main(String[] args) {

        dbConnect.createTables();

        Scanner sc = new Scanner(System.in);
        User userSystem = new User();
        Seed sm = new Seed();

        System.out.println("=== Welcome to Seeds Inventory System ===");

    
        dbConnect db = new dbConnect();
        String checkUsers = "SELECT * FROM users";
        List<Map<String, Object>> existingUsers = db.fetchRecords(checkUsers);

        if (existingUsers.isEmpty()) {
            System.out.println("No users found. Creating default admin!");
            String sql = "INSERT INTO users(username, email, password, role) VALUES(?,?,?,?)";
            db.addRecord(sql, "Administrator", "admin@example.com", "admin123", "Admin");
            System.out.println("Default admin created:");
            System.out.println("   Email: admin@example.com");
            System.out.println("   Password: admin123");
        }

        Map<String, Object> user = userSystem.login(sc);

        boolean isAdmin = userSystem.isAdmin(user);
        int choice;
        do {
            System.out.println("\n========= MENU =========");
            if (isAdmin) {
                System.out.println("1. Add User");
                System.out.println("2. Add Seed");
                System.out.println("3. View Seeds");
                System.out.println("4. Update Quantity");
                System.out.println("5. Delete Seed");
                System.out.println("0. Exit");
                System.out.print("Choice: ");
                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        userSystem.addUser(sc);
                        break;
                    case 2:
                        sm.addSeed(sc, user);
                        break;
                    case 3:
                        sm.viewSeeds();
                        break;
                    case 4:
                        sm.updateQty(sc, user);
                        break;
                    case 5:
                        sm.deleteSeed(sc, user);
                        break;
                    case 0:
                        System.out.println("❎ Exiting... Thank you!");
                        break;
                    default:
                        System.out.println("❌ Invalid choice!");
                }

            } else {
                System.out.println("1. Add Seed");
                System.out.println("2. View Seeds");
                System.out.println("3. Update Quantity");
                System.out.println("4. Delete Seed");
                System.out.println("0. Exit");
                System.out.print("Choice: ");
                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        sm.addSeed(sc, user);
                        break;
                    case 2:
                        sm.viewSeeds();
                        break;
                    case 3:
                        sm.updateQty(sc, user);
                        break;
                    case 4:
                        sm.deleteSeed(sc, user);
                        break;
                    case 0:
                        System.out.println("Exiting... Thank you!");
                        break;
                    default:
                        System.out.println("Invalid choice!");
                }
            }

        } while (choice != 0);

        sc.close();
    }
}
