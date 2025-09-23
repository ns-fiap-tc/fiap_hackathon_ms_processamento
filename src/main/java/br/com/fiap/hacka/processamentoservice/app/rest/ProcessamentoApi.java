package br.com.fiap.hacka.processamentoservice.app.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface ProcessamentoApi {
    ResponseEntity<?> findByUserName(@PathVariable("userName") String userName);
}