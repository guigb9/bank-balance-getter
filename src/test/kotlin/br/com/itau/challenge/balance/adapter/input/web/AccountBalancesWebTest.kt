package br.com.itau.challenge.balance.adapter.input.web

import br.com.itau.challenge.balance.ACCOUNT_ID
import br.com.itau.challenge.balance.OWNER_ID
import br.com.itau.challenge.balance.account
import br.com.itau.challenge.balance.adapter.input.web.config.AccountBalanceControllerAdvice
import br.com.itau.challenge.balance.adapter.input.web.mappers.toResponse
import br.com.itau.challenge.balance.adapter.output.dynamodb.exception.AccountNotFoundException
import br.com.itau.challenge.balance.port.input.GetAccountBalanceUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AccountBalancesWebTest {

    @Test
    fun `controller returns mapped account balance`() {
        val useCase = mock(GetAccountBalanceUseCase::class.java)
        val account = account()
        `when`(useCase.getAccountBalance(ACCOUNT_ID)).thenReturn(account)

        val response = AccountBalancesController(useCase).getBalance(ACCOUNT_ID)!!

        assertEquals(ACCOUNT_ID, response.id)
        assertEquals(OWNER_ID, response.owner)
        assertEquals("BRL", response.balance.currency)
        assertEquals(BigDecimal("183.12"), response.balance.amount)
        verify(useCase).getAccountBalance(ACCOUNT_ID)
    }

    @Test
    fun `controller preserves use case exception for advice`() {
        val useCase = mock(GetAccountBalanceUseCase::class.java)
        val failure = AccountNotFoundException(ACCOUNT_ID)
        `when`(useCase.getAccountBalance(ACCOUNT_ID)).thenThrow(failure)

        assertSame(
            failure,
            assertThrows<AccountNotFoundException> {
                AccountBalancesController(useCase).getBalance(ACCOUNT_ID)
            },
        )
    }

    @Test
    fun `response mapper converts microseconds to Sao Paulo local time`() {
        val account = account(updatedAt = 1_751_641_364_589_998)
        val expected = Instant.ofEpochMilli(account.updatedAt / 1000)
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .toLocalDateTime()

        assertEquals(expected, account.toResponse().updatedAt)
    }

    @Test
    fun `advice returns not found payload with domain message`() {
        val advice = AccountBalanceControllerAdvice(mock(MessageSource::class.java))

        val response = advice.accountBalanceNotFoundException(AccountNotFoundException(ACCOUNT_ID))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("not.found", response.body?.code)
        assertEquals("Account '$ACCOUNT_ID' not found", response.body?.message)
    }

    @Test
    fun `advice localizes unexpected server errors`() {
        val messageSource = mock(MessageSource::class.java)
        `when`(messageSource.getMessage("server.error", null, Locale.getDefault()))
            .thenReturn("Erro interno")

        val response = AccountBalanceControllerAdvice(messageSource).exception(IllegalStateException())

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("server.error", response.body?.code)
        assertEquals("Erro interno", response.body?.message)
    }
}
