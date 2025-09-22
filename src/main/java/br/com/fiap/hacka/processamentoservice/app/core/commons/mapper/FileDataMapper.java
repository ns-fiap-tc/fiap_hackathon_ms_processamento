package br.com.fiap.hacka.processamentoservice.app.core.commons.mapper;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.processamentoservice.app.core.domain.FileData;
import br.com.fiap.hacka.processamentoservice.app.persistence.entity.FileDataEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FileDataMapper {
    FileDataMapper INSTANCE = Mappers.getMapper(FileDataMapper.class);

    FileData toDomain(FileDataEntity entity);
    FileDataEntity toEntity(FileData domain);
    FileDataDto toDto(FileData domain);
    FileDataDto toDto(FileDataEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "framesUrl", source = "frameFilePath")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FileDataDto toDto(FilePartDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "framesUrl", source = "frameFilePath")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FileData toDomain(FilePartDto dto);
}