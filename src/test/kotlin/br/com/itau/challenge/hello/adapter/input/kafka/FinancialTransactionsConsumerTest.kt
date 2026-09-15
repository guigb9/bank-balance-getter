package br.com.itau.challenge.hello.adapter.input.kafka

import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.domain.model.AccountStatusEnum
import br.com.itau.challenge.hello.domain.model.Balance
import br.com.itau.challenge.hello.domain.model.Transaction
import br.com.itau.challenge.hello.domain.model.TransactionStatusEnum
import br.com.itau.challenge.hello.domain.model.TransactionTypeEnum
import br.com.itau.challenge.hello.port.input.FinancialTransactionProcessorUseCase
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals


import tools.jackson.databind.DeserializationFeature
import java.math.BigDecimal
import java.security.InvalidParameterException
import java.util.Currency
import java.util.UUID

class FinancialTransactionsConsumerTest {

    private val objectMapper = JsonMapper.builder().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build()

    private val accountToAssert = Account(
        id = UUID.fromString("5b19c8b6-0cc4-4c72-a989-0c2ee15fa975"),
        ownerId = UUID.fromString("315e3cfe-f4af-4cd2-b298-a449e614349a"),
        createdAt = 1634874339000000,
        updatedAt = 1751641364589998,
        balance = Balance(
            currency = "BRL",
            amount = BigDecimal("183.12").setScale(2)
        ),
        status = AccountStatusEnum.ENABLED,
    )


    private val transactionToAssert = Transaction(
        id = UUID.fromString("8e8ae808-b154-48b5-9f3e-553935cc4543"),
        type = TransactionTypeEnum.valueOf("CREDIT"),
        amount = BigDecimal("97.07"),
        currency = Currency.getInstance("BRL"),
        status = TransactionStatusEnum.valueOf("APPROVED"),
        timestamp = 1751641364589998,
        accountId = UUID.fromString("5b19c8b6-0cc4-4c72-a989-0c2ee15fa975")
    )


    @Test
    fun `should deserialize the JSON payload and delegate it to the save use case`() {
        var capturedAccount: Account? = null
        var capturedTransaction: Transaction? = null;

        val useCase = FinancialTransactionProcessorUseCase { account, transaction ->
            capturedAccount = account
            capturedTransaction = transaction
        }

        val consumer = FinancialTransactionsConsumer(objectMapper, useCase)

        consumer.consume(
            """
            {
              "transaction": {
                "id": "8e8ae808-b154-48b5-9f3e-553935cc4543",
                "type": "CREDIT",
                "amount": 97.07,
                "currency": "BRL",
                "status": "APPROVED",
                "timestamp": 1751641364589998
              },
              "account": {
                "id": "5b19c8b6-0cc4-4c72-a989-0c2ee15fa975",
                "owner": "315e3cfe-f4af-4cd2-b298-a449e614349a",
                "created_at": 1634874339000000,
                "status": "ENABLED",
                "balance": {
                  "amount": 183.12,
                  "currency": "BRL"
                }
              }
            }
        """.trimIndent()
        )

        assertEquals(objectMapper.writeValueAsString(accountToAssert), objectMapper.writeValueAsString(capturedAccount))
        assertEquals(transactionToAssert, capturedTransaction)
    }

    @Test
    fun `should throw InvalidParameterException when JSON payload is invalid`() {

        val useCase = FinancialTransactionProcessorUseCase { account, transaction ->
        }

        val consumer = FinancialTransactionsConsumer(objectMapper, useCase)
        assertThrows<InvalidParameterException> {
            // Chame aqui a função que deve dar erro
            consumer.consume(
                """
            {
              "transaction": {
                "type": "CREDIT",
                "amount": 97.07,
                "currency": "BRL",
                "status": "APPROVED",
                "timestamp": 1751641364589998
              },
              "account": {
                "id": "5b19c8b6-0cc4-4c72-a989-0c2ee15fa975",
                "owner": "315e3cfe-f4af-4cd2-b298-a449e614349a",
                "created_at": 1634874339000000,
                "status": "ENABLED",
                "balance": {
                  "amount": 183.12,
                  "currency": "BRL"
                }
              }
            }
        """.trimIndent()
            )
        }

    }
}