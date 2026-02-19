module com.najahni {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;
    requires java.net.http;
    requires weka.stable;
    requires java.logging;

    opens com.najahni to javafx.fxml;
    opens com.najahni.controllers to javafx.fxml;
    opens com.najahni.models to javafx.base;

    exports com.najahni;
    exports com.najahni.controllers;
    exports com.najahni.models;
    exports com.najahni.services;
    exports com.najahni.services.ml;
    exports com.najahni.utils;
}
