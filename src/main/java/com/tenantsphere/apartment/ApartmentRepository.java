package com.tenantsphere.apartment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    Optional<Apartment> findFirstByTenantPkidOrderByPkidAsc(Long tenantPkid);

    @EntityGraph(attributePaths = {"tenant"})
    List<Apartment> findAllBy();

    List<Apartment> findByTenantPkidIn(List<Long> tenantPkids);

    Optional<Apartment> findByUnitNumber(String unitNumber);

    @Query("select a from Apartment a left join fetch a.tenant where a.id = :externalId")
    Optional<Apartment> findByExternalId(@Param("externalId") UUID externalId);
}
