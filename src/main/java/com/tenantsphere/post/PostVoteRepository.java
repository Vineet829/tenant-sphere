package com.tenantsphere.post;

import com.tenantsphere.post.dto.PostVoteResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** Vote writes share a post row lock and never change the post's edit timestamp. */
@Repository
public class PostVoteRepository {

    private final JdbcTemplate database;

    public PostVoteRepository(JdbcTemplate database) {
        this.database = database;
    }

    public long lockPost(UUID id) {
        return database.query("SELECT pkid FROM posts WHERE id = ? FOR UPDATE",
                (row, index) -> row.getLong("pkid"), id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
    }

    public int currentVote(long post, long user) {
        return database.queryForObject("""
                SELECT CASE
                    WHEN EXISTS (SELECT 1 FROM post_upvotes WHERE post_pkid = ? AND user_pkid = ?) THEN 1
                    WHEN EXISTS (SELECT 1 FROM post_downvotes WHERE post_pkid = ? AND user_pkid = ?) THEN -1
                    ELSE 0 END
                """, Integer.class, post, user, post, user);
    }

    public PostVoteResponse setVote(long post, long user, int vote, String message) {
        if (vote != 1) {
            database.update("DELETE FROM post_upvotes WHERE post_pkid = ? AND user_pkid = ?", post, user);
        }
        if (vote != -1) {
            database.update("DELETE FROM post_downvotes WHERE post_pkid = ? AND user_pkid = ?", post, user);
        }
        if (vote == 1) {
            database.update("INSERT INTO post_upvotes (post_pkid, user_pkid) VALUES (?, ?) ON CONFLICT DO NOTHING", post, user);
        } else if (vote == -1) {
            database.update("INSERT INTO post_downvotes (post_pkid, user_pkid) VALUES (?, ?) ON CONFLICT DO NOTHING", post, user);
        }
        database.update("""
                UPDATE posts SET
                    upvotes = (SELECT count(*) FROM post_upvotes WHERE post_pkid = ?),
                    downvotes = (SELECT count(*) FROM post_downvotes WHERE post_pkid = ?)
                WHERE pkid = ?
                """, post, post, post);
        return database.queryForObject("SELECT id, slug, upvotes, downvotes FROM posts WHERE pkid = ?",
                (row, index) -> new PostVoteResponse(
                        row.getObject("id", UUID.class), row.getString("slug"),
                        row.getInt("upvotes"), row.getInt("downvotes"), vote == 1, vote == -1, message), post);
    }
}
