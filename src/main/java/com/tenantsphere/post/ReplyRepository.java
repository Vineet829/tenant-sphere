package com.tenantsphere.post;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    @EntityGraph(attributePaths = {"author", "post"})
    List<Reply> findByPostPkidInOrderByCreatedAtAsc(List<Long> postPkids);

    @EntityGraph(attributePaths = {"author", "post"})
    List<Reply> findByPostPkidOrderByCreatedAtDesc(Long postPkid);

    long countByPostPkid(Long postPkid);
}
