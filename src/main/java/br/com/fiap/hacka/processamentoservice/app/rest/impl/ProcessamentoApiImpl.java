package br.com.fiap.hacka.processamentoservice.app.rest.impl;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.processamentoservice.app.rest.ProcessamentoApi;
import br.com.fiap.hacka.processamentoservice.app.service.FileDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/video")
public class ProcessamentoApiImpl implements ProcessamentoApi {

    private final FileDataService fileDataService;

    @Override
    @Operation(summary = "Lista os arquivos armazenados a partir do username do usuario.", method = "POST")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista contendo os arquivos armazenedos pelo usuario."),
            @ApiResponse(responseCode = "204", description = "Lista vazia.")
    })
    @GetMapping("/findByUserName/{userName}")
    public ResponseEntity<?> findByUserName(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "username a ser utilizado na consulta dos arquivos armazenados.")
            @PathVariable("userName") String userName) {
        List<FileDataDto> dtos = fileDataService.findByUserName(userName);
        if (dtos != null && !dtos.isEmpty()) {
            return new ResponseEntity<>(dtos, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }
}