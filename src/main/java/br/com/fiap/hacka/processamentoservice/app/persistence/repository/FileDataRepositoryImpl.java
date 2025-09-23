package br.com.fiap.hacka.processamentoservice.app.persistence.repository;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.processamentoservice.app.core.commons.mapper.FileDataMapper;
import br.com.fiap.hacka.processamentoservice.app.core.domain.FileData;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FileDataRepositoryImpl implements FileDataRepository {
    private static final FileDataMapper MAPPER = FileDataMapper.INSTANCE;
    private final FileDataJpaRepository repository;

    public FileData save(FileData fileData) {
        return MAPPER.toDomain(repository.save(MAPPER.toEntity(fileData)));
    }

    public List<FileDataDto> findByUserName(String userName) {
        return repository.findByUserName(userName)
                .stream()
                .map(MAPPER::toDto)
                .toList();
    }
}