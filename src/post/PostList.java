package post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import service.PostService; 


/**
 * Maintains in-memory collections of {@link Post} objects and supports
 * filtering and keyword-based searches.
 *
 * <p>This class acts as a lightweight data model wrapper for the GUI layer.
 * It mirrors the posts managed by {@link PostService} and allows the user
 * interface to perform local filtering, searching, and refreshing without
 * repeatedly querying the database.</p>
 *
 * <p>Two lists are maintained internally:
 * <ul>
 *   <li>{@code allPosts} — the complete set of posts loaded from persistence</li>
 *   <li>{@code subset} — a temporary working list for filters or search results</li>
 * </ul>
 * The subset is cleared and rebuilt whenever a filter or search is executed.</p>
 */
public class PostList {

    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> subset   = new ArrayList<>();

    private final PostService postService;

    /**
     * Constructs a PostList tied to the given PostService.
     * @param postService the service used to load posts from persistence
     */
    public PostList(PostService postService) {
        this.postService = postService;
    }

    /**
     * Reloads all posts from the database and clears the current subset.
     * This should be called after any create, update, or delete operation
     * to ensure the in-memory list is synchronized with the database.
     */
    public void refresh() {
        allPosts.clear();
        allPosts.addAll(postService.getAllPosts());
        subset.clear();
    }

    /**
     * Manually adds a post to the in-memory list.
     * Typically used in testing or immediately after a successful create.
     * @param p the Post to add
     */
    public void addPost(Post p) {
        allPosts.add(p);
    }

    /** @return an unmodifiable view of all posts */
    public List<Post> getAll() {
        return Collections.unmodifiableList(allPosts);
    }

    /** @return an unmodifiable view of the current filtered subset */
    public List<Post> getSubset() {
        return Collections.unmodifiableList(subset);
    }

    /** Clears the current filtered subset. */
    public void clearSubset() {
        subset.clear();
    }

    /**
     * Filters posts by author name (case-insensitive exact match).
     * Matching posts are stored in {@code subset}.
     * @param author the author name to match
     */
    public void filterByAuthor(String author) {
        subset.clear();
        if (author == null) return;
        String target = author.toLowerCase(Locale.ROOT).trim();
        for (Post p : allPosts) {
            if (p.getAuthor() != null &&
                p.getAuthor().toLowerCase(Locale.ROOT).equals(target)) {
                subset.add(p);
            }
        }
    }

    /**
     * Searches posts by keyword in either the title or body (case-insensitive contains).
     * Matching posts are stored in {@code subset}.
     * @param keyword the search keyword
     */
    public void searchByKeyword(String keyword) {
        subset.clear();
        if (keyword == null || keyword.isBlank()) return;
        String q = keyword.toLowerCase(Locale.ROOT);
        for (Post p : allPosts) {
            String title = p.getTitle() == null ? "" : p.getTitle().toLowerCase(Locale.ROOT);
            String body  = p.getBody()  == null ? "" : p.getBody().toLowerCase(Locale.ROOT);
            if (title.contains(q) || body.contains(q)) {
                subset.add(p);
            }
        }
    }
}
