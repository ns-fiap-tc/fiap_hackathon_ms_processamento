package br.com.fiap.processamentoservice.core.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FilePart {

    public static final int CHUNK_SIZE = 1024 * 1024; // 1 MB

    private String fileName;
    private int bytesRead;
    private byte[] bytes;
}
