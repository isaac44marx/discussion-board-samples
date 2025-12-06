package post;


/**
 * Represents a discussion board post.
 *
 * <p>Each post contains an author, title, body text, category,
 * and an internal flag to indicate whether it has been deleted.
 * Deleted posts remain visible in the system but display
 * placeholder text and cannot be edited.</p>
 *
 * <p>This class serves as a plain data model used by
 * {@code PostTable}, {@code PostService}, and the GUI layer.</p>
 */
public class Post {

    private int id;
    private String author;
    private String title;
    private String body;
    private String category;
    private boolean deleted;

    /**
     * Constructs a post with the default category "General".
     *
     * @param author the name of the user who authored the post
     * @param title the post title
     * @param body the main text of the post
     */
    public Post(String author, String title, String body) {
        this(author, title, body, "General");
    }

    /**
     * Constructs a post with a specified category.
     *
     * @param author the name of the user who authored the post
     * @param title the post title
     * @param body the main text of the post
     * @param category the category under which this post is classified
     */
    public Post(String author, String title, String body, String category) {
        this.author = author;
        this.title = title;
        this.body = body;
        this.category = (category == null || category.isBlank()) ? "General" : category;
    }
    
    // ----- Getters and Setters -----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public void setAuthor(String author) { this.author = author; }

    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    @Override
    public String toString() {
        return String.format("[%d] %s: %s", id, author, title);
    }
}
