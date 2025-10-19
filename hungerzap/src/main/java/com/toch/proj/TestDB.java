package com.toch.proj;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/myprojectdb"; // replace with your DB name
        String user = "root";                                   // your MySQL username
        String password = "Mysql25";                      // your MySQL password

        try {
            // Load MySQL Driver (optional in JDBC 4+, but safe to include)
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                if (conn != null) {
                    System.out.println("✅ Connected to the database successfully!");
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Connection failed!");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL Driver not found!");
            e.printStackTrace();
        }
    }
}
