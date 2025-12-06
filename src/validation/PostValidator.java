package validation;

import post.Post;
import java.util.ArrayList;
import java.util.List;

public class PostValidator {
    // Author: 2–25 letters/spaces/hyphens/apostrophes
    private static final String AUTHOR_RE = "^[A-Za-z][A-Za-z '\\-]{1,24}$";

    public List<String> validate(Post p) {
        List<String> errs = new ArrayList<>();
        if (p == null) { errs.add("Post is required."); return errs; }

        if (p.getAuthor() == null || p.getAuthor().isBlank())
            errs.add("Author is required.");
        else if (!p.getAuthor().matches(AUTHOR_RE))
            errs.add("Author must be 2–25 letters/spaces/hyphens/apostrophes and start with a letter.");

        if (p.getTitle() == null || p.getTitle().isBlank())
            errs.add("Title is required.");
        else if (p.getTitle().length() < 2 || p.getTitle().length() > 50)
            errs.add("Title must be 2–50 characters.");

        if (p.getBody() == null || p.getBody().isBlank())
            errs.add("Body is required.");
        else if (p.getBody().length() > 350)
            errs.add("Body must be ≤ 350 characters.");

        return errs;
    }
}
