module com.toch.proj {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.graphics;  
    requires java.sql;

    // ZXing (QR codes)
    requires com.google.zxing;
    requires com.google.zxing.javase;
    

    // Webcam Capture (Sarxos) – automatic module
    requires webcam.capture;

    // SLF4J (logger) – automatic module
    // If this fails, change to: requires slf4j.simple;

    // Allow JavaFX FXML to use reflection on your classes
    opens com.toch.proj to javafx.fxml;
    exports com.toch.proj;
}
