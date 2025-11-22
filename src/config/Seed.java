package seed;

import config.dbConnect;
import tran.Tran;
import java.util.Scanner;
import java.util.Map;
import java.util.List;
import java.time.LocalDate;

public class Seed {
    private dbConnect db;
    private Tran tranSystem;

    public Seed(dbConnect dbConnection, Tran tranSystem) {
        this.db = dbConnection;
        this.tranSystem = tranSystem;
        this.initializeTables(); 
    }
    
 
    private void initializeTables() {
        if (this.db.fetchRecords("SELECT name FROM sqlite_master WHERE type='table' AND name='Locations'").isEmpty()) {
            String createLocations = "CREATE TABLE IF NOT EXISTS Locations (" +
                                     "location_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                     "location_name TEXT UNIQUE NOT NULL," +
                                     "storage_method TEXT," +
                                     "contact_person TEXT)";
            db.addRecord(createLocations);
        }

        if (this.db.fetchRecords("SELECT name FROM sqlite_master WHERE type='table' AND name='Seeds'").isEmpty()) {
            String createSeeds = "CREATE TABLE IF NOT EXISTS Seeds (" +
                                 "seed_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                 "common_name TEXT NOT NULL," +
                                 "variety TEXT NOT NULL," +
                                 "default_unit TEXT NOT NULL," +
                                 "UNIQUE(common_name, variety))";
            db.addRecord(createSeeds);
        }
        
        if (this.db.fetchRecords("SELECT name FROM sqlite_master WHERE type='table' AND name='Inventory'").isEmpty()) {
            String createInventory = "CREATE TABLE IF NOT EXISTS Inventory (" +
                                     "inventory_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                     "seed_id INTEGER NOT NULL," +
                                     "location_id INTEGER NOT NULL," +
                                     "quantity_on_hand REAL NOT NULL," +
                                     "date_last_counted TEXT," +
                                     "FOREIGN KEY (seed_id) REFERENCES Seeds(seed_id) ON DELETE CASCADE," +
                                     "FOREIGN KEY (location_id) REFERENCES Locations(location_id) ON DELETE RESTRICT," +
                                     "UNIQUE(seed_id, location_id))";
            db.addRecord(createInventory);
        }

        if (this.db.fetchRecords("SELECT name FROM sqlite_master WHERE type='table' AND name='Transactions'").isEmpty()) {
            String createTrans = "CREATE TABLE IF NOT EXISTS Transactions (" +
                                 "transaction_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                 "item_name TEXT NOT NULL," +
                                 "quantity_affected REAL NOT NULL," +
                                 "action_type TEXT NOT NULL," +
                                 "timestamp TEXT NOT NULL," +
                                 "user_executed TEXT NOT NULL)";
            db.addRecord(createTrans);
        }
    }
    
 
    private void viewLocations() {
        System.out.println("\n--- Available Storage Locations ---");
        String sqlQuery = "SELECT location_id, location_name, storage_method FROM Locations ORDER BY location_id";
        
        List<Map<String, Object>> records = this.db.fetchRecords(sqlQuery); 
        
        if (records.isEmpty()) {
            System.out.println("No storage locations defined. Please use the 'Create Location' option first.");
            return;
        }

        String format = "%-5s | %-20s | %-20s%n";
        String[] headers = {"ID", "Location Name", "Storage Method"};
        
        System.out.printf(format, (Object[])headers);
        System.out.println("---------------------------------------------------------");

        for (Map<String, Object> record : records) {
            System.out.printf(format,
                record.get("location_id"),
                record.get("location_name"),
                record.get("storage_method")
            );
        }
        System.out.println("---------------------------------------------------------");
    }

    public void createLocation(Scanner sc) {
        System.out.println("\n--- CREATE NEW STORAGE LOCATION ---");
        
        System.out.print("Enter Location Name (e.g., Cold Room 2A): ");
        String locationName = sc.nextLine();
        
        System.out.print("Enter Storage Method (e.g., -80C Freezer, Ambient Shelf): ");
        String storageMethod = sc.nextLine();
        
        System.out.print("Enter Contact Person (optional, e.g., Lab Manager): ");
        String contactPerson = sc.nextLine();

        if (locationName.trim().isEmpty()) {
            System.out.println("❌ Location name cannot be empty.");
            return;
        }

        String insertLocationSql = "INSERT INTO Locations (location_name, storage_method, contact_person) VALUES (?, ?, ?)";
        this.db.addRecord(insertLocationSql, locationName, storageMethod, contactPerson);
        
        String fetchIdSql = "SELECT last_insert_rowid() AS location_id";
        List<Map<String, Object>> results = this.db.fetchRecords(fetchIdSql);
        
        if (!results.isEmpty()) {
             Number idNumber = (Number) results.get(0).get("location_id"); 
             System.out.println("✅ Location '" + locationName + "' successfully created with ID: " + idNumber.intValue());
             
             // Record Transaction
             String username = (String) this.db.fetchRecords("SELECT username FROM users WHERE role = 'Admin' LIMIT 1").get(0).get("username"); // Best effort to get an admin name
             this.tranSystem.recordTransaction("Location: " + locationName, 0.0, "LOCATION_CREATED", username);
        } else {
             System.out.println("⚠️ Location created, but failed to retrieve new ID.");
        }
    }

    public void addSeed(Scanner sc, Map<String, Object> user) {
        System.out.println("\n--- ADD NEW SEED AND INITIAL STOCK ---");
        
        System.out.print("Enter Common Name (e.g., Tomato): ");
        String commonName = sc.nextLine();
        
        System.out.print("Enter Variety/Line Name (e.g., Roma VF): ");
        String variety = sc.nextLine();
        
        System.out.print("Enter Initial Quantity to Store (e.g., 500.0): ");
        String qtyInput = sc.nextLine();
        double quantityOnHand;
        try {
            quantityOnHand = Double.parseDouble(qtyInput);
            if (quantityOnHand < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("❌ Error: Quantity must be a valid non-negative number. Aborting.");
            return;
        }

        System.out.print("Enter Default Unit (e.g., seeds, grams, vials): ");
        String defaultUnit = sc.nextLine();
        
        viewLocations(); 
        System.out.print("Enter Storage Location ID from the list above: ");
        String locationInput = sc.nextLine();
        int locationId;
        try {
            locationId = Integer.parseInt(locationInput);
            if (this.db.fetchRecords("SELECT location_id FROM Locations WHERE location_id = ?", locationId).isEmpty()) {
                 System.out.println("❌ Error: Location ID " + locationId + " does not exist.");
                 return;
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Error: Location ID must be an integer. Aborting.");
            return;
        }
        
        String fetchSeedIdSql = "SELECT seed_id FROM Seeds WHERE common_name = ? AND variety = ?";
        List<Map<String, Object>> existingSeed = this.db.fetchRecords(fetchSeedIdSql, commonName, variety);
        
        int seedId;
        String logItemName = commonName + " - " + variety;

        if (existingSeed.isEmpty()) {
            String insertSeedSql = "INSERT INTO Seeds (common_name, variety, default_unit) VALUES (?, ?, ?)";
            this.db.addRecord(insertSeedSql, commonName, variety, defaultUnit);
            
            String fetchIdSql = "SELECT MAX(seed_id) AS id FROM Seeds";
            List<Map<String, Object>> results = this.db.fetchRecords(fetchIdSql);
            
            if (!results.isEmpty() && results.get(0).get("id") instanceof Number) {
                seedId = ((Number) results.get(0).get("id")).intValue();
            } else {
                System.out.println("⚠️ WARNING: Failed to retrieve new Seed ID. Aborting inventory insertion.");
                return;
            }

        } else {
            seedId = ((Number) existingSeed.get(0).get("seed_id")).intValue();
        }
        
        String checkInventorySql = "SELECT quantity_on_hand FROM Inventory WHERE seed_id = ? AND location_id = ?";
        List<Map<String, Object>> existingInventory = this.db.fetchRecords(checkInventorySql, seedId, locationId);
        
        String dateLastCounted = LocalDate.now().toString(); 
        String inventoryMessage;
        String transactionAction;

        if (existingInventory.isEmpty()) {
            String insertInventorySql = "INSERT INTO Inventory (seed_id, location_id, quantity_on_hand, date_last_counted) VALUES (?, ?, ?, ?)";
            this.db.addRecord(insertInventorySql, seedId, locationId, quantityOnHand, dateLastCounted);
            inventoryMessage = "New stock record created";
            transactionAction = "ADDED (Initial Stock)";

        } else {
            double currentQty = ((Number) existingInventory.get(0).get("quantity_on_hand")).doubleValue();
            double newQty = currentQty + quantityOnHand;
            
            String updateInventorySql = "UPDATE Inventory SET quantity_on_hand = ?, date_last_counted = ? WHERE seed_id = ? AND location_id = ?";
            this.db.updateRecord(updateInventorySql, newQty, dateLastCounted, seedId, locationId);
            inventoryMessage = "Existing stock updated to " + newQty;
            transactionAction = "UPDATED (Added Stock)";
        }
        
        String username = (String) user.get("username");
        this.tranSystem.recordTransaction(logItemName, quantityOnHand, transactionAction, username);
        
        System.out.println("🎉 Successfully processed seed (ID: " + seedId + "). " + inventoryMessage);
    }

    public void viewSeeds() {
        System.out.println("\n========= 🔍 CURRENT RESEARCH SEED INVENTORY =========");

        String sqlQuery = "SELECT " +
                              "S.seed_id, S.common_name, S.variety, S.default_unit, " +
                              "I.quantity_on_hand, L.location_name " +
                              "FROM Seeds AS S " +
                              "INNER JOIN Inventory AS I ON S.seed_id = I.seed_id " + 
                              "INNER JOIN Locations AS L ON I.location_id = L.location_id " +
                              "ORDER BY S.common_name, S.variety;";
                      
        List<Map<String, Object>> records = this.db.fetchRecords(sqlQuery); 
        
        if (records.isEmpty()) {
            System.out.println("No seed inventory records found.");
            System.out.println("=========================================================");
            return;
        }

        String format = "%-5s | %-20s | %-15s | %-8s | %-10s | %-20s%n";
        
        System.out.printf(format, "ID", "Common Name", "Variety/Line", "Unit", "QTY", "Location");
        System.out.println("------------------------------------------------------------------------------------------");

        for (Map<String, Object> record : records) {
             Object qtyObject = record.get("quantity_on_hand");
             String qtyDisplay = (qtyObject instanceof Number) ? String.format("%.2f", ((Number)qtyObject).doubleValue()) : "N/A";
            System.out.printf(format,
                record.get("seed_id"),
                record.get("common_name"),
                record.get("variety"),
                record.get("default_unit"),
                qtyDisplay,
                record.get("location_name")
            );
        }
        
        System.out.println("=========================================================");
    }
    
    public void updateQty(Scanner sc, Map<String, Object> user) {
        viewSeeds();
        
        System.out.println("\n--- UPDATE SEED QUANTITY ---");
        
        System.out.print("Enter Seed ID to update: ");
        String idInput = sc.nextLine();
        int seedId;
        try {
            seedId = Integer.parseInt(idInput);
        } catch (NumberFormatException e) {
            System.out.println("❌ Error: Seed ID must be a number. Aborting.");
            return;
        }

        String fetchSql = "SELECT S.common_name, S.variety, I.quantity_on_hand " +
                          "FROM Seeds AS S INNER JOIN Inventory AS I ON S.seed_id = I.seed_id " +
                          "WHERE S.seed_id = ?";
        
        List<Map<String, Object>> results = this.db.fetchRecords(fetchSql, seedId);

        if (results.isEmpty()) {
            System.out.println("❌ Seed ID " + seedId + " not found in inventory.");
            return;
        }

        Map<String, Object> record = results.get(0);
        String itemName = record.get("common_name") + " - " + record.get("variety");
        double currentQty = ((Number) record.get("quantity_on_hand")).doubleValue();

        System.out.println("\nFound: " + itemName + " (Current Qty: " + currentQty + ")");
        System.out.print("Enter new quantity adjustment (e.g., 500.5 to add, -50.2 to remove): ");
        
        String adjInput = sc.nextLine();
        double adjustment;
        try {
            adjustment = Double.parseDouble(adjInput);
        } catch (NumberFormatException e) {
            System.out.println("❌ Error: Adjustment must be a valid number. Aborting.");
            return;
        }
        
        double newQty = currentQty + adjustment;

        if (newQty < 0) {
            System.out.println("❌ Error: Cannot set quantity below zero (" + newQty + "). Update aborted.");
            return;
        }

        String dateLastCounted = LocalDate.now().toString(); 
        String updateSql = "UPDATE Inventory SET quantity_on_hand = ?, date_last_counted = ? WHERE seed_id = ?";
        this.db.updateRecord(updateSql, newQty, dateLastCounted, seedId);
        
        // --- STEP 3: Record Transaction ---
        String username = (String) user.get("username");
        String actionType = adjustment > 0 ? "STOCK_IN" : "STOCK_OUT";
        this.tranSystem.recordTransaction(itemName, adjustment, actionType, username);
        
        System.out.println("🎉 Successfully updated " + itemName + ". New Quantity: " + String.format("%.2f", newQty));
    }
    
    public void deleteSeed(Scanner sc, Map<String, Object> user) {
        viewSeeds();

        System.out.println("\n--- DELETE SEED AND INVENTORY ---");

        System.out.print("Enter Seed ID to delete: ");
        String idInput = sc.nextLine();
        int seedId;
        try {
            seedId = Integer.parseInt(idInput);
        } catch (NumberFormatException e) {
            System.out.println("❌ Error: Seed ID must be a number. Aborting.");
            return;
        }

        String fetchSql = "SELECT S.common_name, S.variety, I.quantity_on_hand " +
                          "FROM Seeds AS S INNER JOIN Inventory AS I ON S.seed_id = I.seed_id " +
                          "WHERE S.seed_id = ?";
        
        List<Map<String, Object>> results = this.db.fetchRecords(fetchSql, seedId);

        if (results.isEmpty()) {
            System.out.println("❌ Seed ID " + seedId + " not found in the database.");
            return;
        }
        
        Map<String, Object> record = results.get(0);
        String itemName = record.get("common_name") + " - " + record.get("variety");
        double deletedQty = ((Number) record.get("quantity_on_hand")).doubleValue();
  
        System.out.print("⚠️ Are you sure you want to delete '" + itemName + "' (Qty: " + String.format("%.2f", deletedQty) + ")? (yes/no): ");
        String confirmation = sc.nextLine();

        if (!confirmation.trim().equalsIgnoreCase("yes")) {
            System.out.println("🚫 Deletion cancelled.");
            return;
        }

        String deleteSql = "DELETE FROM Seeds WHERE seed_id = ?";
        this.db.deleteRecord(deleteSql, seedId);
        
        String username = (String) user.get("username");
        this.tranSystem.recordTransaction(itemName, -deletedQty, "DELETED", username); 
        
        System.out.println("🗑️ Successfully deleted seed: " + itemName + " (ID: " + seedId + "). Inventory cleared.");
    }
}