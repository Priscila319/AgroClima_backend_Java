package br.com.agroclima.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlantationEntityTest {
    @Test
    void setsStartDateAtPersistenceAndHarvestDateOnlyOnHarvest() {
        UserEntity owner = new UserEntity("Produtora", "produtora@example.test", "hash", "", "", "", Set.of(new RoleEntity("PRODUCER", "Produtor")));
        PlantationEntity plantation = new PlantationEntity(owner, "Uva Itália", 120, "Talhão 1", "Observação");

        assertEquals(null, plantation.getPlantedAt());
        assertEquals(null, plantation.getHarvestedAt());
        plantation.onCreate();

        assertNotNull(plantation.getPlantedAt());
        assertEquals(PlantationEntity.Status.EM_CULTIVO, plantation.getStatus());
        plantation.markHarvested();
        assertNotNull(plantation.getHarvestedAt());
        assertEquals(PlantationEntity.Status.COLHIDA, plantation.getStatus());
        assertThrows(IllegalStateException.class, plantation::markHarvested);
    }

    @Test
    void archivePreservesHarvestTimestamp() {
        UserEntity owner = new UserEntity("Produtora", "produtora@example.test", "hash", "", "", "", Set.of());
        PlantationEntity plantation = new PlantationEntity(owner, "Uva Itália", 10, "", "");
        plantation.onCreate();
        plantation.markHarvested();
        Instant harvestedAt = plantation.getHarvestedAt();
        plantation.archive();
        assertEquals(PlantationEntity.Status.ARQUIVADA, plantation.getStatus());
        assertEquals(harvestedAt, plantation.getHarvestedAt());
        assertNotNull(plantation.getArchivedAt());
    }
}
