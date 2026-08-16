package com.tenantsphere.post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("select p from Post p order by p.upvotes desc, p.createdAt desc")
    Page<Post> findAllOrdered(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    List<Post> findAllBy();

    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("select p from Post p where p.author.pkid = :authorPkid order by p.upvotes desc, p.createdAt desc")
    Page<Post> findByAuthor(@Param("authorPkid") Long authorPkid, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("""
            select p from Post p join p.bookmarkedBy u
            where u.pkid = :userPkid
            order by p.createdAt desc, p.updatedAt desc
            """)
    Page<Post> findBookmarkedBy(@Param("userPkid") Long userPkid, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("""
            select p from Post p join p.tags t
            where t.slug = :tagSlug
            order by p.pkid asc
            """)
    Page<Post> findByTagSlug(@Param("tagSlug") String tagSlug, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    Optional<Post> findBySlug(String slug);

    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("select p from Post p where p.id = :externalId")
    Optional<Post> findByExternalId(@Param("externalId") UUID externalId);

    @Query("select p.pkid from Post p join p.bookmarkedBy u where u.pkid = :userPkid and p.pkid in :postPkids")
    List<Long> findBookmarkedPkids(
            @Param("userPkid") Long userPkid, @Param("postPkids") List<Long> postPkids);

    @Query("select p.pkid from Post p join p.upvotedBy u where u.pkid = :userPkid and p.pkid in :postPkids")
    List<Long> findUpvotedPkids(
            @Param("userPkid") Long userPkid, @Param("postPkids") List<Long> postPkids);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
            select p from Post p
            order by p.upvotes desc, p.createdAt desc
            """)
    List<Post> findTopPosts(Pageable pageable);
}
