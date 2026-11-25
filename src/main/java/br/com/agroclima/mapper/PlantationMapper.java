package br.com.agroclima.mapper;

import br.com.agroclima.domain.PlantationEntity;
import br.com.agroclima.dto.PlantationDtos;

public final class PlantationMapper {
    private PlantationMapper() {}

    public static PlantationDtos.PlantationResponse toResponse(PlantationEntity plantation) {
        String status = switch (plantation.getStatus()) {
            case COLHIDA -> "Colhida";
            case ARQUIVADA -> "Arquivada";
            default -> "Em cultivo";
        };
        return new PlantationDtos.PlantationResponse(plantation.getId(), plantation.getOwner().getId(),
            plantation.getOwner().getFullName(), plantation.getVariety(), plantation.getQuantity(),
            plantation.getFieldName(), plantation.getNotes(), status, plantation.getPlantedAt(),
            plantation.getHarvestedAt(), plantation.getCreatedAt(), plantation.getArchivedAt());
    }
}
