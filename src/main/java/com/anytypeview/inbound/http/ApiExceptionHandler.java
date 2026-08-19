package com.anytypeview.inbound.http;

import com.anytypeview.core.dto.SyncResultDTO;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public SyncResultDTO handleRestClientException(RestClientException exception) {
        LOGGER.error("Erro ao acessar a API do Anytype", exception);
        return new SyncResultDTO(
            "ANYTYPE_UNAVAILABLE",
            "Nao foi possivel acessar a API do Anytype: " + exception.getMessage(),
            OffsetDateTime.now(),
            null,
            null
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public SyncResultDTO handleIllegalStateException(IllegalStateException exception) {
        LOGGER.error("Erro durante sincronizacao", exception);
        return new SyncResultDTO(
            "SYNC_ERROR",
            exception.getMessage(),
            OffsetDateTime.now(),
            null,
            null
        );
    }
}
