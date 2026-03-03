package controllers.community;
import models.community.*;
import services.SessionService;
import services.community.*;
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/community/GroupsPage.fxml"));
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

         final int currentUserId = SessionService.getInstance().getCurrentUser().getId();
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

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/community/GroupsPage.fxml"));
            Parent root = loader.load();
            creategroup.getScene().setRoot(root);

        } catch (SQLException | IOException e) {

            if (e instanceof SQLException && "GROUP_NAME_EXISTS".equals(e.getMessage())) {
                statusLabel.setText("Group name already exists.");
            } else {
                statusLabel.setText("Error adding group");
                System.out.println(e.getMessage());
            }
        }
    }

    @FXML
    private void handleGoBack() {
        goBack();
    }

    @FXML
    private void handleGoHome() {
        tools.NavigationHelper.goHome(creategroup);
    }



}
