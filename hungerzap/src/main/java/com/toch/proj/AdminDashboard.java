package com.toch.proj;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.sql.*;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;


public class AdminDashboard extends Application {

    private TableView<MenuItem> menuTable;
    private ObservableList<MenuItem> menuData;
    private TableView<OrderItem> orderTable;
private ObservableList<OrderItem> orderData;
// Balance management fields
private TextField userIdBalanceField;
private Label currentBalanceLabel;
private TextField newBalanceField;



    private TextField itemNameField, itemPriceField;
    private CheckBox itemAvailableCheck;

    private TextField startTimeField, endTimeField;

    private volatile boolean scanning = false;
    private ImageView webcamView;

    private Connection connectDB() throws SQLException, ClassNotFoundException {
        String url = "jdbc:mysql://localhost:3306/myprojectdb";
        String user = "root";
        String password = "Mysql25"; // change this
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public void start(Stage primaryStage) {
        Label title = new Label("SwiftServe - Admin Dashboard");
        title.setFont(Font.font("Verdana", 28));
        title.setTextFill(Color.web("#FFADAD"));
        title.setPadding(new Insets(10));

        // ---- Menu Table ----
        menuTable = new TableView<>();
        menuData = FXCollections.observableArrayList();

        TableColumn<MenuItem, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<MenuItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<MenuItem, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);

        TableColumn<MenuItem, Boolean> availableCol = new TableColumn<>("Available");
        availableCol.setCellValueFactory(new PropertyValueFactory<>("available"));
        availableCol.setPrefWidth(100);

        menuTable.getColumns().addAll(idCol, nameCol, priceCol, availableCol);
        loadMenuData();

        // Auto-refresh orders every 5 seconds
Timeline refreshOrders = new Timeline(
    new KeyFrame(Duration.seconds(5), e -> loadOrders(orderTable, orderData))
);
refreshOrders.setCycleCount(Animation.INDEFINITE);
refreshOrders.play();


        // ---- Add Item Section ----
        itemNameField = new TextField();
        itemNameField.setPromptText("Item Name");

        itemPriceField = new TextField();
        itemPriceField.setPromptText("Price");

        itemAvailableCheck = new CheckBox("Available");

        Button addButton = styledButton("Add Item");
        addButton.setOnAction(e -> addMenuItem());
        Button removeButton = styledButton("Remove Item");
removeButton.setOnAction(e -> removeMenuItem());

        

       HBox addBox = new HBox(10, itemNameField, itemPriceField, itemAvailableCheck, addButton, removeButton);

        addBox.setAlignment(Pos.CENTER);

        VBox menuCard = new VBox(10, new Label("📋 Menu Items"), menuTable, addBox);
        menuCard.setAlignment(Pos.CENTER);
        menuCard.setPadding(new Insets(15));
        menuCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        // ---- Booking Time Section ----
        startTimeField = new TextField();
        startTimeField.setPromptText("Start (HH:MM:SS)");

        endTimeField = new TextField();
        endTimeField.setPromptText("End (HH:MM:SS)");

        Button updateBookingBtn = styledButton("Update Booking Window");
        updateBookingBtn.setOnAction(e -> updateBookingWindow());

        HBox bookingBox = new HBox(10, startTimeField, endTimeField, updateBookingBtn);
        bookingBox.setAlignment(Pos.CENTER);

        VBox bookingCard = new VBox(10, new Label("⏰ Booking Window"), bookingBox);
        bookingCard.setAlignment(Pos.CENTER);
        bookingCard.setPadding(new Insets(15));
        bookingCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        // ---- Active Orders Section ----
       orderTable = new TableView<>();
orderData = FXCollections.observableArrayList();


        TableColumn<OrderItem, Integer> orderIdCol = new TableColumn<>("Order ID");
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<OrderItem, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<OrderItem, Double> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<OrderItem, String> etaCol = new TableColumn<>("ETA");
        etaCol.setCellValueFactory(new PropertyValueFactory<>("eta"));

        TableColumn<OrderItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<OrderItem, String> qrCol = new TableColumn<>("QR Code");
        qrCol.setCellValueFactory(new PropertyValueFactory<>("qrCode"));

        orderTable.getColumns().addAll(orderIdCol, userCol, amtCol, etaCol, statusCol, qrCol);
        loadOrders(orderTable, orderData);

        Button completeBtn = styledButton("Mark as Completed");
        completeBtn.setOnAction(e -> {
            OrderItem selected = orderTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                updateOrderStatus(selected.getOrderId(), "completed");
                loadOrders(orderTable, orderData);
            }
        });

        // ---- Integrated Webcam QR Scanner ----
        Button scanQRBtn = styledButton("Scan QR Code");
        scanQRBtn.setOnAction(e -> startQRScanner(orderTable, orderData));

        HBox orderBtns = new HBox(10, completeBtn, scanQRBtn);
        orderBtns.setAlignment(Pos.CENTER);

        VBox orderCard = new VBox(10, new Label("📦 Active Orders"), orderTable, orderBtns);
        orderCard.setAlignment(Pos.CENTER);
        orderCard.setPadding(new Insets(15));
        orderCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        

                
        VBox root = new VBox(20, title, menuCard, bookingCard, orderCard);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #E4F1EE, #DEDAF4);");
        
        // ---- Balance Management Section ----
userIdBalanceField = new TextField();
userIdBalanceField.setPromptText("Enter UserID");

Button checkBalanceBtn = styledButton("Check Balance");
currentBalanceLabel = new Label("Current Balance: -");

newBalanceField = new TextField();
newBalanceField.setPromptText("Enter New Balance");

Button updateBalanceBtn = styledButton("Update Balance");

// Actions
checkBalanceBtn.setOnAction(e -> loadUserBalance());
updateBalanceBtn.setOnAction(e -> updateUserBalance());

VBox balanceCard = new VBox(10,
        new Label("💰 Balance Management"),
        userIdBalanceField,
        checkBalanceBtn,
        currentBalanceLabel,
        newBalanceField,
        updateBalanceBtn
);
balanceCard.setAlignment(Pos.CENTER);
balanceCard.setPadding(new Insets(15));
balanceCard.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);");

// Add balance card to root
root.getChildren().add(balanceCard);


        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("SwiftServe - Admin Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ---- Styled Button ----
    private Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #FFD6A5; -fx-text-fill: #333; -fx-font-weight: bold; "
                + "-fx-background-radius: 20; -fx-padding: 8 20;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #FFADAD; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #FFD6A5; -fx-text-fill: #333; "
                + "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;"));
        return btn;
    }

    // ---- Webcam QR Scanner ----
    // ---- Webcam QR Scanner ----
private void startQRScanner(TableView<OrderItem> orderTable, ObservableList<OrderItem> orderData) {
    if (scanning) return;
    scanning = true;

    webcamView = new ImageView();
    webcamView.setFitWidth(320);
    webcamView.setFitHeight(240);

    Alert camAlert = new Alert(Alert.AlertType.INFORMATION);
    camAlert.setTitle("QR Scanner");
    camAlert.setHeaderText("Show QR Code to Camera");
    camAlert.getDialogPane().setContent(webcamView);

    new Thread(() -> {
        try {
            Webcam webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR, "❌ No webcam detected!", ButtonType.OK).showAndWait();
                });
                return;
            }
            webcam.open();

            while (scanning) {
                BufferedImage image = webcam.getImage();
                if (image == null) continue;

                Platform.runLater(() -> webcamView.setImage(SwingFXUtils.toFXImage(image, null)));

                try {
                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    Result result = new MultiFormatReader().decode(bitmap);

                    if (result != null) {
                        String scannedQR = result.getText();
                        Platform.runLater(() -> {
                            java.awt.Toolkit.getDefaultToolkit().beep(); // ✅ play beep sound
                            verifyOrderByQR(scannedQR, orderTable, orderData);
                            camAlert.close();
                        });
                        scanning = false;
                        webcam.close();
                        break;
                    }
                } catch (NotFoundException ignored) {}
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }).start();

    camAlert.showAndWait();
    scanning = false;
}


    // ---- Load Menu Items ----
    private void loadMenuData() {
        menuData.clear();
        try (Connection conn = connectDB()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM menu_items");
            while (rs.next()) {
                menuData.add(new MenuItem(
                        rs.getInt("item_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getBoolean("available")
                ));
            }
            menuTable.setItems(menuData);
            menuTable.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMenuItem() {
        try (Connection conn = connectDB()) {
            String query = "INSERT INTO menu_items (name, price, available) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, itemNameField.getText());
            stmt.setDouble(2, Double.parseDouble(itemPriceField.getText()));
            stmt.setBoolean(3, itemAvailableCheck.isSelected());
            stmt.executeUpdate();

            loadMenuData();
            itemNameField.clear();
            itemPriceField.clear();
            itemAvailableCheck.setSelected(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- Remove Menu Item ----
// ---- Remove Menu Item ----
private void removeMenuItem() {
    MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
        Alert a = new Alert(Alert.AlertType.WARNING, "⚠ Please select an item to remove.", ButtonType.OK);
        a.showAndWait();
        return;
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Confirm Removal");
    confirm.setHeaderText("Delete Menu Item");
    confirm.setContentText("Are you sure you want to remove '" + selected.getName() + "' ?");
    java.util.Optional<ButtonType> r = confirm.showAndWait();
    if (!r.isPresent() || r.get() != ButtonType.OK) return;

    try (Connection conn = connectDB()) {
        String sql = "DELETE FROM menu_items WHERE item_id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, selected.getId());
        int rows = ps.executeUpdate();

        if (rows > 0) {
            loadMenuData();
            new Alert(Alert.AlertType.INFORMATION, "✅ Item removed successfully.", ButtonType.OK).showAndWait();
        } else {
            new Alert(Alert.AlertType.WARNING, "No rows were deleted (item may not exist).", ButtonType.OK).showAndWait();
        }

    } catch (java.sql.SQLIntegrityConstraintViolationException fkEx) {
        // foreign key / referenced rows exist
        fkEx.printStackTrace();
        Alert choice = new Alert(Alert.AlertType.CONFIRMATION);
        choice.setTitle("Cannot delete — item is referenced");
        choice.setHeaderText("This item is referenced by other records (e.g. order_items).");
        choice.setContentText("Choose action:\n• Mark Unavailable (recommended)\n• Force Delete related rows (dangerous)\n• Cancel");

        ButtonType markUnavailable = new ButtonType("Mark Unavailable");
        ButtonType forceDelete = new ButtonType("Force Delete (delete related order_items)");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        choice.getButtonTypes().setAll(markUnavailable, forceDelete, cancel);

        java.util.Optional<ButtonType> ch = choice.showAndWait();
        if (ch.isPresent() && ch.get() == markUnavailable) {
            // mark available = false
            try (Connection conn2 = connectDB()) {
                PreparedStatement ps2 = conn2.prepareStatement("UPDATE menu_items SET available = FALSE WHERE item_id = ?");
                ps2.setInt(1, selected.getId());
                ps2.executeUpdate();
                loadMenuData();
                new Alert(Alert.AlertType.INFORMATION, "✅ Item marked unavailable.", ButtonType.OK).showAndWait();
            } catch (Exception ex2) {
                ex2.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error marking unavailable: " + ex2.getMessage(), ButtonType.OK).showAndWait();
            }
        } else if (ch.isPresent() && ch.get() == forceDelete) {
            // extra confirmation
            Alert confirm2 = new Alert(Alert.AlertType.CONFIRMATION,
                    "This will DELETE related order_items rows too. This may affect order history. Proceed?",
                    ButtonType.YES, ButtonType.NO);
            java.util.Optional<ButtonType> c2 = confirm2.showAndWait();
            if (c2.isPresent() && c2.get() == ButtonType.YES) {
                try (Connection conn3 = connectDB()) {
                    conn3.setAutoCommit(false);
                    try (PreparedStatement del1 = conn3.prepareStatement("DELETE FROM order_items WHERE item_id = ?");
                         PreparedStatement del2 = conn3.prepareStatement("DELETE FROM menu_items WHERE item_id = ?")) {

                        del1.setInt(1, selected.getId());
                        del1.executeUpdate();

                        del2.setInt(1, selected.getId());
                        int delRows = del2.executeUpdate();

                        conn3.commit();
                        loadMenuData();
                        new Alert(Alert.AlertType.INFORMATION,
                                "✅ Removed item and deleted related order_items (" + delRows + " rows).",
                                ButtonType.OK).showAndWait();
                    } catch (Exception inner) {
                        conn3.rollback();
                        inner.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Error during forced delete: " + inner.getMessage(), ButtonType.OK).showAndWait();
                    } finally {
                        conn3.setAutoCommit(true);
                    }
                } catch (Exception ex3) {
                    ex3.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Error: " + ex3.getMessage(), ButtonType.OK).showAndWait();
                }
            }
        } // else cancelled
    } catch (SQLException ex) {
        ex.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error removing item: " + ex.getMessage(), ButtonType.OK).showAndWait();
    } catch (Exception ex) {
        ex.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Unexpected error: " + ex.getMessage(), ButtonType.OK).showAndWait();
    }
}



    private void updateBookingWindow() {
        String start = startTimeField.getText();
        String end = endTimeField.getText();
        try (Connection conn = connectDB()) {
            String query = "UPDATE booking_settings SET start_time=?, end_time=? WHERE active=TRUE";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, start);
            stmt.setString(2, end);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                String insert = "INSERT INTO booking_settings (start_time, end_time, active) VALUES (?, ?, TRUE)";
                PreparedStatement stmt2 = conn.prepareStatement(insert);
                stmt2.setString(1, start);
                stmt2.setString(2, end);
                stmt2.executeUpdate();
            }
            System.out.println("✅ Booking window updated: " + start + " - " + end);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadOrders(TableView<OrderItem> orderTable, ObservableList<OrderItem> orderData) {
        orderData.clear();
        try (Connection conn = connectDB()) {
            String query = "SELECT o.order_id, u.username, o.total_amount, o.eta_time, o.status, o.qr_code " +
                           "FROM orders o JOIN users u ON o.user_id = u.id " +
                           "WHERE o.status IN ('pending','ready') ORDER BY o.order_time ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orderData.add(new OrderItem(
                        rs.getInt("order_id"),
                        rs.getString("username"),
                        rs.getDouble("total_amount"),
                        rs.getString("eta_time"),
                        rs.getString("status"),
                        rs.getString("qr_code")
                ));
            }
            orderTable.setItems(orderData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateOrderStatus(int orderId, String newStatus) {
        try (Connection conn = connectDB()) {
            String query = "UPDATE orders SET status=? WHERE order_id=?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- Verify Order by QR ----
   // ---- Verify Order by QR ----
private void verifyOrderByQR(String qrCode, TableView<OrderItem> orderTable, ObservableList<OrderItem> orderData) {
    try (Connection conn = connectDB()) {
        String query = "SELECT order_id FROM orders WHERE qr_code=? AND status IN ('pending','ready')";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, qrCode);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            int orderId = rs.getInt("order_id");

            // Fetch order items
            String itemsQuery = "SELECT m.name, oi.quantity " +
                                "FROM order_items oi " +
                                "JOIN menu_items m ON oi.item_id = m.item_id " +
                                "WHERE oi.order_id = ?";
            PreparedStatement itemsStmt = conn.prepareStatement(itemsQuery);
            itemsStmt.setInt(1, orderId);
            ResultSet itemsRs = itemsStmt.executeQuery();

            StringBuilder sb = new StringBuilder("Order #" + orderId + " contains:\n\n");
            while (itemsRs.next()) {
                sb.append("• ").append(itemsRs.getString("name"))
                  .append(" x ").append(itemsRs.getInt("quantity"))
                  .append("\n");
            }

            // Show order details
            Alert orderPopup = new Alert(Alert.AlertType.INFORMATION);
            orderPopup.setTitle("Order Verified");
            orderPopup.setHeaderText("✅ QR Verified Successfully!");
            orderPopup.setContentText(sb.toString());
            orderPopup.showAndWait();

            // Auto-mark completed (no orange popup anymore)
            updateOrderStatus(orderId, "completed");
            loadOrders(orderTable, orderData);

        } else {
            new Alert(Alert.AlertType.ERROR, "❌ No active order found for this QR.", ButtonType.OK).showAndWait();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
// ---- Load User Balance ----
private void loadUserBalance() {
    String userid = userIdBalanceField.getText();
    if (userid.isEmpty()) {
        new Alert(Alert.AlertType.WARNING, "⚠ Please enter a UserID", ButtonType.OK).showAndWait();
        return;
    }

    try (Connection conn = connectDB()) {
        String query = "SELECT balance FROM users WHERE userid = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, userid);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            double balance = rs.getDouble("balance");
            currentBalanceLabel.setText("Current Balance: ₹" + balance);
        } else {
            currentBalanceLabel.setText("User not found!");
        }
    } catch (Exception e) {
        e.printStackTrace();
        currentBalanceLabel.setText("Error fetching balance");
    }
}

// ---- Update User Balance ----
// ---- Update User Balance (Add to existing) ----
private void updateUserBalance() {
    String userid = userIdBalanceField.getText();
    String addBalText = newBalanceField.getText();

    if (userid.isEmpty() || addBalText.isEmpty()) {
        new Alert(Alert.AlertType.WARNING, "⚠ Enter UserID and Amount to Add", ButtonType.OK).showAndWait();
        return;
    }

    try {
        double addAmount = Double.parseDouble(addBalText);

        try (Connection conn = connectDB()) {
            String query = "UPDATE users SET balance = balance + ? WHERE userid = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setDouble(1, addAmount);
            stmt.setString(2, userid);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                // Fetch new balance
                PreparedStatement checkStmt = conn.prepareStatement("SELECT balance FROM users WHERE userid=?");
                checkStmt.setString(1, userid);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    double updatedBalance = rs.getDouble("balance");
                    currentBalanceLabel.setText("✅ Balance updated! New Balance: ₹" + updatedBalance);
                }
            } else {
                currentBalanceLabel.setText("User not found!");
            }
        }
    } catch (NumberFormatException nfe) {
        new Alert(Alert.AlertType.ERROR, "❌ Invalid amount", ButtonType.OK).showAndWait();
    } catch (Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "❌ Error updating balance", ButtonType.OK).showAndWait();
    }
}





    // ---- Models ----
    public static class MenuItem {
        private int id; private String name; private double price; private boolean available;
        public MenuItem(int id, String name, double price, boolean available) {
            this.id=id; this.name=name; this.price=price; this.available=available;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public boolean isAvailable() { return available; }
    }

    public static class OrderItem {
        private int orderId; private String username; private double amount; private String eta; private String status; private String qrCode;
        public OrderItem(int orderId, String username, double amount, String eta, String status, String qrCode) {
            this.orderId=orderId; this.username=username; this.amount=amount; this.eta=eta; this.status=status; this.qrCode=qrCode;
        }
        public int getOrderId(){return orderId;}
        public String getUsername(){return username;}
        public double getAmount(){return amount;}
        public String getEta(){return eta;}
        public String getStatus(){return status;}
        public String getQrCode(){return qrCode;}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
