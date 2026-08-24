package fr.francetv.demo.infrastructure.adapter.in.web;

import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.infrastructure.adapter.in.web.dto.CreateItemRequest;
import fr.francetv.demo.infrastructure.adapter.in.web.dto.ItemResponse;
import fr.francetv.foundation.mapping.config.FoundationMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = FoundationMapperConfig.class)
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item toItem(CreateItemRequest request);

    ItemResponse toResponse(Item item);
}
