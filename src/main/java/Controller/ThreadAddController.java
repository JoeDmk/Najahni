package Controller;

import Entites.Group;
import Entites.Thread;
import Services.ThreadCRUD;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ThreadAddController {
    @FXML
    private Label errorLabel;

    @FXML
    private TextField titleField;

    @FXML
    private TextArea contentArea;

    private int groupId;
    // TODO: Replace with SessionService.getInstance().getCurrentUser().getId()
    // Temporary logged user for testing (integration with User module not done yet)
    private final int currentUserId = 1;


    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
    private Group group;

    public void setGroup(Group group) {
        this.group = group;
    }


    @FXML
    private void createThread() {

        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        // 🔥 VALIDATION
        if (title.isEmpty()) {
            errorLabel.setText("Title cannot be empty.");
            return;
        }

        if (title.length() < 3) {
            errorLabel.setText("Title must be at least 3 characters.");
            return;
        }

        if (content.isEmpty()) {
            errorLabel.setText("Content cannot be empty.");
            return;
        }

        if (content.length() < 5) {
            errorLabel.setText("Content must be at least 5 characters.");
            return;
        }

        errorLabel.setText(""); // clear errors

        ThreadCRUD threadCRUD = new ThreadCRUD();

        try {
            Thread thread = new Thread(
                    groupId,
                    currentUserId,
                    title,
                    content
            );

            threadCRUD.ajouter(thread);

            goBackToDashboard();


        } catch (Exception e) {
            errorLabel.setText("Error creating thread.");
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void goBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupDashboard.fxml"));
            Parent root = loader.load();

            GroupDashboardController controller = loader.getController();
            controller.setGroup(group); // 🔥 THIS IS THE FIX

            titleField.getScene().setRoot(root);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
