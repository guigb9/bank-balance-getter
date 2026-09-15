package br.com.itau.challenge.balance.port.output

import br.com.itau.challenge.balance.domain.model.Account
import java.util.UUID

interface AccountBalanceProvider {
    fun getAccountWithBalance(accountId: UUID) : Account
}