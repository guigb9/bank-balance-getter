package br.com.itau.challenge.balance.port.output

import br.com.itau.challenge.balance.domain.model.Transaction

interface TransactionRepository {
    fun registerTransaction(newTransaction: Transaction)
}