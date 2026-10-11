package com.tenantsphere.profile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profile p where p.pkid = :pkid")
    Optional<Profile> findByPkidForUpdate(@Param("pkid") Long pkid);

    Optional<Profile> findByUserPkid(Long userPkid);

    @EntityGraph(attributePaths = {"user"})
    List<Profile> findAllBy();

    Optional<Profile> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            select p from Profile p
            join fetch p.user u
            where u.active = true
              and u.staff = false
              and u.superuser = false
              and u.guest = false
              and p.occupation = com.tenantsphere.profile.Occupation.TENANT
            order by p.createdAt desc, p.updatedAt desc
            """)
    Page<Profile> findTenantProfiles(Pageable pageable);

    @Query("""
            select p from Profile p
            join fetch p.user u
            where u.active = true
              and u.staff = false
              and u.superuser = false
              and u.guest = false
              and p.occupation <> com.tenantsphere.profile.Occupation.TENANT
            order by p.createdAt desc, p.updatedAt desc
            """)
    Page<Profile> findNonTenantProfiles(Pageable pageable);

    @Query("""
            select p from Profile p
            join fetch p.user u
            where u.active = true
              and u.staff = false
              and u.superuser = false
              and u.guest = false
              and p.occupation = com.tenantsphere.profile.Occupation.TENANT
              and (lower(u.username) like %:term%
                   or lower(u.firstName) like %:term%
                   or lower(u.lastName) like %:term%)
            order by p.createdAt desc, p.updatedAt desc
            """)
    Page<Profile> searchTenantProfiles(@Param("term") String term, Pageable pageable);

    @Query("""
            select p from Profile p
            join fetch p.user u
            where u.active = true
              and u.staff = false
              and u.superuser = false
              and u.guest = false
              and p.occupation <> com.tenantsphere.profile.Occupation.TENANT
              and (lower(u.username) like %:term%
                   or lower(u.firstName) like %:term%
                   or lower(u.lastName) like %:term%)
            order by p.createdAt desc, p.updatedAt desc
            """)
    Page<Profile> searchNonTenantProfiles(@Param("term") String term, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Profile> findAllByUserPkidIn(Collection<Long> userPkids);
}
