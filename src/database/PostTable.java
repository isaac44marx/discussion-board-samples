package database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import post.Post;


/**
 * Handles all direct database interactions for Posts.
 * 
 * <p>This class performs CRUD (Create, Read, Update, Delete) operations 
 * on the Posts table using prepared SQL statements. It serves as the 
 * data-access layer between the {@code PostService} and the database.</p>
 *
 * <p>Each method encapsulates a specific SQL query and shields the 
 * rest of the program from low-level database logic.</p>
 */
public class PostTable {

    private final Database db;

    /**
     * Constructor for PostTable.
     * @param db The shared Database instance containing the active connection.
     */
    public PostTable(Database db) {
        this.db = db;
    }

    /**
     * Inserts a new post into the Posts table.
     * 
     * <p>Automatically sets the creation timestamp. After insertion, 
     * retrieves the generated ID and assigns it to the Post object.</p>
     * 
     * @param post the Post object containing author, title, body, and category
     */
    public void insert(Post post) {
        String sql = "INSERT INTO Posts (author, title, body, category, createdAt) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getAuthor());
            ps.setString(2, post.getTitle());
            ps.setString(3, post.getBody());
            ps.setString(4, post.getCategory());
            ps.executeUpdate();

            // Retrieve generated ID and assign to the post
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    post.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all posts from the database.
     * 
     * <p>Returns posts in reverse chronological order (newest first). 
     * Includes deleted posts so the GUI can display "[Deleted Post]" entries.</p>
     * 
     * @return a List of Post objects representing all posts in the table
     */
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM Posts ORDER BY createdAt DESC";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
            	Post p = new Post(
            	    rs.getString("author"),
            	    rs.getString("title"),
            	    rs.getString("body"),
            	    rs.getString("category")                        
            	);
            	p.setId(rs.getInt("id"));
            	p.setDeleted(rs.getBoolean("deleted"));
                posts.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    /**
     * Retrieves a single Post object by its ID.
     * 
     * @param id the post ID to search for
     * @return the matching Post object, or {@code null} if no post is found
     */
    public Post getById(int id) {
        String sql = "SELECT * FROM Posts WHERE id = ?";
        try (PreparedStatement pstmt = db.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
            	Post p = new Post(
            	    rs.getString("author"),
            	    rs.getString("title"),
            	    rs.getString("body"),
            	    rs.getString("category")                        
            	);
            	p.setId(rs.getInt("id"));
            	p.setDeleted(rs.getBoolean("deleted"));
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing post's title, body, author, and category.
     * 
     * <p>Uses a prepared statement to ensure safety from SQL injection. 
     * Only updates the record matching the post's ID.</p>
     * 
     * @param post the modified Post object to update in the database
     */
    public void update(Post post) {
    	String up = "UPDATE Posts SET title=?, body=?, author=?, category=? WHERE id=?";
    	try (PreparedStatement ps = db.getConnection().prepareStatement(up)) {
    	    ps.setString(1, post.getTitle());
    	    ps.setString(2, post.getBody());
    	    ps.setString(3, post.getAuthor());
    	    ps.setString(4, post.getCategory());            
    	    ps.setInt(5, post.getId());
    	    ps.executeUpdate();
    	} catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs a soft delete on a post by marking it as deleted instead of removing it.
     * 
     * <p>Replaces the title and body text to indicate deletion while preserving 
     * replies and post structure for discussion context.</p>
     * 
     * @param postId the ID of the post to mark as deleted
     */
    public void delete(int postId) {
        String sql = "UPDATE Posts SET deleted = TRUE, title = '[Deleted Post]', body = '[This post has been deleted by the author]' WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
