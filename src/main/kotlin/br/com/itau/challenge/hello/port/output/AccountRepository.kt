package br.com.itau.challenge.hello.port.output

import br.com.itau.challenge.hello.domain.model.Account

interface AccountRepository {
    fun updateBalance(account: Account)
}