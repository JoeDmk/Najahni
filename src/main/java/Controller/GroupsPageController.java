package Controller;

import Entites.Group;
import Services.GroupCRUD;
import Services.ThreadCRUD;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class GroupsPageController {
    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private VBox groupsContainer;

    @FXML
    public void initialize() {

        sortComboBox.getItems().addAll(
                "Name (A-Z)",
                "Privacy (Public First)",
                "Most Active"
        );

        sortComboBox.setValue("Name (A-Z)");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            loadGroups();
        });


        loadGroups();
    }
    @FXML
    private void handleSortChange() {
        loadGroups();
    }
    private int getThreadCount(int groupId) {

        try {
            return new ThreadCRUD().getThreadsByGroup(groupId).size();
        } catch (Exception e) {
            return 0;
        }
    }


    private void loadGroups() {

        groupsContainer.getChildren().clear();

        GroupCRUD crud = new GroupCRUD();

        try {
            List<Group> groups = crud.afficher();

            String selectedSort = sortComboBox.getValue();
            String searchText = searchField.getText().toLowerCase().trim();

            //text  search
            if (!searchText.isEmpty()) {
                groups.removeIf(group ->
                        !group.getName().toLowerCase().contains(searchText)
                                && !group.getDescription().toLowerCase().contains(searchText));
            }
            if (groups.isEmpty()) {
                Label emptyLabel = new Label("No groups found.");
                emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14;");
                groupsContainer.getChildren().add(emptyLabel);
                return;
            }


            //tri

            if (selectedSort.equals("Name (A-Z)")) {

                groups.sort((g1, g2) ->
                        g1.getName().compareToIgnoreCase(g2.getName()));

            } else if (selectedSort.equals("Privacy (Public First)")) {

                groups.sort((g1, g2) ->
                        Boolean.compare(g1.getIsPrivate(), g2.getIsPrivate()));

            } else if (selectedSort.equals("Most Active")) {

                groups.sort((g1, g2) ->
                        Integer.compare(
                                getThreadCount(g2.getId()),
                                getThreadCount(g1.getId())
                        ));
            }

            for (Group g : groups) {
                VBox card = createGroupCard(g);
                groupsContainer.getChildren().add(card);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    private VBox createGroupCard(Group group) {

        VBox card = new VBox();
        card.setSpacing(8);
        card.setStyle("""
    -fx-background-color: white;
    -fx-padding: 20;
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);
""");


        // 🔹 Top row (Name + Privacy)
        HBox topRow = new HBox();
        topRow.setSpacing(10);

        Label name = new Label(group.getName());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label privacyLabel = new Label();

        if (group.getIsPrivate()) {
            privacyLabel.setText("🔒 Private");
            privacyLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            privacyLabel.setText("🔓 Public");
            privacyLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }

        // Push privacy to right side
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(name, spacer, privacyLabel);

        // 🔹 Description
        Label desc = new Label(group.getDescription());
        desc.setWrapText(true);

        card.getChildren().addAll(topRow, desc);

        // 🔥 Make clickable
        card.setOnMouseClicked(event -> openGroupDashboard(group));

        return card;
    }



    @FXML
        private void goToAddGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddGroup.fxml"));
            Parent root = loader.load();

            groupsContainer.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    private void openGroupDashboard(Group group) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupDashboard.fxml"));
            Parent root = loader.load();

            GroupDashboardController controller = loader.getController();
            controller.setGroup(group);

            groupsContainer.getScene().setRoot(root);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }



    @FXML
    private void goToHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/CommunityHomePage.fxml"));
            groupsContainer.getScene().setRoot(root);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }






}
