package main;

import config.dbConnect;
import user.User; // Ensure this import is here
import seed.Seed;
import tran.Tran;
import java.util.Scanner;
import java.util.Map;

public class MainClass {
    
    // --- REMOVED: public static void displayAdminMenu() and displayUserMenu() ---

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        // 1. Initialize Core Dependencies
        dbConnect db = new dbConnect();
        User userSystem = new User(db);
        Tran tranSystem = new Tran(db);
        Seed sm = new Seed(db, tranSystem); 
        
        Map<String, Object> currentUser = null;

        System.out.println("🌱 Welcome to the Seed Inventory Manager!");

        if (db.fetchRecords("SELECT user_id FROM users LIMIT 1").isEmpty()) {
            System.out.println("\n*** INITIAL SETUP: No users found. Please create the first ADMIN account. ***");
            userSystem.addUser(sc);
            // After creation, proceed to login
        }

        while (currentUser == null) {
            currentUser = userSystem.login(sc);
        }

        boolean isAdmin = userSystem.isAdmin(currentUser);
        boolean running = true;

        while (running) {
            
            if (isAdmin) {
                // CHANGE 1: Call the method from the User class
                User.displayAdminMenu(); 
                System.out.print("Admin Choice: ");
                
                if (!sc.hasNextInt()) {
                    System.out.println("❌ Invalid input. Please enter a number.");
                    sc.nextLine();
                    continue;
                }
                int choice = sc.nextInt();
                sc.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        userSystem.addUser(sc);
                        break;
                    case 2:
                        sm.addSeed(sc, currentUser);
                        break;
                    case 3:
                        sm.viewSeeds();
                        break;
                    case 4:
                        sm.updateQty(sc, currentUser);
                        break;
                    case 5:
                        sm.deleteSeed(sc, currentUser);
                        break;
                    case 6:
                        sm.createLocation(sc);
                        break;
                    case 7:
                        tranSystem.viewTransactions();
                        break;
                    case 0:
                        System.out.println("👋 Admin " + currentUser.get("username") + " logged out.");
                        running = false;
                        break;
                    default:
                        System.out.println("❓ Invalid Admin option.");
                }
                
            } else { 
                User.displayUserMenu(); 
                System.out.print("User Choice: ");

                if (!sc.hasNextInt()) {
                    System.out.println("Invalid input. Please enter a number.");
                    sc.nextLine();
                    continue;
                }
                int choice = sc.nextInt();
                sc.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        sm.addSeed(sc, currentUser);
                        break;
                    case 2:
                        sm.viewSeeds();
                        break;
                    case 3:
                        sm.updateQty(sc, currentUser);
                        break;
                    case 4:
                        sm.deleteSeed(sc, currentUser);
                        break;
                    case 0:
                        System.out.println("👋 User " + currentUser.get("username") + " logged out.");
                        running = false;
                        break;
                    default:
                        System.out.println("❓ Invalid User option.");
                }
            }
        }
        
        sc.close();
        System.out.println("\nApplication shutdown complete.");
    }
}