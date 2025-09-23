package br.com.fiap.hacka.processamentoservice.app.persistence.repository;

import br.com.fiap.hacka.processamentoservice.app.persistence.entity.FileDataEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FileDataJpaRepository extends MongoRepository<FileDataEntity, String> {

    @Query("{'userName' : ?0}")
    List<FileDataEntity> findByUserName(String userName);
}