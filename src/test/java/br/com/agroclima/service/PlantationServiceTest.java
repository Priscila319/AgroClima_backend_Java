package br.com.agroclima.service;

import br.com.agroclima.domain.PlantationEntity;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.dto.PlantationDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.repository.PlantationRepository;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.repository.VarietyRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantationServiceTest {
    @Mock PlantationRepository plantations;
    @Mock UserRepository users;
    @Mock VarietyRepository varieties;
    @Mock AuditService audit;
    @InjectMocks PlantationService service;

    @Test
    void producerReadsOnlyTheirOwnPlantations() {
        UUID producerId = UUID.randomUUID();
        AgroUserPrincipal producer = new AgroUserPrincipal(producerId, "Produtora", "produtora@example.test",
            "hash", UserEntity.Status.ACTIVE, null, Set.of("PRODUCER"), Set.of("plantations.manage"));
        when(plantations.findAllByArchivedAtIsNullAndOwner_IdOrderByPlantedAtDesc(producerId)).thenReturn(List.of());

        assertTrue(service.list(producer, false).isEmpty());

        verify(plantations).findAllByArchivedAtIsNullAndOwner_IdOrderByPlantedAtDesc(producerId);
        verify(plantations, never()).findAllByArchivedAtIsNullOrderByPlantedAtDesc();
    }

    // ---- Isolamento entre produtores (owner_id) ----

    private static UserEntity userWithId(UUID id, String email) {
        UserEntity user = new UserEntity("Usuário " + email, email, "hash", "", "", "", Set.of());
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static AgroUserPrincipal principal(UUID id, String role, String... permissions) {
        return new AgroUserPrincipal(id, "Teste", id + "@example.test", "hash", UserEntity.Status.ACTIVE, null,
            Set.of(role), Set.of(permissions));
    }

    private PlantationEntity plantationOwnedBy(UUID plantationId, UserEntity owner) {
        PlantationEntity plantation = new PlantationEntity(owner, "Uva Itália", 100, "Talhão 1", "");
        ReflectionTestUtils.setField(plantation, "id", plantationId);
        when(plantations.findById(plantationId)).thenReturn(Optional.of(plantation));
        return plantation;
    }

    @Test
    void producerCannotUpdateAnotherProducersPlantation() {
        UUID plantationId = UUID.randomUUID();
        plantationOwnedBy(plantationId, userWithId(UUID.randomUUID(), "a@example.test"));
        AgroUserPrincipal producerB = principal(UUID.randomUUID(), "PRODUCER", "plantations.manage");

        ApiException error = assertThrows(ApiException.class, () -> service.update(plantationId,
            new PlantationDtos.UpdatePlantationRequest("Uva Itália", 200, "Talhão 1", ""), producerB));

        assertEquals(403, error.getStatus().value());
        assertEquals("Você só pode alterar as suas próprias plantações.", error.getMessage());
        verify(plantations, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void producerCannotHarvestOrArchiveAnotherProducersPlantation() {
        UUID plantationId = UUID.randomUUID();
        plantationOwnedBy(plantationId, userWithId(UUID.randomUUID(), "a@example.test"));
        AgroUserPrincipal producerB = principal(UUID.randomUUID(), "PRODUCER", "plantations.manage");

        assertEquals(403, assertThrows(ApiException.class, () -> service.harvest(plantationId, producerB)).getStatus().value());
        assertEquals(403, assertThrows(ApiException.class, () -> service.archive(plantationId, producerB)).getStatus().value());
        verify(plantations, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ownerCanHarvestTheirOwnPlantation() {
        UUID ownerId = UUID.randomUUID();
        UUID plantationId = UUID.randomUUID();
        PlantationEntity plantation = plantationOwnedBy(plantationId, userWithId(ownerId, "a@example.test"));
        when(plantations.save(plantation)).thenReturn(plantation);

        service.harvest(plantationId, principal(ownerId, "PRODUCER", "plantations.manage"));

        assertEquals(PlantationEntity.Status.COLHIDA, plantation.getStatus());
    }

    @Test
    void adminCanHarvestAnyonesPlantation() {
        UUID plantationId = UUID.randomUUID();
        PlantationEntity plantation = plantationOwnedBy(plantationId, userWithId(UUID.randomUUID(), "a@example.test"));
        when(plantations.save(plantation)).thenReturn(plantation);

        service.harvest(plantationId, principal(UUID.randomUUID(), "ADMIN", "plantations.manage"));

        assertEquals(PlantationEntity.Status.COLHIDA, plantation.getStatus());
    }
}
