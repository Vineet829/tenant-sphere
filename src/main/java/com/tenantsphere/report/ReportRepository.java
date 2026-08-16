package com.tenantsphere.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReportedByPkid(Long reportedByPkid, Pageable pageable);

    @EntityGraph(attributePaths = {"reportedBy", "reportedUser"})
    List<Report> findAllBy();

    long countByReportedUserPkid(Long reportedUserPkid);

    Optional<Report> findBySlug(String slug);
}
