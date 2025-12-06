package validation;

import database.Database;
import database.PostTable;
import service.PostService;
import post.Post;

import java.util.List;

public class TestingAutomation {

	public static void main(String[] args) {
        System.out.println("Database connecting...");
        Database db = new Database();
        db.connect();
        db.createTables();
        System.out.println("Tables created or verified.");
        System.out.println("\n=== HW2 Semi–Auto Test Suite ===");

        PostValidator validator = new PostValidator();
        PostTable postTable = new PostTable(db);
        PostService postService = new PostService(validator, postTable);

        // ---------- TEST 1: Create Post ----------
        System.out.println("\nTest 1: Create Valid Post");
        Post post = postService.createPost("Isaac", "First Post", "Hello HW2!", "General");
        int postId = post.getId();
        System.out.println("***PASS*** Post created successfully (ID " + postId + ")");

        // ---------- TEST 2: Validation Tests ----------
        System.out.println("\nTest 2: Validation (Invalid Inputs)");

        testValidation(validator, new Post("", "Valid Title", "Valid Body", "General"), "Empty Author");
        testValidation(validator, new Post("Isaac", "", "Valid Body", "General"), "Empty Title");
        testValidation(validator, new Post("Isaac", "Title", "", "General"), "Empty Body");

        // ---------- TEST 3: Update Post ----------
        System.out.println("\nTest 3: Update Post Body");
        boolean updated = postService.updatePost(postId, "First Post", "Updated content", "Isaac", "General");
        System.out.println(updated ? "***PASS*** Post updated successfully" : "***FAIL*** Update failed");

        // ---------- TEST 4: Delete Post ----------
        System.out.println("\nTest 4: Delete Post");
        boolean deleted = postService.deletePost(postId);
        System.out.println(deleted ? "***PASS*** Post deleted successfully" : "***FAIL*** Deletion failed");

        // ---------- TEST 5: Visual Cues ----------
        System.out.println("\nTest 5: Visual Check for Deleted Post");
        var deletedPost = postTable.getById(postId);
        if (deletedPost != null && deletedPost.isDeleted()) {
            System.out.println("***PASS*** Deleted post remains visible with '[Deleted Post]' label");
        } else {
            System.out.println("***FAIL*** Deleted post missing or label not set");
        }

        System.out.println("\n=== Testing Complete ===");
    }

    private static void testValidation(PostValidator validator, Post post, String caseName) {
        System.out.println("\nSub-Test: " + caseName);
        List<String> errors = validator.validate(post);

        if (errors.isEmpty()) {
            System.out.println("***FAIL*** Expected validation errors not found.");
        } else {
            System.out.println("***PASS*** Validation errors caught:");
            for (String err : errors) {
                System.out.println(" - " + err);
            }
        }
    }
}