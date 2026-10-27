package br.com.agroclima.service;

import br.com.agroclima.domain.PlantationEntity;
import br.com.agroclima.domain.UserEntity;
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
import java.util.Set;
import java.util.UUID;

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
}
