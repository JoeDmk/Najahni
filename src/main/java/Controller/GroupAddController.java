package Controller;
import Entites.*;
import Services.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

public class GroupAddController {
    @FXML
    private CheckBox privateCheckBox;

    @FXML
    private Button creategroup;

    @FXML
    private TextField groupdescription;

    @FXML
    private TextField groupname;

    @FXML
    private Label statusLabel;

    @FXML
    void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupsPage.fxml"));
            Parent root = loader.load();
            creategroup.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    void addgroup() {

        if (groupname.getText().isEmpty()) {
            statusLabel.setText("Group name is required");
            return;
        }

        // TODO: Replace with SessionService.getInstance().getCurrentUser().getId()
        // Temporary logged user for testing (integration with User module not done yet)
         final int currentUserId = 1;
        // simulate logged user

        Group g = new Group(
                groupname.getText(),
                groupdescription.getText(),
                currentUserId,
                privateCheckBox.isSelected()
        );


        GroupCRUD groupCRUD = new GroupCRUD();

        try {

            groupCRUD.ajouter(g);
            GroupMemberCRUD gmCrud = new GroupMemberCRUD();
            gmCrud.ajouter(new GroupMember(g.getId(), currentUserId));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupsPage.fxml"));
            Parent root = loader.load();
            creategroup.getScene().setRoot(root);

        } catch (SQLException | IOException e) {
            statusLabel.setText("Error adding group");
            System.out.println(e.getMessage());
        }
    }




}
