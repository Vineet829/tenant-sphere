package com.tenantsphere.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.tenantsphere.profile.dto.ProfileResponse;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProfileService.class)
class ProfileDirectoryTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        POSTGRES.start();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    @Autowired private ProfileRepository profiles;
    @Autowired private ProfileService service;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private JdbcTemplate database;

    @Test
    void directoriesListOnlyAccountsThatAreOn() {
        person("tenant_on", true, "tenant");
        person("tenant_never_activated", false, "tenant");
        person("plumber_on", true, "plumber");
        person("plumber_turned_off", false, "plumber");
        var page = PageRequest.of(0, 10);

        assertThat(profiles.findTenantProfiles(page)).extracting(p -> p.getUser().getUsername())
                .containsExactly("tenant_on");
        assertThat(profiles.searchTenantProfiles("tenant", page)).extracting(p -> p.getUser().getUsername())
                .containsExactly("tenant_on");
        assertThat(profiles.findNonTenantProfiles(page)).extracting(p -> p.getUser().getUsername())
                .containsExactly("plumber_on");
        assertThat(profiles.searchNonTenantProfiles("plumber", page)).extracting(p -> p.getUser().getUsername())
                .containsExactly("plumber_on");
    }

    @Test
    void aDirectoryPageLoadsApartmentsAndRatingsForEveryoneAtOnce() {
        long first = person("first_tenant", true, "tenant");
        long second = person("second_tenant", true, "tenant");
        long third = person("third_tenant", true, "tenant");
        person("fourth_tenant", true, "tenant");
        long plumber = person("rated_plumber", true, "plumber");
        apartment("A1", first);
        apartment("A2", first);
        apartment("B1", third);
        rating(plumber, first, 5);
        rating(plumber, second, 2);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var tenants = service.listTenants(PageRequest.of(0, 10), null).getContent();
        var technicians = service.listNonTenants(PageRequest.of(0, 10), null).getContent();

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(6);
        assertThat(tenants)
                .extracting(ProfileResponse::username,
                        tenant -> tenant.apartment() == null ? null : tenant.apartment().unitNumber())
                .containsExactlyInAnyOrder(tuple("first_tenant", "A1"), tuple("second_tenant", null),
                        tuple("third_tenant", "B1"), tuple("fourth_tenant", null));
        assertThat(technicians).extracting(ProfileResponse::averageRating).containsExactly(3.5);
    }

    private void apartment(String unit, long tenant) {
        database.update("INSERT INTO apartments (id, unit_number, building, floor, tenant_pkid) VALUES (?, ?, 'North', 1, ?)",
                UUID.randomUUID(), unit, tenant);
    }

    private void rating(long rated, long rater, int stars) {
        database.update("INSERT INTO ratings (id, rated_user_pkid, rating_user_pkid, rating) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), rated, rater, stars);
    }

    private long person(String username, boolean active, String occupation) {
        long user = database.queryForObject("""
                INSERT INTO users (id, email, username, first_name, last_name, password, is_active)
                VALUES (?, ?, ?, 'Test', 'Person', '!unused', ?) RETURNING pkid
                """, Long.class, UUID.randomUUID(), username + "@example.test", username, active);
        database.update("INSERT INTO profiles (id, user_pkid, slug, occupation) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), user, username, occupation);
        return user;
    }
}
