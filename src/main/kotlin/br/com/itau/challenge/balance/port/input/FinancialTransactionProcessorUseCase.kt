package br.com.itau.challenge.balance.port.input

import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.domain.model.Transaction

fun interface FinancialTransactionProcessorUseCase {
    fun processTransaction(account: Account, transaction: Transaction)
}