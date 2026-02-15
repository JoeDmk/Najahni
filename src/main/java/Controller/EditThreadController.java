package Controller;

import Entites.Group;
import Entites.Thread;
import Services.ThreadCRUD;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EditThreadController {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea contentField;

    private Thread thread;
    private Group group;

    public void setThread(Thread thread) {
        this.thread = thread;
        titleField.setText(thread.getTitle());
        contentField.setText(thread.getContent());
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @FXML
    private void updateThread() {
        if (titleField.getText().trim().isEmpty()) return;
        if (contentField.getText().trim().isEmpty()) return;

        thread.setTitle(titleField.getText());
        thread.setContent(contentField.getText());

        ThreadCRUD crud = new ThreadCRUD();

        try {
            crud.modifier(thread);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        goBack();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupDashboard.fxml"));
            Parent root = loader.load();

            GroupDashboardController controller = loader.getController();
            controller.setGroup(group);

            titleField.getScene().setRoot(root);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
