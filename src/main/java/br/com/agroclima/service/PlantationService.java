package br.com.agroclima.service;

import br.com.agroclima.config.RoleCatalog;
import br.com.agroclima.domain.PlantationEntity;
import br.com.agroclima.domain.UserEntity;
import br.com.agroclima.domain.VarietyEntity;
import br.com.agroclima.dto.PlantationDtos;
import br.com.agroclima.exception.ApiException;
import br.com.agroclima.mapper.PlantationMapper;
import br.com.agroclima.repository.PlantationRepository;
import br.com.agroclima.repository.UserRepository;
import br.com.agroclima.repository.VarietyRepository;
import br.com.agroclima.security.AgroUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlantationService {
    private final PlantationRepository plantations;
    private final UserRepository users;
    private final VarietyRepository varieties;
    private final AuditService audit;

    public PlantationService(PlantationRepository plantations, UserRepository users,
                            VarietyRepository varieties, AuditService audit) {
        this.plantations = plantations;
        this.users = users;
        this.varieties = varieties;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PlantationDtos.PlantationResponse> list(AgroUserPrincipal actor, boolean includeArchived) {
        boolean canReadAll = actor.roleKeys().contains(RoleCatalog.ADMIN)
            || actor.permissionKeys().contains("plantations.read.all");
        List<PlantationEntity> found;
        if (canReadAll) {
            found = includeArchived ? plantations.findAllByOrderByPlantedAtDesc() : plantations.findAllByArchivedAtIsNullOrderByPlantedAtDesc();
        } else {
            found = includeArchived
                ? plantations.findAllByOwner_IdOrderByPlantedAtDesc(actor.id())
                : plantations.findAllByArchivedAtIsNullAndOwner_IdOrderByPlantedAtDesc(actor.id());
        }
        return found.stream().map(PlantationMapper::toResponse).toList();
    }

    @Transactional
    public PlantationDtos.PlantationResponse create(PlantationDtos.CreatePlantationRequest request, AgroUserPrincipal actor) {
        UserEntity owner = users.findWithRolesById(actor.id())
            .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        VarietyEntity variety = activeVariety(request.variety());
        PlantationEntity entity = new PlantationEntity(owner, variety.getName(), request.quantity(),
            request.field(), request.notes());
        PlantationEntity saved = plantations.save(entity);
        audit.record(actor, "Cadastrou uma plantação", "Plantações",
            variety.getName() + " · " + request.quantity() + " plantas · data inicial automática.");
        return PlantationMapper.toResponse(saved);
    }

    @Transactional
    public PlantationDtos.PlantationResponse update(UUID plantationId, PlantationDtos.UpdatePlantationRequest request,
                                                     AgroUserPrincipal actor) {
        PlantationEntity plantation = findPlantation(plantationId);
        ensureOwnerOrAdmin(plantation, actor);
        VarietyEntity variety = activeVariety(request.variety());
        try {
            plantation.updateDetails(variety.getName(), request.quantity(), request.field(), request.notes());
        } catch (IllegalStateException exception) {
            throw ApiException.conflict(exception.getMessage());
        }
        PlantationEntity saved = plantations.save(plantation);
        audit.record(actor, "Atualizou uma plantação", "Plantações", saved.getVariety() + " · detalhes atualizados.");
        return PlantationMapper.toResponse(saved);
    }

    @Transactional
    public PlantationDtos.PlantationResponse harvest(UUID plantationId, AgroUserPrincipal actor) {
        PlantationEntity plantation = findPlantation(plantationId);
        ensureOwnerOrAdmin(plantation, actor);
        try {
            plantation.markHarvested();
        } catch (IllegalStateException exception) {
            throw ApiException.conflict(exception.getMessage());
        }
        PlantationEntity saved = plantations.save(plantation);
        audit.record(actor, "Registrou a colheita", "Plantações",
            saved.getVariety() + " · data da colheita registrada pelo sistema.");
        return PlantationMapper.toResponse(saved);
    }

    @Transactional
    public void archive(UUID plantationId, AgroUserPrincipal actor) {
        PlantationEntity plantation = findPlantation(plantationId);
        ensureOwnerOrAdmin(plantation, actor);
        try {
            plantation.archive();
        } catch (IllegalStateException exception) {
            throw ApiException.conflict(exception.getMessage());
        }
        plantations.save(plantation);
        audit.record(actor, "Arquivou uma plantação", "Plantações", plantation.getVariety() + " · registro preservado no histórico.");
    }

    private PlantationEntity findPlantation(UUID id) {
        return plantations.findById(id).orElseThrow(() -> ApiException.notFound("Plantação não encontrada."));
    }

    private void ensureOwnerOrAdmin(PlantationEntity plantation, AgroUserPrincipal actor) {
        boolean admin = actor.roleKeys().contains(RoleCatalog.ADMIN);
        if (!admin && !plantation.getOwner().getId().equals(actor.id())) {
            throw ApiException.forbidden("Você só pode alterar as suas próprias plantações.");
        }
    }

    private VarietyEntity activeVariety(String name) {
        return varieties.findByNameIgnoreCase(name.trim()).filter(VarietyEntity::isActive)
            .orElseThrow(() -> ApiException.badRequest("Selecione um tipo de uva ativo e válido."));
    }
}
