package fr.francetv.demo.infrastructure.adapter.out.persistence;

import fr.francetv.demo.domain.model.Item;
import fr.francetv.foundation.mapping.config.FoundationMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = FoundationMapperConfig.class)
public interface ItemPersistenceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ItemEntity toEntity(Item item);

    Item toDomain(ItemEntity entity);
}
