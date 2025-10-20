package com.toch.proj;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginApp extends Application {

    // DB connection - use your settings
    private Connection connectDB() throws Exception {
        String url = "jdbc:mysql://localhost:3306/myprojectdb";
        String user = "root";
        String password = "Mysql25";

        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public void start(Stage primaryStage) {
        Label appTitle = new Label("Quickdine");
        appTitle.getStyleClass().add("app-title");

        Label tagline = new Label("Tap. Scan. Savor");
        tagline.getStyleClass().add("tagline");

        Label userLabel = new Label("User ID");
        userLabel.getStyleClass().add("field-label");
        TextField userField = new TextField();
        userField.setPromptText("e.g. STD102");
        userField.getStyleClass().add("input-field");

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("field-label");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Your password");
        passField.getStyleClass().add("input-field");

        Label messageLabel = new Label();
        messageLabel.getStyleClass().add("message-label");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-btn");
        loginButton.setDefaultButton(true);

        VBox form = new VBox(8,
                userLabel, userField,
                passLabel, passField,
                loginButton,
                messageLabel);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(10, 10, 6, 10));

        VBox card = new VBox(12, appTitle, tagline, form);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.getStyleClass().add("login-card");
        card.setMaxWidth(460);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("root-bg");

        Scene scene = new Scene(root, 620, 560);
        scene.getStylesheets().add(getClass().getResource("/com/toch/proj/login.css").toExternalForm());

        FadeTransition ft = new FadeTransition(Duration.millis(700), card);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        loginButton.setOnAction(e -> {
            String userid = userField.getText().trim();
            String password = passField.getText();

            if (userid.isEmpty() || password.isEmpty()) {
                messageLabel.setText("✖ Please enter both User ID and Password");
                messageLabel.getStyleClass().removeAll("message-success");
                messageLabel.getStyleClass().add("message-error");
                return;
            }

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

                    messageLabel.setText("✔ Welcome " + name);
                    messageLabel.getStyleClass().removeAll("message-error");
                    messageLabel.getStyleClass().add("message-success");

                    if ("admin".equalsIgnoreCase(role)) {
                        Stage adminStage = new Stage();
                        new AdminDashboard().start(adminStage);
                    } else {
                        Stage userStage = new Stage();
                        new StudentDashboard(userId).start(userStage);
                    }

                    ((Stage) loginButton.getScene().getWindow()).close();
                } else {
                    messageLabel.setText("✖ Invalid User ID or Password");
                    messageLabel.getStyleClass().removeAll("message-success");
                    messageLabel.getStyleClass().add("message-error");
                }
            } catch (Exception ex) {
                messageLabel.setText("⚠ Database error");
                messageLabel.getStyleClass().removeAll("message-success");
                messageLabel.getStyleClass().add("message-error");
                ex.printStackTrace();
            }
        });

        primaryStage.setTitle("QuickDine Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        userField.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
