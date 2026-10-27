package br.com.agroclima.repository;

import br.com.agroclima.domain.PlantationEntity;
import br.com.agroclima.domain.RoleEntity;
import br.com.agroclima.domain.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class MySqlPersistenceTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired VarietyRepository varieties;
    @Autowired PlantationRepository plantations;

    @Test
    void migrationsSeedRealVarietiesAndPlantationDatesAreServerOwned() {
        assertEquals(5, varieties.findAllByActiveTrueOrderByNameAsc().size());
        RoleEntity producerRole = roles.findByNameIgnoreCase("PRODUCER").orElseThrow();
        UserEntity producer = users.saveAndFlush(new UserEntity("Produtora de Teste", "producer@test.invalid",
            "not-a-real-password-hash", "", "", "", Set.of(producerRole)));
        PlantationEntity plantation = plantations.saveAndFlush(new PlantationEntity(producer, "Uva Itália", 250, "Talhão A", ""));

        assertNotNull(plantation.getPlantedAt());
        assertNull(plantation.getHarvestedAt());
        plantation.markHarvested();
        plantations.saveAndFlush(plantation);
        assertNotNull(plantation.getHarvestedAt());
        assertEquals(PlantationEntity.Status.COLHIDA, plantation.getStatus());
    }
}
