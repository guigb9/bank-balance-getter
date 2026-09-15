package br.com.itau.challenge.balance.application

import br.com.itau.challenge.balance.ACCOUNT_ID
import br.com.itau.challenge.balance.account
import br.com.itau.challenge.balance.port.output.AccountBalanceProvider
import br.com.itau.challenge.balance.port.output.AccountRepository
import br.com.itau.challenge.balance.port.output.TransactionRepository
import br.com.itau.challenge.balance.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doThrow
import kotlin.test.assertSame

class BalanceServicesTest {

    @Test
    fun `processor updates balance before registering transaction`() {
        val accountRepository = mock(AccountRepository::class.java)
        val transactionRepository = mock(TransactionRepository::class.java)
        val service = FinancialTransactionProcessorService(transactionRepository, accountRepository)
        val account = account()
        val transaction = transaction()

        service.processTransaction(account, transaction)

        inOrder(accountRepository, transactionRepository).apply {
            verify(accountRepository).updateBalance(account)
            verify(transactionRepository).registerTransaction(transaction)
        }
    }

    @Test
    fun `processor does not register transaction when balance update fails`() {
        val accountRepository = mock(AccountRepository::class.java)
        val transactionRepository = mock(TransactionRepository::class.java)
        val service = FinancialTransactionProcessorService(transactionRepository, accountRepository)
        val failure = IllegalStateException("DynamoDB unavailable")
        val account = account()
        doThrow(failure).`when`(accountRepository).updateBalance(account)

        val thrown = assertThrows<IllegalStateException> {
            service.processTransaction(account, transaction())
        }

        assertSame(failure, thrown)
        verify(transactionRepository, never()).registerTransaction(transaction())
    }

    @Test
    fun `balance service delegates lookup and returns provider result`() {
        val provider = mock(AccountBalanceProvider::class.java)
        val expected = account()
        `when`(provider.getAccountWithBalance(ACCOUNT_ID)).thenReturn(expected)

        val result = GetAccountBalanceService(provider).getAccountBalance(ACCOUNT_ID)

        assertSame(expected, result)
        verify(provider).getAccountWithBalance(ACCOUNT_ID)
    }
}
