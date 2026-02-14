module com.najahni {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;

    opens com.najahni to javafx.fxml;
    opens com.najahni.controllers to javafx.fxml;
    opens com.najahni.models to javafx.base;

    exports com.najahni;
    exports com.najahni.controllers;
    exports com.najahni.models;
    exports com.najahni.dao;
    exports com.najahni.services;
    exports com.najahni.utils;
}
