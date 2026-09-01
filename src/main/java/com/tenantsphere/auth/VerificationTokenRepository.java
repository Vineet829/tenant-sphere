package com.tenantsphere.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<VerificationToken> findByTokenHashAndPurpose(
            String tokenHash, VerificationPurpose purpose);

    @Modifying
    @Query("delete from VerificationToken t where t.user.pkid = :userPkid and t.purpose = :purpose")
    void deleteByUserAndPurpose(
            @Param("userPkid") Long userPkid, @Param("purpose") VerificationPurpose purpose);
}
