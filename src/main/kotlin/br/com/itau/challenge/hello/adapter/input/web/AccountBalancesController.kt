package br.com.itau.challenge.hello.adapter.input.web

import br.com.itau.challenge.hello.adapter.input.web.dto.AccountBalanceResponse
import br.com.itau.challenge.hello.adapter.input.web.mappers.toResponse
import br.com.itau.challenge.hello.port.input.GetAccountBalanceUseCase
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/balances")
class AccountBalancesController(
    private val getAccountBalanceUseCase: GetAccountBalanceUseCase
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/{id}")
    fun getBalance(@PathVariable id: UUID) : AccountBalanceResponse? {
        try {
            return getAccountBalanceUseCase.getAccountBalance(id).toResponse()
        } catch (ex: Exception) {
            logger.error("Erro while trying to get balance by id $id", ex)
            throw ex;
        }
    }
}