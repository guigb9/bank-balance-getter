package br.com.itau.challenge.hello.port.input

import br.com.itau.challenge.hello.domain.model.Account
import java.util.UUID

interface GetAccountBalanceUseCase {
    fun getAccountBalance(accountId: UUID) : Account
}