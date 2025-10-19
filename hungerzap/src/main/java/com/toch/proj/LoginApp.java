package com.toch.proj;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;

public class LoginApp extends Application {

    private Connection connectDB() throws SQLException, ClassNotFoundException {
        String url = "jdbc:mysql://localhost:3306/myprojectdb";
        String user = "root";
        String password = "Mysql25";

        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public void start(Stage primaryStage) {
        // --- App title ---
        Label appTitle = new Label("SwiftServe");
        appTitle.getStyleClass().add("app-title");

        Label tagline = new Label("Tap. Scan. Savor");
        tagline.getStyleClass().add("tagline");

        // --- Login form ---
        Label userLabel = new Label("User ID:");
        TextField userField = new TextField();
        userField.setPromptText("Enter User ID");

        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter Password");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("login-btn");

        Label messageLabel = new Label();

        // --- Button Actions ---
        loginButton.setOnAction(e -> {
            String userid = userField.getText();
            String password = passField.getText();

            try (Connection conn = connectDB()) {
                String query = "SELECT id, username, role FROM users WHERE userid=? AND password=?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, userid);
                stmt.setString(2, password);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String name = rs.getString("username");
                    String role = rs.getString("role");
                    int userId = rs.getInt("id");

                    if ("admin".equalsIgnoreCase(role)) {
                        messageLabel.setText("✅ Welcome " + name + " (Admin)");

                        Stage adminStage = new Stage();
                        new AdminDashboard().start(adminStage);

                        ((Stage) loginButton.getScene().getWindow()).close();
                    } else {
                        messageLabel.setText("✅ Welcome " + name + " (" + role + ")");

                        Stage userStage = new Stage();
                        new StudentDashboard(userId).start(userStage);

                        ((Stage) loginButton.getScene().getWindow()).close();
                    }

                } else {
                    messageLabel.setText("❌ Invalid User ID or Password");
                }
            } catch (Exception ex) {
                messageLabel.setText("⚠ Database error!");
                ex.printStackTrace();
            }
        });

        // --- Layout ---
        VBox vbox = new VBox(15, appTitle, tagline, userLabel, userField, passLabel, passField, loginButton, messageLabel);
        vbox.setAlignment(Pos.CENTER);
        vbox.getStyleClass().add("login-container");

        // Root with gradient background
        StackPane root = new StackPane(vbox);
        root.getStyleClass().add("root-bg");

        Scene scene = new Scene(root, 420, 450);

        // Attach CSS (make sure login.css is in resources/com/toch/proj/)
        scene.getStylesheets().add(getClass().getResource("/com/toch/proj/login.css").toExternalForm());

        primaryStage.setTitle("SwiftServe Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
