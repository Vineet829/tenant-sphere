package com.tenantsphere.common;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentViewRepository extends JpaRepository<ContentView, Long> {

    long countByContentTypeAndObjectPkid(String contentType, Long objectPkid);

    @EntityGraph(attributePaths = {"user"})
    List<ContentView> findByContentTypeAndObjectPkidOrderByLastViewedDesc(
            String contentType, Long objectPkid);

    @EntityGraph(attributePaths = {"user"})
    List<ContentView> findAllBy();

    Optional<ContentView> findByContentTypeAndObjectPkidAndUserPkidAndViewerIp(
            String contentType, Long objectPkid, Long userPkid, String viewerIp);

    @Query("""
            select v.objectPkid as objectPkid, count(v) as total
            from ContentView v
            where v.contentType = :contentType and v.objectPkid in :objectPkids
            group by v.objectPkid
            """)
    List<ViewCount> countByObjects(
            @Param("contentType") String contentType,
            @Param("objectPkids") List<Long> objectPkids);

    interface ViewCount {
        Long getObjectPkid();

        Long getTotal();
    }
}
