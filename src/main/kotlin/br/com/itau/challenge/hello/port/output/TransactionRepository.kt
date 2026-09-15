package br.com.itau.challenge.hello.port.output

import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.domain.model.Transaction

interface TransactionRepository {
    fun registerTransaction(newTransaction: Transaction)
}