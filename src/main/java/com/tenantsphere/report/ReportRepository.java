package com.tenantsphere.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReportedByPkid(Long reportedByPkid, Pageable pageable);

    @EntityGraph(attributePaths = {"reportedBy", "reportedUser"})
    List<Report> findAllBy();

    @Query("""
            select count(distinct r.reportedBy.pkid) from Report r
            where r.reportedUser.pkid = :reportedUserPkid
              and r.reportedBy.pkid <> r.reportedUser.pkid
              and r.reportedBy.guest = false
            """)
    long countDistinctReporters(@Param("reportedUserPkid") Long reportedUserPkid);

    Optional<Report> findBySlug(String slug);
}
