package br.com.itau.challenge.hello.application

import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.port.input.GetAccountBalanceUseCase
import br.com.itau.challenge.hello.port.output.AccountBalanceProvider
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetAccountBalanceService(private val accountBalanceProvider: AccountBalanceProvider) : GetAccountBalanceUseCase {
    override fun getAccountBalance(accountId: UUID) : Account {
        return accountBalanceProvider.getAccountWithBalance(accountId = accountId)
    }
}