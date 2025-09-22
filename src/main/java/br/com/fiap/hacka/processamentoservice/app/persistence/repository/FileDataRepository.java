package br.com.fiap.hacka.processamentoservice.app.persistence.repository;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.processamentoservice.app.core.domain.FileData;
import java.util.List;

public interface FileDataRepository {

    FileData save(FileData fileData);
    List<FileDataDto> findByUserName(String userName);
}