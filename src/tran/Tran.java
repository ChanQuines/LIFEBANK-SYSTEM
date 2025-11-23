package tran;

import config.dbConnect;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Tran {
    private dbConnect db;

    public Tran(dbConnect dbConnection) {
        this.db = dbConnection;
    }

    public void recordTransaction(String itemName, double quantityAffected, String actionType, String userExecuted) {
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);

        String sql = "INSERT INTO Transactions (item_name, quantity_affected, action_type, timestamp, user_executed) " +
                      "VALUES (?, ?, ?, ?, ?)";
        
        db.addRecord(
            sql, 
            itemName, 
            quantityAffected, 
            actionType, 
            timestamp, 
            userExecuted
        );

        System.out.println("Logged Transaction: " + actionType + " | Item: " + itemName + " | Qty: " + quantityAffected);
    }

    public void viewTransactions() {
        System.out.println("\n========================================= FULL TRANSACTION LOG (ADMIN VIEW) =========================================");

        String sqlQuery = "SELECT transaction_id, item_name, quantity_affected, action_type, timestamp, user_executed " +
                          "FROM Transactions ORDER BY timestamp DESC;";
                          
        List<Map<String, Object>> records = this.db.fetchRecords(sqlQuery);
        
        if (records.isEmpty()) {
            System.out.println("No transaction records found in the log.");
            System.out.println("=================================================================================================");
            return;
        }

        String format = "%-5s | %-30s | %-12s | %-25s | %-25s | %-15s%n";
        
        System.out.printf(format, "ID", "Item Name", "Quantity", "Action", "Timestamp", "User");
        System.out.println("-------------------------------------------------------------------------------------------------------------------");

        for (Map<String, Object> record : records) {
            Object qtyObject = record.get("quantity_affected");
            String qtyDisplay = (qtyObject instanceof Number) ? String.format("%.2f", ((Number)qtyObject).doubleValue()) : "N/A";

            System.out.printf(format,
                record.get("transaction_id"),
                record.get("item_name"),
                qtyDisplay,
                record.get("action_type"),
                record.get("timestamp"),
                record.get("user_executed")
            );
        }
        
        System.out.println("===================================================================================================================");
    }
}