    package Controller;
    import Services.GroupJoinRequestCRUD;
    import Entites.GroupJoinRequest;

    import Entites.Group;
    import Entites.GroupMember;
    import Services.GroupCRUD;
    import Services.GroupMemberCRUD;
    import Services.ThreadCRUD;
    import javafx.fxml.FXML;
    import javafx.fxml.FXMLLoader;
    import javafx.scene.Parent;
    import javafx.scene.control.*;
    import javafx.scene.layout.BorderPane;
    import javafx.scene.layout.HBox;
    import javafx.scene.layout.VBox;
    import Entites.Thread;

    import java.sql.SQLException;
    import java.util.List;

    public class GroupDashboardController {
        @FXML
        private HBox mainContentBox;

        @FXML
        private BorderPane rootPane;

        @FXML
        private VBox pendingRequestsBox;

        @FXML
        private Button leaveButton;

        @FXML
        private Label threadsLabel;
        @FXML
        private VBox membersContainer;

        @FXML
        private Label memberCountLabel;

        @FXML
        private Label groupName;

        @FXML
        private Label groupDescription;

        @FXML
        private Button joinButton;

        @FXML
        private Button addThreadButton;

        @FXML
        private VBox threadsContainer;

        private Group group;


        private final int currentUserId = 1;
        private String softWrapLongTokens(String text, int every) {
            if (text == null) return "";
            // Inserts zero-width spaces so Label can wrap even without spaces
            return text.replaceAll("(.{" + every + "})", "$1\u200B");
        }
        private VBox createAdminPanel() {

            VBox card = new VBox(15);
            card.setPrefWidth(280);

            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-padding: 20;" +
                            "-fx-background-radius: 12;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);"
            );

            Label title = new Label("Pending Requests");
            title.setStyle("-fx-font-size:16px; -fx-font-weight:bold;");

            pendingRequestsBox = new VBox(10);

            card.getChildren().addAll(title, pendingRequestsBox);

            return card;
        }


        public void setGroup(Group group) {
            this.group = group;

            groupName.setText(group.getName());
            groupDescription.setText(group.getDescription());

            checkMembership();
        }
        private boolean isAdmin() {
            return group.getGroupAdminId() == currentUserId;
        }

        private void checkMembership() {

            GroupMemberCRUD gmCrud = new GroupMemberCRUD();

            try {
                boolean isMember = gmCrud.isUserMember(group.getId(), currentUserId);

                if (isMember) {

                    joinButton.setVisible(false);
                    leaveButton.setVisible(true);
                    addThreadButton.setVisible(true);
                    threadsLabel.setVisible(true);

                    memberCountLabel.setVisible(true);
                    membersContainer.setVisible(true);

                    loadThreads();
                    loadMembers();
                    if (isAdmin() && group.getIsPrivate()) {

                        if (pendingRequestsBox == null) {
                            VBox adminPanel = createAdminPanel();
                            mainContentBox.getChildren().add(adminPanel);
                        }

                        loadPendingRequests();

                    } else {

                        if (pendingRequestsBox != null) {
                            mainContentBox.getChildren().remove(pendingRequestsBox);
                            pendingRequestsBox = null;
                        }
                    }
                }
                else {

                    joinButton.setVisible(true);
                    leaveButton.setVisible(false);
                    addThreadButton.setVisible(false);
                    threadsLabel.setVisible(false);

                    memberCountLabel.setVisible(false);
                    membersContainer.setVisible(false);

                    threadsContainer.getChildren().clear();
                    membersContainer.getChildren().clear();

                    // 🔥 NEW LOGIC
                    try {
                        GroupJoinRequestCRUD requestCRUD = new GroupJoinRequestCRUD();

                        if (group.getIsPrivate() &&
                                requestCRUD.hasPendingRequest(group.getId(), currentUserId)) {

                            joinButton.setText("Request Pending");
                            joinButton.setDisable(true);
                        } else {
                            joinButton.setText("Join Group");
                            joinButton.setDisable(false);
                        }

                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }
                }



            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        private void loadPendingRequests() {

            pendingRequestsBox.getChildren().clear();
            pendingRequestsBox.setVisible(true);

            GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();

            try {

                List<GroupJoinRequest> requests =
                        crud.getPendingRequests(group.getId());

                if (requests.isEmpty()) {
                    Label empty = new Label("No pending requests.");
                    pendingRequestsBox.getChildren().add(empty);
                    return;
                }

                for (GroupJoinRequest req : requests) {

                    HBox row = new HBox(10);

                    Label name = new Label(req.getUsername());

                    Button approveBtn = new Button("Approve");
                    Button rejectBtn = new Button("Reject");

                    approveBtn.setOnAction(e -> approveRequest(req));
                    rejectBtn.setOnAction(e -> rejectRequest(req));

                    row.getChildren().addAll(name, approveBtn, rejectBtn);

                    pendingRequestsBox.getChildren().add(row);
                }

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        private void approveRequest(GroupJoinRequest req) {

            GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();
            GroupMemberCRUD memberCRUD = new GroupMemberCRUD();

            try {

                crud.approveRequest(req.getId());

                memberCRUD.ajouter(
                        new GroupMember(req.getGroupId(), req.getUserId())
                );

                loadPendingRequests();
                loadMembers();

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        private void rejectRequest(GroupJoinRequest req) {

            GroupJoinRequestCRUD crud = new GroupJoinRequestCRUD();

            try {
                crud.rejectRequest(req.getId());
                loadPendingRequests();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        @FXML
        private void joinGroup() {

            // 🔒 IF PRIVATE GROUP → SEND REQUEST
            if (group.getIsPrivate()) {

                try {

                    GroupJoinRequestCRUD requestCRUD = new GroupJoinRequestCRUD();

                    if (requestCRUD.hasPendingRequest(group.getId(), currentUserId)) {
                        joinButton.setText("Request Pending");
                        joinButton.setDisable(true);
                        return;
                    }

                    requestCRUD.ajouter(
                            new GroupJoinRequest(group.getId(), currentUserId)
                    );

                    joinButton.setText("Request Sent");
                    joinButton.setDisable(true);

                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }

                return;
            }

            // 🟢 PUBLIC GROUP → JOIN DIRECTLY
            GroupMemberCRUD gmCrud = new GroupMemberCRUD();

            try {
                gmCrud.ajouter(new GroupMember(group.getId(), currentUserId));
            } catch (SQLException e) {
                System.out.println("Already a member.");
            }

            checkMembership();
        }
        private void deleteThread(int threadId) {

            if (!confirmAction("Delete Thread",
                    "Are you sure you want to delete this thread?"))
                return;

            ThreadCRUD crud = new ThreadCRUD();
            try {
                crud.supprimer(threadId);
                loadThreads();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        private void openEditThread(Thread thread) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditThread.fxml"));
                Parent root = loader.load();

                EditThreadController controller = loader.getController();
                controller.setThread(thread);
                controller.setGroup(group); // so we can go back correctly

                groupName.getScene().setRoot(root);

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }


        private void loadThreads() {

            threadsContainer.getChildren().clear();

            ThreadCRUD threadCRUD = new ThreadCRUD();

            try {
                threadCRUD.getThreadsByGroup(group.getId())
                        .forEach(thread -> {

                            VBox card = new VBox(10);
                            String normal =
                                    "-fx-background-color: white;" +
                                            "-fx-padding: 18;" +
                                            "-fx-background-radius: 14;" +
                                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 4);" +
                                            "-fx-border-color: transparent;" +
                                            "-fx-border-radius: 14;";

                            String hover =
                                    "-fx-background-color: white;" +
                                            "-fx-padding: 18;" +
                                            "-fx-background-radius: 14;" +
                                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 4);" + // ✅ SAME shadow (no expansion)
                                            "-fx-border-color: rgba(52,152,219,0.35);" +
                                            "-fx-border-width: 1.2;" +
                                            "-fx-border-radius: 14;";

                            card.setStyle(normal);

                            card.setOnMouseEntered(e -> card.setStyle(hover));
                            card.setOnMouseExited(e -> card.setStyle(normal));

                            Label author = new Label("By " + thread.getFirstname());
                            author.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12;");

                            Label title = new Label(thread.getTitle());
                            title.setMinWidth(0);
                            title.maxWidthProperty().bind(card.widthProperty().subtract(20));
                            title.setWrapText(true);
                            title.setStyle("-fx-font-size:16px; -fx-font-weight: bold; -fx-text-fill:#111827;");

                            Label content = new Label(softWrapLongTokens(thread.getContent(), 40));
                            content.setWrapText(true);
                            content.setStyle("-fx-text-fill:#374151;");

                            // IMPORTANT: prevent it from growing the HBox width
                            content.setMinWidth(0);
                            content.maxWidthProperty().bind(card.widthProperty().subtract(20));

                            card.getChildren().addAll(author, title, content);


                            card.setOnMouseClicked(e -> openThreadPage(thread));


                            // ✅ ONLY SHOW BUTTONS IF USER IS OWNER
                            if (thread.getUserId() == currentUserId) {

                                Button editBtn = new Button("✏ Edit");
                                editBtn.setStyle(
                                        "-fx-background-color:#3b82f6;" +
                                                "-fx-text-fill:white;" +
                                                "-fx-background-radius:8;"
                                );

                                Button deleteBtn = new Button("🗑 Delete");
                                deleteBtn.setStyle(
                                        "-fx-background-color:#ef4444;" +
                                                "-fx-text-fill:white;" +
                                                "-fx-background-radius:8;"
                                );


                                editBtn.setOnAction(e -> openEditThread(thread));
                                deleteBtn.setOnAction(e -> deleteThread(thread.getId()));
                                deleteBtn.setOnMouseEntered(e ->
                                        deleteBtn.setStyle(deleteBtn.getStyle() + "-fx-opacity:0.85;")
                                );

                                deleteBtn.setOnMouseExited(e ->
                                        deleteBtn.setStyle(deleteBtn.getStyle().replace("-fx-opacity:0.85;", ""))
                                );
                                editBtn.setOnMouseEntered(e ->
                                        editBtn.setStyle(editBtn.getStyle() + "-fx-opacity:0.85;")
                                );

                                editBtn.setOnMouseExited(e ->
                                        editBtn.setStyle(editBtn.getStyle().replace("-fx-opacity:0.85;", ""))
                                );
                                HBox actions = new HBox(10, editBtn, deleteBtn);
                                actions.setStyle("-fx-padding: 10 0 0 0;");
                                card.getChildren().add(actions);
                            }

                            threadsContainer.getChildren().add(card);
                        });

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        private void openThreadPage(Thread thread) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ThreadPage.fxml"));
                Parent root = loader.load();

                ThreadPageController controller = loader.getController();
                controller.setThread(thread);
                controller.setGroup(group);

                groupName.getScene().setRoot(root);

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        @FXML
        private void addThread() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddThread.fxml"));
                Parent root = loader.load();

                ThreadAddController controller = loader.getController();
                controller.setGroup(group);               // ADD THIS
                controller.setGroupId(group.getId());

                groupName.getScene().setRoot(root);

            } catch (Exception e) {
                System.out.println(e.getMessage());

            }
        }

        @FXML
        private void goBack() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupsPage.fxml"));
                Parent root = loader.load();
                groupName.getScene().setRoot(root);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        @FXML
        private void leaveGroup() {

            // 🔴 IF ADMIN
            if (isAdmin()) {

                if (!confirmAdminLeave())
                    return;

                GroupCRUD groupCRUD = new GroupCRUD();

                try {
                    groupCRUD.supprimer(group.getId());
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }

                goBack(); // return to homepage
                return;
            }

            // 🟢 NORMAL MEMBER
            if (!confirmAction("Leave Group",
                    "Are you sure you want to leave this group?"))
                return;

            GroupMemberCRUD gmCrud = new GroupMemberCRUD();

            try {
                gmCrud.deleteMembership(group.getId(), currentUserId);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            checkMembership();
        }


        private void loadMembers() {

            membersContainer.getChildren().clear();

            GroupMemberCRUD gmCrud = new GroupMemberCRUD();

            try {

                List<GroupMember> members = gmCrud.getMembersByGroup(group.getId());

                memberCountLabel.setText("Members: " + members.size());

                for (GroupMember member : members) {

                    HBox row = new HBox(15);
                    row.setStyle(
                            "-fx-padding:8;" +
                                    "-fx-background-color:white;" +
                                    "-fx-background-radius:6;"
                    );

                    Label name = new Label(member.getFirstname());

                    name.setStyle("-fx-font-size: 14px;");

                    // 🔥 ROLE LABEL
                    Label roleLabel = new Label();

                    if (member.getUserId() == group.getGroupAdminId()) {
                        roleLabel.setText("Admin");
                        roleLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        roleLabel.setText("Member");
                        roleLabel.setStyle("-fx-text-fill: gray;");
                    }

                    row.getChildren().addAll(name, roleLabel);

                    // ✅ ADMIN LOGIC (Kick button)
                    if (isAdmin() && member.getUserId() != currentUserId) {

                        Button kickBtn = new Button("Kick");
                        kickBtn.setStyle("-fx-background-color:#e53935; -fx-text-fill:white;");
                        kickBtn.setMinWidth(70);
                        kickBtn.setPrefWidth(70);
                        kickBtn.setOnAction(e -> kickMember(member.getUserId()));

                        row.getChildren().add(kickBtn);
                    }

                    membersContainer.getChildren().add(row);
                }


            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        private void kickMember(int userId) {

            if (!confirmAction("Kick Member",
                    "Are you sure you want to remove this member?"))
                return;

            GroupMemberCRUD gmCrud = new GroupMemberCRUD();

            try {
                gmCrud.deleteMembership(group.getId(), userId);
                loadMembers();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        private boolean confirmAction(String title, String message) {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            ButtonType yesBtn = new ButtonType("Yes");
            ButtonType noBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(yesBtn, noBtn);

            return alert.showAndWait().orElse(noBtn) == yesBtn;
        }
        private boolean confirmAdminLeave() {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Delete Group");
            alert.setHeaderText("You are the ADMIN of this group!");
            alert.setContentText(
                    "If you leave, the entire group will be permanently deleted.\n\n" +
                            "All threads, comments, and members will be removed.\n\n" +
                            "This action CANNOT be undone.\n\n" +
                            "Do you want to continue?"
            );

            ButtonType deleteBtn = new ButtonType("Delete Group");
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(deleteBtn, cancelBtn);

            return alert.showAndWait().orElse(cancelBtn) == deleteBtn;
        }









    }
