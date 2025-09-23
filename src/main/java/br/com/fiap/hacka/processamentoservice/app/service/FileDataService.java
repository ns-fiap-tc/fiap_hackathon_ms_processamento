package br.com.fiap.hacka.processamentoservice.app.service;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.processamentoservice.app.core.commons.mapper.FileDataMapper;
import br.com.fiap.hacka.processamentoservice.app.persistence.repository.FileDataRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FileDataService {
    private static final FileDataMapper MAPPER = FileDataMapper.INSTANCE;
    private final FileDataRepository fileDataRepository;

    public FileDataDto save(FilePartDto dto) {
        return MAPPER.toDto(fileDataRepository.save(MAPPER.toDomain(dto)));
    }

    public List<FileDataDto> findByUserName(String userName) {
        return fileDataRepository.findByUserName(userName);
    }
}