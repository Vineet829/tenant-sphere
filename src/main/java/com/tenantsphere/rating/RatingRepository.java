package com.tenantsphere.rating;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    @Query("select avg(r.rating) from Rating r where r.ratedUser.pkid = :userPkid")
    Optional<Double> averageForUser(@Param("userPkid") Long userPkid);

    boolean existsByRatedUserPkidAndRatingUserPkid(Long ratedUserPkid, Long ratingUserPkid);

    @EntityGraph(attributePaths = {"ratedUser", "ratingUser"})
    List<Rating> findAllBy();

    @EntityGraph(attributePaths = {"ratingUser"})
    Page<Rating> findByRatedUserPkidOrderByCreatedAtDescPkidDesc(Long userPkid, Pageable pageable);

    @Query("""
            select r.rating as stars, count(r) as total
            from Rating r where r.ratedUser.pkid = :userPkid
            group by r.rating
            """)
    List<RatingCount> countByStars(@Param("userPkid") Long userPkid);

    interface RatingCount {
        Integer getStars();
        Long getTotal();
    }
}
