package guiCRUD;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import post.Post;
import reply.Reply;
import database.Database;
import database.PostTable;
import database.ReplyTable;
import validation.PostValidator;
import validation.ReplyValidator;
import service.PostService;
import service.ReplyService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Main JavaFX GUI class for the Discussion Board CRUD application.
 * 
 * <p>This class builds and manages the graphical user interface (GUI)
 * for browsing, creating, editing, deleting, and replying to posts.
 * It coordinates between the presentation layer (JavaFX controls)
 * and the service layer ({@code PostService}, {@code ReplyService}).</p>
 *
 * <p>The interface is divided into three vertical panels:
 * <ul>
 *   <li>Left: category selection and thread creation</li>
 *   <li>Center: list of posts with search functionality</li>
 *   <li>Right: post details and nested replies</li>
 * </ul>
 * 
 * Each section is built modularly using helper methods for clarity.</p>
 */
public class CrudApp extends Application {

    private TableView<Post> postTable = new TableView<>();
    private TextArea postBodyArea = new TextArea();
    private VBox replyContainer = new VBox(8);
    private TextField searchField = new TextField();

    private PostService postService;
    private ReplyService replyService;

    private String selectedCategory = "General";
    private String currentAuthor = "Anonymous";
    
    
    /**
     * Application entry point: initializes the database,
     * builds the UI layout, and loads initial data.
     */
    @Override
    public void start(Stage stage) {
        // ---------- Database + Services ----------
        Database db = new Database();
        db.connect();
        db.createTables();

        postService = new PostService(new PostValidator(), new PostTable(db));
        replyService = new ReplyService(new ReplyValidator(), new ReplyTable(db));

        // ---------- Layout ----------
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox categoryPane = createCategoryPane();
        VBox postListPane = createPostListPane();
        VBox detailPane = createDetailPane();

        root.setLeft(categoryPane);
        root.setCenter(postListPane);
        root.setRight(detailPane);

        // ---------- Scene Setup ----------
        Scene scene = new Scene(root, 1250, 600);
        stage.setScene(scene);
        stage.setTitle("Discussion Board");
        stage.show();

        loadPostsForCategory(selectedCategory);
    }

 // -------------------------------------------------------------------------
    // LEFT COLUMN: Categories + "Create Thread"
    // -------------------------------------------------------------------------

    /**
     * Builds the left-side category pane.
     * 
     * <p>Contains category buttons, a "Create Thread" button,
     * and an option to set the current author.</p>
     */
    private VBox createCategoryPane() {
        VBox categories = new VBox(10);
        categories.setPadding(new Insets(10));

        Label label = new Label("CATEGORIES");
        label.setStyle("-fx-font-weight: bold; -fx-underline: true;");

        Button general = new Button("General");
        Button lectures = new Button("Lectures");
        Button assignments = new Button("Assignments");
        Button social = new Button("Social");

        Button createThread = new Button("Create Thread");
        createThread.setMaxWidth(Double.MAX_VALUE);
        
        Button setAuthorBtn = new Button("Set Author");
        Label currentAuthorLabel = new Label("Current: " + currentAuthor);
        currentAuthorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
        
        // Allow user to switch displayed author name
        setAuthorBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(currentAuthor);
            dialog.setHeaderText("Enter author name:");
            dialog.setContentText("Name:");
            
            dialog.showAndWait().ifPresent(name -> {
                currentAuthor = name.trim();
                currentAuthorLabel.setText("Current: " + currentAuthor);
            });
        });    
        

        categories.getChildren().addAll(label, general, lectures, assignments, social, new Separator(), createThread, setAuthorBtn, currentAuthorLabel);
        categories.setPrefWidth(180);
        
        // Category filtering
        general.setOnAction(e -> loadPostsForCategory("General"));
        lectures.setOnAction(e -> loadPostsForCategory("Lectures"));
        assignments.setOnAction(e -> loadPostsForCategory("Assignments"));
        social.setOnAction(e -> loadPostsForCategory("Social"));

        createThread.setOnAction(e -> openCreateThreadDialog());

        return categories;
    }

    // -------------------------------------------------------------------------
    // CENTER COLUMN: Post List + Search Bar
    // -------------------------------------------------------------------------

    /**
     * Builds the center pane containing the post list and search field.
     */
    private VBox createPostListPane() {
        VBox postListPane = new VBox(10);
        postListPane.setPadding(new Insets(10));

        searchField.setPromptText("Search posts...");
        setupPostColumns();
        // Filter posts live as user types
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.isEmpty()) loadPostsForCategory(selectedCategory);
            else searchPosts(newText);
        });

        postListPane.getChildren().addAll(searchField, postTable);
        VBox.setVgrow(postTable, Priority.ALWAYS);
        return postListPane;
    }

    // -------------------------------------------------------------------------
    // RIGHT COLUMN: Post Details + Replies
    // -------------------------------------------------------------------------
    /**
     * Builds the right-side detail pane where a selected post’s content
     * and its replies are displayed.
     */
    private VBox createDetailPane() {
        VBox details = new VBox(10);
        details.setPadding(new Insets(10));

        Label titleLabel = new Label("Post Title");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label authorLabel = new Label();
        postBodyArea.setEditable(false);
        postBodyArea.setWrapText(true);
        postBodyArea.setPrefHeight(200);

        Label replyLabel = new Label("Replies");
        replyLabel.setStyle("-fx-font-weight: bold;");

        // Scrollable area for nested replies
        ScrollPane replyScroll = new ScrollPane(replyContainer);
        replyScroll.setFitToWidth(true);
        replyScroll.setPrefHeight(300);
        replyScroll.setStyle("-fx-background-color: #f8f8f8;");

        details.getChildren().addAll(titleLabel, authorLabel, postBodyArea, replyLabel, replyScroll);
        VBox.setVgrow(replyScroll, Priority.ALWAYS);
        // Load post details when clicked in table
        postTable.setOnMouseClicked(e -> {
            Post selected = postTable.getSelectionModel().getSelectedItem();
            if (selected != null) showPostDetails(selected, titleLabel, authorLabel);
        });

        return details;
    }

    // -------------------------------------------------------------------------
    // BEHAVIOR: Load, Search, and Display
    // -------------------------------------------------------------------------
    /**
     * Loads all posts for the selected category and refreshes the table.
     */
    private void loadPostsForCategory(String category) {
        selectedCategory = category;
        List<Post> posts = postService.getAllPosts().stream()
                .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                .sorted(Comparator.comparing(Post::getId).reversed())
                .collect(Collectors.toList());
        postTable.getItems().setAll(posts);
    }
    /**
     * Searches posts by keyword using PostService’s search function.
     */
    private void searchPosts(String query) {
        List<Post> results = postService.searchPosts(query);
        postTable.getItems().setAll(results);
    }
    /**
     * Displays a post’s details and dynamically attaches edit/delete/reply buttons.
     */
    private void showPostDetails(Post post, Label titleLabel, Label authorLabel) {
        titleLabel.setText(post.getTitle());
        authorLabel.setText("by " + post.getAuthor());
        postBodyArea.setText(post.getBody());

        // Remove any existing action button sets
        if (postBodyArea.getParent() instanceof VBox details) {
            details.getChildren().removeIf(node -> node.getUserData() != null && node.getUserData().equals("postActions"));

            HBox postActions = new HBox(10);
            postActions.setUserData("postActions"); 
            postActions.setPadding(new Insets(5, 0, 5, 0));

            Button replyBtn = new Button("Reply");
            postActions.getChildren().add(replyBtn);
            
            // Visual cue for deleted posts
            if (post.isDeleted()) {
                titleLabel.setText("[Deleted Post]");
                authorLabel.setText("by [deleted]");
                postBodyArea.setText("[This post has been deleted by the author]");
            }
            
            // Author-only edit/delete controls
            if (post.getAuthor().equals(currentAuthor)) {
                Button editBtn = new Button("Edit");
                Button deleteBtn = new Button("Delete");

                editBtn.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(post.getBody());
                    dialog.setHeaderText("Edit Post Body");
                    dialog.showAndWait().ifPresent(newBody -> {
                        postService.updatePost(
                            post.getId(),
                            post.getTitle(),
                            newBody,
                            post.getAuthor(),
                            post.getCategory()
                        );
                        loadPostsForCategory(selectedCategory);
                        showPostDetails(post, titleLabel, authorLabel);
                    });
                });

                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete this post?");
                    confirm.setContentText("Are you sure you want to delete this post? Replies will remain visible.");
                    
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            postService.deletePost(post.getId());
                            loadPostsForCategory(selectedCategory);
                        }
                    });
                });

                postActions.getChildren().addAll(editBtn, deleteBtn);
            }

            replyBtn.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setHeaderText("Reply to post");
                dialog.showAndWait().ifPresent(text -> {
                    replyService.createReply(post.getId(), currentAuthor, text, null);
                    renderReplies(post);
                });
            });

            int replyIndex = details.getChildren().indexOf(replyContainer);
            if (replyIndex >= 0) {
                details.getChildren().add(replyIndex, postActions);
            } else {
                details.getChildren().add(postActions);
            }
        }

        renderReplies(post);
    }
    /**
     * Clears and re-renders the nested replies for a post.
     */
    private void renderReplies(Post post) {
        replyContainer.getChildren().clear();

        List<Reply> allReplies = replyService.getRepliesForPost(post.getId());

        // Recursively render replies
        renderRepliesRecursive(allReplies, null, 0);
    }
    /**
     * Recursively displays replies with indentation to represent nesting depth.
     */
    private void renderRepliesRecursive(List<Reply> allReplies, Integer parentId, int depth) {
        List<Reply> children = allReplies.stream()
                .filter(r -> (r.getParentId() == null && parentId == null) ||
                             (r.getParentId() != null && r.getParentId().equals(parentId)))
                .collect(Collectors.toList());

        for (Reply r : children) {
            VBox box = new VBox(3);
            box.setPadding(new Insets(5, 5, 5, 20 * depth));
            box.setStyle("-fx-background-color: #f6f6f6; -fx-border-color: #ccc; -fx-border-radius: 4;");

            Label author = new Label(r.getAuthor());
            author.setStyle("-fx-font-weight: bold;");
            Label body = new Label(r.getBody());

            HBox actions = new HBox(5);
            Button replyBtn = new Button("Reply");
            actions.getChildren().add(replyBtn);
            
            // Only allow edit/delete for the reply’s author
            if (r.getAuthor().equals(currentAuthor)) {
                Button editBtn = new Button("Edit");
                Button deleteBtn = new Button("Delete");
                actions.getChildren().addAll(editBtn, deleteBtn);

                editBtn.setOnAction(e -> {
                    TextInputDialog d = new TextInputDialog(r.getBody());
                    d.setHeaderText("Edit Reply");
                    d.showAndWait().ifPresent(newBody -> {
                        replyService.updateReply(r.getId(), r.getPostId(), newBody);
                        renderReplies(postTable.getSelectionModel().getSelectedItem());
                    });
                });

                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete this reply?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            replyService.deleteReply(r.getId());
                            renderReplies(postTable.getSelectionModel().getSelectedItem());
                        }
                    });
                });
            }

            replyBtn.setOnAction(e -> {
                TextInputDialog d = new TextInputDialog();
                d.setHeaderText("Reply to " + r.getAuthor());
                d.showAndWait().ifPresent(text -> {
                    replyService.createReply(r.getPostId(), currentAuthor, text, r.getId());
                    renderReplies(postTable.getSelectionModel().getSelectedItem());
                });
            });

            box.getChildren().addAll(author, body, actions);
            replyContainer.getChildren().add(box);

            // Recursive call for this reply's children
            renderRepliesRecursive(allReplies, r.getId(), depth + 1);
        }
    }

    // -------------------------------------------------------------------------
    // CREATE THREAD DIALOG
    // -------------------------------------------------------------------------

    /**
     * Opens a modal dialog for creating a new discussion thread.
     */
    private void openCreateThreadDialog() {
        Dialog<Post> dialog = new Dialog<>();
        dialog.setTitle("Create New Thread");

        Label titleLabel = new Label("Title:");
        TextField titleField = new TextField();

        Label bodyLabel = new Label("Body:");
        TextArea bodyArea = new TextArea();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(titleLabel, 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(bodyLabel, 0, 1);
        grid.add(bodyArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new Post(currentAuthor, titleField.getText(), bodyArea.getText(), selectedCategory);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(post -> {
            postService.createPost(post);
            loadPostsForCategory(selectedCategory);
        });
    }

    // -------------------------------------------------------------------------
    // TABLE CONFIGURATION
    // -------------------------------------------------------------------------

    /**
     * Defines table columns for the post list, including logic to
     * display placeholders for deleted posts.
     */
    private void setupPostColumns() {
        postTable.getColumns().clear();
        TableColumn<Post, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(cellData -> {
            Post p = cellData.getValue();
            String author = p.isDeleted() ? "[deleted]" : p.getAuthor();
            return new SimpleStringProperty(author);
        });

        TableColumn<Post, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cellData -> {
            Post p = cellData.getValue();
            String title = p.isDeleted() ? "[Deleted Post]" : p.getTitle();
            return new SimpleStringProperty(title);
        });

        postTable.getColumns().addAll(authorCol, titleCol);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
