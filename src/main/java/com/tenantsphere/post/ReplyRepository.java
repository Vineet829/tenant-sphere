package com.tenantsphere.post;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    @EntityGraph(attributePaths = {"author", "post"})
    List<Reply> findByPostPkidInOrderByCreatedAtAsc(List<Long> postPkids);

    @EntityGraph(attributePaths = {"author", "post"})
    List<Reply> findByPostPkidOrderByCreatedAtDesc(Long postPkid);

    @EntityGraph(attributePaths = {"author", "post"})
    org.springframework.data.domain.Page<Reply> findByPostPkid(Long postPkid, org.springframework.data.domain.Pageable pageable);

    long countByPostPkid(Long postPkid);

    @Query("""
            select r.post.pkid as postPkid, count(r) as total
            from Reply r where r.post.pkid in :postPkids
            group by r.post.pkid
            """)
    List<ReplyCount> countByPosts(@Param("postPkids") List<Long> postPkids);

    interface ReplyCount {
        Long getPostPkid();
        Long getTotal();
    }
}
