package com.tenantsphere.profile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserPkid(Long userPkid);

    @EntityGraph(attributePaths = {"user"})
    List<Profile> findAllBy();

    Optional<Profile> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            select p from Profile p
            join fetch p.user u
            where u.staff = false
              and u.superuser = false
              and p.occupation = com.tenantsphere.profile.Occupation.TENANT
            order by p.createdAt desc, p.updatedAt desc
            """)
    Page<Profile> findTenantProfiles(Pageable pageable);
}
