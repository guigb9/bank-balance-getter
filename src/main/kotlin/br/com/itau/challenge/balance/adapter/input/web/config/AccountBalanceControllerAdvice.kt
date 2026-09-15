package br.com.itau.challenge.balance.adapter.input.web.config

import br.com.itau.challenge.balance.adapter.input.web.config.dto.ErrorDTO
import br.com.itau.challenge.balance.adapter.output.dynamodb.exception.AccountNotFoundException
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.Locale

@ControllerAdvice
class AccountBalanceControllerAdvice(
    private val messageSource: MessageSource
) {

    @ExceptionHandler(AccountNotFoundException::class)
    fun accountBalanceNotFoundException(ex: AccountNotFoundException) : ResponseEntity<ErrorDTO> {
        return ResponseEntity.status(
            HttpStatus.NOT_FOUND
        ).body(
            ErrorDTO("not.found", ex.message ?: "server.error")
        )
    }


    @ExceptionHandler(Exception::class)
    fun exception(ex: Exception) : ResponseEntity<ErrorDTO> {
        return ResponseEntity.status(
            HttpStatus.INTERNAL_SERVER_ERROR
        ).body(
            ErrorDTO("server.error", messageSource.getMessage("server.error", null, Locale.getDefault()))
        )
    }
}