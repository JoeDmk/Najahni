module com.najahni {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.swing;
    requires transitive java.sql;
    requires java.net.http;
    requires java.logging;
    requires java.desktop;

    // PDF generation (OpenPDF) & rendering (PDFBox)
    requires com.github.librepdf.openpdf;
    requires org.apache.pdfbox;

    opens com.najahni to javafx.fxml;
    opens com.najahni.controllers to javafx.fxml;
    opens com.najahni.models to javafx.base, javafx.fxml;

    exports com.najahni;
    exports com.najahni.controllers;
    exports com.najahni.models;
    exports com.najahni.services;
    exports com.najahni.utils;
}
