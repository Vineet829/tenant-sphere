package com.tenantsphere.issue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    @EntityGraph(attributePaths = {"apartment", "reportedBy", "assignedTo"})
    Page<Issue> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"apartment", "reportedBy", "assignedTo"})
    List<Issue> findAllBy();

    @EntityGraph(attributePaths = {"apartment", "reportedBy", "assignedTo"})
    Page<Issue> findByReportedByPkid(Long reportedByPkid, Pageable pageable);

    @EntityGraph(attributePaths = {"apartment", "reportedBy", "assignedTo"})
    Page<Issue> findByAssignedToPkid(Long assignedToPkid, Pageable pageable);

    @EntityGraph(attributePaths = {"apartment", "reportedBy", "assignedTo"})
    @Query("select i from Issue i where i.id = :externalId")
    Optional<Issue> findByExternalId(@Param("externalId") UUID externalId);
}
