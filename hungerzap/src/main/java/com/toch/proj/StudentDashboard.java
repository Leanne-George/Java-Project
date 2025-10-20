package com.toch.proj;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

public class StudentDashboard extends Application {

    private int userId;
    private double balance;
    private String userName = "User";

    private Label balanceLabel, totalLabel;
    private TableView<MenuItem> menuTable;
    private TableView<CartItem> cartTable;
    private TableView<OrderHistoryItem> historyTable;

    private ObservableList<MenuItem> menuData;
    private ObservableList<CartItem> cartData;
    private ObservableList<OrderHistoryItem> historyData;

    private Connection connectDB() throws SQLException, ClassNotFoundException {
        String url = "jdbc:mysql://localhost:3306/myprojectdb";
        String user = "root";
        String password = "Mysql25"; // change if needed
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    public StudentDashboard(int userId) {
        this.userId = userId;
    }

    @Override
    public void start(Stage primaryStage) {
        // Title
        Label title = new Label("QuickDine - User Dashboard");
        title.setFont(Font.font("Verdana", 24));
        title.getStyleClass().add("app-title-small"); // use css class

        // Welcome and balance labels
        Label welcomeLabel = new Label("Welcome, " + userName + "!");
        welcomeLabel.getStyleClass().add("welcome-label");
        balanceLabel = new Label("Balance: Loading...");
        balanceLabel.setFont(Font.font("Arial", 16));
        balanceLabel.getStyleClass().add("balance-label");

        // Fetch username and balance from DB
        loadUserName();
        welcomeLabel.setText("Welcome, " + userName + "!");
        loadBalance();

        // --- Menu Table ---
        menuTable = new TableView<>();
        menuData = FXCollections.observableArrayList();
        menuTable.setItems(menuData);
        menuTable.setPrefHeight(180);
        menuTable.setPrefWidth(700);
        menuTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<MenuItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setMinWidth(240);

        TableColumn<MenuItem, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setMinWidth(100);

        TableColumn<MenuItem, Boolean> availableCol = new TableColumn<>("Available");
        availableCol.setCellValueFactory(new PropertyValueFactory<>("available"));
        availableCol.setMinWidth(120);

        menuTable.getColumns().addAll(nameCol, priceCol, availableCol);
        loadMenuData();

        // --- Cart Table ---
        cartTable = new TableView<>();
        cartData = FXCollections.observableArrayList();
        cartTable.setItems(cartData);
        cartTable.setPrefHeight(160);
        cartTable.setPrefWidth(420);
        // Make columns auto-fit and prevent horizontal scrollbar
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CartItem, String> cartNameCol = new TableColumn<>("Item");
        cartNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        cartNameCol.setMinWidth(200);

        TableColumn<CartItem, Integer> cartQtyCol = new TableColumn<>("Qty");
        cartQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartQtyCol.setMinWidth(80);

        TableColumn<CartItem, Double> cartTotalCol = new TableColumn<>("Total");
        cartTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        cartTotalCol.setMinWidth(120);

        cartTable.getColumns().addAll(cartNameCol, cartQtyCol, cartTotalCol);

        Spinner<Integer> qtySpinner = new Spinner<>(1, 10, 1);
        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.getStyleClass().add("primary-btn-small");
        addToCartBtn.setOnAction(e -> {
            MenuItem selected = menuTable.getSelectionModel().getSelectedItem();
            if (selected != null) addToCart(selected, qtySpinner.getValue());
            else {
                Alert a = new Alert(Alert.AlertType.WARNING, "Please select a menu item first.", ButtonType.OK);
                a.showAndWait();
            }
        });

        // --- Remove / Clear Cart ---
        Button removeFromCartBtn = new Button("Remove Selected");
        removeFromCartBtn.getStyleClass().add("secondary-btn-small");
        removeFromCartBtn.setOnAction(e -> {
            CartItem selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                cartData.remove(selected);
                updateCartTotal();
            }
        });

        Button clearCartBtn = new Button("Clear Cart");
        clearCartBtn.getStyleClass().add("secondary-btn-small");
        clearCartBtn.setOnAction(e -> {
            cartData.clear();
            updateCartTotal();
        });

        HBox cartBtns = new HBox(10, removeFromCartBtn, clearCartBtn);
        cartBtns.setAlignment(Pos.CENTER_LEFT);

        totalLabel = new Label("Cart Total: ₹0.00");
        totalLabel.getStyleClass().add("cart-total-label");
        Button placeOrderBtn = new Button("Place Order");
        placeOrderBtn.getStyleClass().add("primary-btn");

        Label messageLabel = new Label();

        placeOrderBtn.setOnAction(e -> placeOrderWithQR(messageLabel));

        // --- Order History Table ---
        historyTable = new TableView<>();
        historyData = FXCollections.observableArrayList();
        historyTable.setItems(historyData);
        historyTable.setPrefHeight(220);
        historyTable.setPrefWidth(940);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<OrderHistoryItem, Integer> histIdCol = new TableColumn<>("Order ID");
        histIdCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        histIdCol.setMinWidth(80);

        TableColumn<OrderHistoryItem, Double> histAmountCol = new TableColumn<>("Amount");
        histAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        histAmountCol.setMinWidth(100);

        TableColumn<OrderHistoryItem, String> histEtaCol = new TableColumn<>("ETA");
        histEtaCol.setCellValueFactory(new PropertyValueFactory<>("eta"));
        histEtaCol.setMinWidth(160);

        TableColumn<OrderHistoryItem, String> histStatusCol = new TableColumn<>("Status");
        histStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        histStatusCol.setMinWidth(120);

        TableColumn<OrderHistoryItem, String> histQrCol = new TableColumn<>("QR Code");
        histQrCol.setCellValueFactory(new PropertyValueFactory<>("qrCode"));
        histQrCol.setMinWidth(340);

        historyTable.getColumns().addAll(histIdCol, histAmountCol, histEtaCol, histStatusCol, histQrCol);

        // Add "View QR" button column
        TableColumn<OrderHistoryItem, Void> qrViewCol = new TableColumn<>("Show QR");
        qrViewCol.setMinWidth(100);
        qrViewCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View QR");
            {
                btn.getStyleClass().add("primary-btn-small");
                btn.setOnAction(e -> {
                    OrderHistoryItem order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        showQRCodePopup(order.getQrCode());
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btn);
            }
        });
        historyTable.getColumns().add(qrViewCol);

        loadOrderHistory();

        // Layout using cards (keeps consistent look with login CSS)
        VBox menuCard = new VBox(8, new Label("Menu"), menuTable, new HBox(10, new Label("Qty:"), qtySpinner, addToCartBtn));
        menuCard.setPadding(new Insets(12));
        menuCard.getStyleClass().add("dashboard-card");

        VBox cartCard = new VBox(8, new Label("Cart"), cartTable, cartBtns, totalLabel, placeOrderBtn);
        cartCard.setPadding(new Insets(12));
        cartCard.getStyleClass().add("dashboard-card");

        HBox topRow = new HBox(20, menuCard, cartCard);
        topRow.setAlignment(Pos.CENTER);

        VBox ordersCard = new VBox(8, new Label("Order History"), historyTable);
        ordersCard.setPadding(new Insets(12));
        ordersCard.getStyleClass().add("dashboard-card");

        VBox header = new VBox(6, title, welcomeLabel, balanceLabel);
        header.setAlignment(Pos.CENTER);

        VBox main = new VBox(18, header, topRow, ordersCard);
        main.setPadding(new Insets(20));
        main.setAlignment(Pos.TOP_CENTER);

        StackPane root = new StackPane(main);
        root.getStyleClass().add("root-bg"); // uses same css as login

        Scene scene = new Scene(root, 980, 720);
        // attach your existing stylesheet so look matches login
        try {
            scene.getStylesheets().add(getClass().getResource("/com/toch/proj/login.css").toExternalForm());
        } catch (Exception ex) {
            // if stylesheet missing, fallback to inline gradient
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #E4F1EE, #D9EDF8);");
        }

        primaryStage.setTitle("QuickDine - User Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- Balance & DB ---
    private void loadBalance() {
        try (Connection conn = connectDB()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT balance FROM users WHERE id=?");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                balance = rs.getDouble("balance");
                balanceLabel.setText(String.format("Balance: ₹%.2f", balance));
            } else {
                balanceLabel.setText("Balance: ₹0.00");
            }
        } catch (Exception e) { e.printStackTrace(); balanceLabel.setText("Balance: ₹0.00"); }
    }

    private void loadUserName() {
        try (Connection conn = connectDB()) {
            PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE id=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) userName = rs.getString("username");
        } catch (Exception e) { e.printStackTrace(); userName = "User"; }
    }

    private void loadMenuData() {
        menuData.clear();
        try (Connection conn = connectDB()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM menu_items");
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addToCart(MenuItem item, int qty) {
        for (CartItem ci : cartData) {
            if (ci.getId() == item.getId()) {
                ci.setQuantity(ci.getQuantity() + qty, item.getPrice());
                cartTable.refresh();
                updateCartTotal();
                return;
            }
        }
        cartData.add(new CartItem(item.getId(), item.getName(), qty, item.getPrice() * qty));
        updateCartTotal();
    }

    private void updateCartTotal() {
        double total = cartData.stream().mapToDouble(CartItem::getTotal).sum();
        totalLabel.setText(String.format("Cart Total: ₹%.2f", total));
    }

    // --- Place order with QR ---
    private void placeOrderWithQR(Label messageLabel) {
        double total = cartData.stream().mapToDouble(CartItem::getTotal).sum();
        if (total == 0) { messageLabel.setText("⚠ Cart is empty."); messageLabel.setTextFill(Color.web("#AF3833")); return; }
        if (total > balance) { messageLabel.setText("❌ Not enough balance!"); messageLabel.setTextFill(Color.web("#AF3833")); return; }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Order");
        confirmAlert.setHeaderText("Place Order?");
        confirmAlert.setContentText("Are you sure you want to place this order for ₹" + String.format("%.2f", total) + "?");

        ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
        ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(yesBtn, noBtn);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == yesBtn) {
                try (Connection conn = connectDB()) {
                    conn.setAutoCommit(false);

                    String qrCodeText = "QR-" + UUID.randomUUID();
                    String filePath = "qrcodes/" + qrCodeText + ".png";

                    PreparedStatement orderStmt = conn.prepareStatement(
                            "INSERT INTO orders (user_id, total_amount, eta_time, status, qr_code) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 30 MINUTE), 'pending', ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    orderStmt.setInt(1, userId);
                    orderStmt.setDouble(2, total);
                    orderStmt.setString(3, qrCodeText);
                    orderStmt.executeUpdate();

                    ResultSet keys = orderStmt.getGeneratedKeys();
                    int orderId = keys.next() ? keys.getInt(1) : 0;

                    PreparedStatement itemStmt = conn.prepareStatement("INSERT INTO order_items (order_id, item_id, quantity) VALUES (?, ?, ?)");
                    for (CartItem ci : cartData) {
                        itemStmt.setInt(1, orderId);
                        itemStmt.setInt(2, ci.getId());
                        itemStmt.setInt(3, ci.getQuantity());
                        itemStmt.addBatch();
                    }
                    itemStmt.executeBatch();

                    PreparedStatement balStmt = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE id=?");
                    balStmt.setDouble(1, total);
                    balStmt.setInt(2, userId);
                    balStmt.executeUpdate();

                    conn.commit();

                    // --- QR Generation ---
                    File dir = new File("qrcodes");
                    if (!dir.exists()) dir.mkdirs();

                    QRCodeWriter qrCodeWriter = new QRCodeWriter();
                    BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 250, 250);
                    Path path = FileSystems.getDefault().getPath(filePath);
                    MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

                    // Show QR popup
                    Image img = new Image(new FileInputStream(filePath));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(240);
                    iv.setFitHeight(240);
                    Alert qrAlert = new Alert(Alert.AlertType.INFORMATION);
                    qrAlert.setTitle("Order Placed - QR Code");
                    qrAlert.setHeaderText("Show this QR at pickup");
                    qrAlert.getDialogPane().setContent(iv);
                    qrAlert.showAndWait();

                    cartData.clear();
                    updateCartTotal();
                    loadBalance();
                    loadOrderHistory();

                    messageLabel.setText("✅ Order placed!");
                    messageLabel.setTextFill(Color.web("#53B176"));

                } catch (Exception e) {
                    e.printStackTrace();
                    messageLabel.setText("⚠ Error placing order.");
                    messageLabel.setTextFill(Color.web("#AF3833"));
                }
            } else {
                messageLabel.setText("❌ Order cancelled.");
                messageLabel.setTextFill(Color.web("#AF3833"));
            }
        });
    }

    private void loadOrderHistory() {
        historyData.clear();
        try (Connection conn = connectDB()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT order_id, total_amount, eta_time, status, qr_code FROM orders WHERE user_id=? ORDER BY order_time DESC LIMIT 50");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                historyData.add(new OrderHistoryItem(
                        rs.getInt("order_id"), rs.getDouble("total_amount"),
                        rs.getString("eta_time"), rs.getString("status"),
                        rs.getString("qr_code")
                ));
            }
            historyTable.setItems(historyData);
            historyTable.refresh();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ---- Show QR Popup ----
    private void showQRCodePopup(String qrCode) {
        try {
            String filePath = "qrcodes/" + qrCode + ".png";
            File qrFile = new File(filePath);

            if (!qrFile.exists()) {
                new Alert(Alert.AlertType.ERROR, "❌ QR file not found!", ButtonType.OK).showAndWait();
                return;
            }

            Image qrImage = new Image(new FileInputStream(qrFile));
            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(250);
            qrView.setFitHeight(250);

            Alert popup = new Alert(Alert.AlertType.INFORMATION);
            popup.setTitle("Order QR Code");
            popup.setHeaderText("Show this QR at collection point");
            popup.getDialogPane().setContent(qrView);
            popup.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "⚠ Error opening QR image", ButtonType.OK).showAndWait();
        }
    }

    // --- Models ---
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

    public static class CartItem {
        private int id; private String name; private int quantity; private double total;
        public CartItem(int id, String name, int quantity, double total) {
            this.id=id; this.name=name; this.quantity=quantity; this.total=total;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getTotal() { return total; }
        public void setQuantity(int q, double pricePerUnit) { this.quantity=q; this.total=q*pricePerUnit; }
    }

    public static class OrderHistoryItem {
        private int orderId; private double amount; private String eta; private String status; private String qrCode;
        public OrderHistoryItem(int orderId,double amount,String eta,String status,String qrCode){
            this.orderId=orderId;this.amount=amount;this.eta=eta;this.status=status;this.qrCode=qrCode;
        }
        public int getOrderId(){return orderId;}
        public double getAmount(){return amount;}
        public String getEta(){return eta;}
        public String getStatus(){return status;}
        public String getQrCode(){return qrCode;}
    }

    public static void main(String[] args) {
        new StudentDashboard(2).start(new Stage()); // test with userId=2
    }
}
