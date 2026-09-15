package br.com.itau.challenge.hello.port.output

import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.port.output.data.AccountBalanceOutputPort
import java.util.UUID

interface AccountBalanceProvider {
    fun getAccountWithBalance(accountId: UUID) : Account
}