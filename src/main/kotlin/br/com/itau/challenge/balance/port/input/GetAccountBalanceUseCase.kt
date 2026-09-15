package br.com.itau.challenge.balance.port.input

import br.com.itau.challenge.balance.domain.model.Account
import java.util.UUID

interface GetAccountBalanceUseCase {
    fun getAccountBalance(accountId: UUID) : Account
}