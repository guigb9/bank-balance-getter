package br.com.itau.challenge.balance.adapter.input.kafka.mapper

import br.com.itau.challenge.balance.ACCOUNT_ID
import br.com.itau.challenge.balance.OWNER_ID
import br.com.itau.challenge.balance.TRANSACTION_ID
import br.com.itau.challenge.balance.adapter.input.kafka.dto.AccountMessage
import br.com.itau.challenge.balance.adapter.input.kafka.dto.BalanceMessage
import br.com.itau.challenge.balance.adapter.input.kafka.dto.TransactionMessage
import br.com.itau.challenge.balance.adapter.input.kafka.exception.InvalidMessageFormatException
import br.com.itau.challenge.balance.domain.model.AccountStatusEnum
import br.com.itau.challenge.balance.domain.model.TransactionStatusEnum
import br.com.itau.challenge.balance.domain.model.TransactionTypeEnum
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.security.InvalidParameterException
import kotlin.test.assertEquals

class MessageMappersTest {

    private val validAccount = AccountMessage(
        id = ACCOUNT_ID.toString(),
        owner = OWNER_ID.toString(),
        createdAt = 10,
        status = "ENABLED",
        balance = BalanceMessage("BRL", BigDecimal("42.10")),
    )

    private val validTransaction = TransactionMessage(
        id = TRANSACTION_ID.toString(),
        type = "CREDIT",
        currency = "BRL",
        amount = BigDecimal("12.30"),
        status = "APPROVED",
        timestamp = 20,
    )

    @Test
    fun `account message maps all fields`() {
        val result = validAccount.toEntity(updatedAt = 20)

        assertEquals(ACCOUNT_ID, result.id)
        assertEquals(OWNER_ID, result.ownerId)
        assertEquals(AccountStatusEnum.ENABLED, result.status)
        assertEquals(BigDecimal("42.10"), result.balance.amount)
        assertEquals("BRL", result.balance.currency)
        assertEquals(10, result.createdAt)
        assertEquals(20, result.updatedAt)
    }

    @Test
    fun `account mapper identifies each missing required field`() {
        val cases = listOf(
            "id" to validAccount.copy(id = null),
            "ownerId" to validAccount.copy(owner = null),
            "status" to validAccount.copy(status = null),
            "balance" to validAccount.copy(balance = null),
            "balance.amount" to validAccount.copy(balance = BalanceMessage("BRL", null)),
            "balance.amount" to validAccount.copy(balance = BalanceMessage(null, BigDecimal.ONE)),
            "createdAt" to validAccount.copy(createdAt = null),
        )

        cases.forEach { (field, message) ->
            val exception = assertThrows<InvalidMessageFormatException> { message.toEntity(20) }
            assertEquals("Field '$field' is not a valid format", exception.message)
        }

        assertThrows<InvalidMessageFormatException> { validAccount.toEntity(null) }
    }

    @Test
    fun `transaction message maps all fields`() {
        val result = validTransaction.toEntity(ACCOUNT_ID.toString())

        assertEquals(TRANSACTION_ID, result.id)
        assertEquals(TransactionTypeEnum.CREDIT, result.type)
        assertEquals(BigDecimal("12.30"), result.amount)
        assertEquals("BRL", result.currency.currencyCode)
        assertEquals(TransactionStatusEnum.APPROVED, result.status)
        assertEquals(20, result.timestamp)
        assertEquals(ACCOUNT_ID, result.accountId)
    }

    @Test
    fun `transaction mapper rejects each missing required field`() {
        val messages = listOf(
            validTransaction.copy(id = null),
            validTransaction.copy(type = null),
            validTransaction.copy(amount = null),
            validTransaction.copy(currency = null),
            validTransaction.copy(status = null),
            validTransaction.copy(timestamp = null),
        )

        messages.forEach { message ->
            assertThrows<InvalidParameterException> { message.toEntity(ACCOUNT_ID.toString()) }
        }
        assertThrows<InvalidParameterException> { validTransaction.toEntity(null) }
    }
}
