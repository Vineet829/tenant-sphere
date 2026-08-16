package com.tenantsphere.post;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    @Query("""
            select t.name as name, t.slug as slug, count(p) as postCount
            from Tag t join Post p on t member of p.tags
            group by t.pkid, t.name, t.slug
            order by count(p) desc, t.name asc
            """)
    List<TagCount> findPopular(Pageable pageable);

    interface TagCount {
        String getName();

        String getSlug();

        Long getPostCount();
    }
}
