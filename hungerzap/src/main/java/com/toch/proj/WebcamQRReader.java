package com.toch.proj;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class WebcamQRReader extends Application {

    private volatile boolean running = true;
    private Label resultLabel = new Label("Scan a QR Code...");

    @Override
    public void start(Stage stage) {
        stage.setTitle("QR Code Scanner");

        // Open default webcam
        Webcam webcam = Webcam.getDefault();
        webcam.setViewSize(new java.awt.Dimension(640, 480));

        // Swing panel for webcam feed
        WebcamPanel webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setMirrored(true);

        // Wrap Swing panel in a SwingNode (for JavaFX)
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> swingNode.setContent(webcamPanel));

        // Layout
        BorderPane root = new BorderPane();
        root.setCenter(swingNode);
        root.setBottom(resultLabel);

        Scene scene = new Scene(root, 640, 520);
        stage.setScene(scene);
        stage.show();

        // Start background thread for scanning
        new Thread(() -> {
            while (running) {
                try {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        LuminanceSource source = new BufferedImageLuminanceSource(image);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                        try {
                            Result result = new MultiFormatReader().decode(bitmap);
                            if (result != null) {
                                Platform.runLater(() -> resultLabel.setText("QR Code: " + result.getText()));
                            }
                        } catch (NotFoundException e) {
                            // no QR code in frame
                        }
                    }
                    Thread.sleep(200); // reduce CPU load
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Clean up when window closes
        stage.setOnCloseRequest(e -> {
            running = false;
            webcam.close();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
