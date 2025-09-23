package br.com.fiap.hacka.processamentoservice.app.core.domain;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FileData {

    private String id;
    private String userName;
    private String fileName;
    private String framesUrl;
    private String fileUrl;
    private Date createdAt;
    private Date updatedAt;
}