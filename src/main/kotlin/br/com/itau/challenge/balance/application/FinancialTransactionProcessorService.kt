package br.com.itau.challenge.balance.application

import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.domain.model.Transaction
import br.com.itau.challenge.balance.port.input.FinancialTransactionProcessorUseCase
import br.com.itau.challenge.balance.port.output.AccountRepository
import br.com.itau.challenge.balance.port.output.TransactionRepository
import org.springframework.stereotype.Service

@Service
class FinancialTransactionProcessorService(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : FinancialTransactionProcessorUseCase {
    override fun processTransaction(
        account: Account,
        transaction: Transaction,
    ) {
        accountRepository.updateBalance(
            account = account,
        )

        transactionRepository.registerTransaction(newTransaction = transaction)
    }
}