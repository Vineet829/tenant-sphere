package com.tenantsphere.rating;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    @Query("select avg(r.rating) from Rating r where r.ratedUser.pkid = :userPkid")
    Optional<Double> averageForUser(@Param("userPkid") Long userPkid);

    @EntityGraph(attributePaths = {"ratedUser", "ratingUser"})
    List<Rating> findAllBy();
}
