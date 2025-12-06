package service;

import validation.PostValidator;
import database.PostTable;
import java.util.List;
import java.util.ArrayList;
import post.Post;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PostService {
    private final PostValidator validator;
    private final PostTable table;

    private final List<String> lastErrors = new ArrayList<>();
    public List<String> getErrors() { return List.copyOf(lastErrors); }
    private void clearErrors() { lastErrors.clear(); }

    // constructor
    public PostService(PostValidator validator, PostTable table) {
        this.validator = validator; this.table = table;
    }

    /** CREATE: returns created Post or null; errors via getErrors() */
    public Post createPost(String author, String title, String body) {
        clearErrors();
        Post post = new Post(author, title, body);
        var errs = validator.validate(post);
        if (!errs.isEmpty()) { lastErrors.addAll(errs); return null; }
        try {
            table.insert(post);
            return post;
        } catch (Exception e) {
            lastErrors.add("Failed to create post: " + e.getMessage());
            return null;
        }
    }
    // overload createPost
    public Post createPost(String author, String title, String body, String category) {
        clearErrors();
        Post post = new Post(author, title, body, category);
        var errs = validator.validate(post);
        if (!errs.isEmpty()) { lastErrors.addAll(errs); return null; }
        try { table.insert(post); return post; }
        catch (Exception e) { lastErrors.add("Failed to create post: " + e.getMessage()); return null; }
    }
    // overload passes object
    public Post createPost(Post p) {
        clearErrors();
        var errs = validator.validate(p);
        if (!errs.isEmpty()) { lastErrors.addAll(errs); return null; }
        try {
            table.insert(p);
            return p;
        } catch (Exception e) {
            lastErrors.add("Failed to create post: " + e.getMessage());
            return null;
        }
    }

    /** READ */
    public List<Post> getAllPosts() {
        clearErrors();
        try { return table.getAll(); }
        catch (Exception e) { lastErrors.add("Failed to load posts: " + e.getMessage()); return List.of(); }
    }

    public Post getPostById(int id) {
        clearErrors();
        try {
            Post p = table.getById(id);
            if (p == null) lastErrors.add("Post not found: id=" + id);
            return p;
        } catch (Exception e) {
            lastErrors.add("Failed to load post: " + e.getMessage());
            return null;
        }
    }
    
    /** SEARCH: find posts whose title or body contains the query (case-insensitive) */
    public List<Post> searchPosts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return table.getAll();
        }

        String q = query.toLowerCase();
        return table.getAll().stream()
                .filter(p -> p.getTitle().toLowerCase().contains(q) ||
                             p.getBody().toLowerCase().contains(q) ||
                             (p.getAuthor() != null && p.getAuthor().toLowerCase().contains(q)))
                .sorted(Comparator.comparing(Post::getId).reversed())
                .collect(Collectors.toList());
    }

    /** UPDATE: true on success; errors via getErrors() */
    public boolean updatePost(int id, String newTitle, String newBody, String newAuthor, String newCategory) {
        clearErrors();
        try {
            Post existing = table.getById(id);
            if (existing == null) {
                lastErrors.add("Post not found: id=" + id);
                return false;
            }

            if (newTitle != null) existing.setTitle(newTitle);
            if (newBody != null) existing.setBody(newBody);
            if (newAuthor != null) existing.setAuthor(newAuthor);  // Use setter instead of recreating object
            if (newCategory != null) existing.setCategory(newCategory);

            // Validate the modified post
            var errs = validator.validate(existing);
            if (!errs.isEmpty()) {
                lastErrors.addAll(errs);
                return false;
            }

            // Write to DB
            table.update(existing);
            return true;

        } catch (Exception e) {
            lastErrors.add("Failed to update post: " + e.getMessage());
            return false;
        }
    }

    /** DELETE: (do NOT delete replies) */
    public boolean deletePost(int id) {
        clearErrors();
        try {
            Post existing = table.getById(id);
            if (existing == null) { lastErrors.add("Post not found: id=" + id); return false; }
            table.delete(id);
            return true;
        } catch (Exception e) {
            lastErrors.add("Failed to delete post: " + e.getMessage());
            return false;
        }
    }
}
