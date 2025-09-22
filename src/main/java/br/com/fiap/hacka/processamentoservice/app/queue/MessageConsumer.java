package br.com.fiap.hacka.processamentoservice.app.queue;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import org.springframework.messaging.handler.annotation.Payload;

public interface MessageConsumer {
    public void receive(@Payload FilePartDto filePart);
}