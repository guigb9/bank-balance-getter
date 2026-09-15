package br.com.itau.challenge.hello.port.input

import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.domain.model.Transaction

fun interface FinancialTransactionProcessorUseCase {
    fun processTransaction(account: Account, transaction: Transaction)
}