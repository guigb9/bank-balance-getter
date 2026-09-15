package br.com.itau.challenge.balance.port.output

import br.com.itau.challenge.balance.domain.model.Account

interface AccountRepository {
    fun updateBalance(account: Account)
}