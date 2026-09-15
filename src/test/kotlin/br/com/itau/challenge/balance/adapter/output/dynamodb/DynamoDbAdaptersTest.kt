package br.com.itau.challenge.balance.adapter.output.dynamodb

import br.com.itau.challenge.balance.ACCOUNT_ID
import br.com.itau.challenge.balance.OWNER_ID
import br.com.itau.challenge.balance.TRANSACTION_ID
import br.com.itau.challenge.balance.account
import br.com.itau.challenge.balance.adapter.output.dynamodb.exception.AccountNotFoundException
import br.com.itau.challenge.balance.adapter.output.dynamodb.exception.InvalidDbParameterException
import br.com.itau.challenge.balance.adapter.output.dynamodb.util.getAttributeMap
import br.com.itau.challenge.balance.adapter.output.dynamodb.util.toAccount
import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.CancellationReason
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DynamoDbAdaptersTest {

    @Test
    fun `account writer builds conditional versioned upsert`() {
        val client = mock(DynamoDbClient::class.java)
        AccountWriter("accounts", client).updateBalance(account())

        val request = captureTransactionRequest(client)
        val put = request.transactItems().single().put()
        assertEquals("accounts", put.tableName())
        assertEquals("attribute_not_exists(id) OR updatedAt < :updatedAtMessage", put.conditionExpression())
        assertEquals(account().updatedAt.toString(), put.expressionAttributeValues()[":updatedAtMessage"]?.n())
        assertEquals(ACCOUNT_ID.toString(), put.item()["id"]?.s())
        assertEquals("183.12", put.item()["balance"]?.n())
    }

    @Test
    fun `account writer ignores only conditional cancellation`() {
        val client = mock(DynamoDbClient::class.java)
        doThrow(cancellation("ConditionalCheckFailed"))
            .`when`(client).transactWriteItems(any(TransactWriteItemsRequest::class.java))

        assertDoesNotThrow { AccountWriter("accounts", client).updateBalance(account()) }
    }

    @Test
    fun `account writer propagates non conditional cancellation`() {
        val client = mock(DynamoDbClient::class.java)
        val failure = cancellation("ValidationError")
        doThrow(failure).`when`(client).transactWriteItems(any(TransactWriteItemsRequest::class.java))

        assertSame(
            failure,
            assertThrows<TransactionCanceledException> {
                AccountWriter("accounts", client).updateBalance(account())
            },
        )
    }

    @Test
    fun `transaction writer builds idempotent put`() {
        val client = mock(DynamoDbClient::class.java)
        TransactionWriter("transactions", "transaction_idempotency", 86_400, client)
            .registerTransaction(transaction())

        val puts = captureTransactionRequest(client).transactItems().map { it.put() }
        val transactionPut = puts[0]
        val idempotencyPut = puts[1]

        assertEquals("transactions", transactionPut.tableName())
        assertEquals("attribute_not_exists(id)", transactionPut.conditionExpression())
        assertEquals(TRANSACTION_ID.toString(), transactionPut.item()["id"]?.s())
        assertEquals(ACCOUNT_ID.toString(), transactionPut.item()["accountId"]?.s())
        assertEquals("97.07", transactionPut.item()["amount"]?.n())

        assertEquals("transaction_idempotency", idempotencyPut.tableName())
        assertEquals(TRANSACTION_ID.toString(), idempotencyPut.item()["id"]?.s())
        assertTrue(idempotencyPut.item()["expiresAt"]!!.n().toLong() > 0)
        assertEquals("attribute_not_exists(id) OR expiresAt < :now", idempotencyPut.conditionExpression())
    }

    @Test
    fun `transaction writer handles idempotency and propagates other failures`() {
        val duplicateClient = mock(DynamoDbClient::class.java)
        doThrow(cancellation(null, "ConditionalCheckFailed"))
            .`when`(duplicateClient).transactWriteItems(any(TransactWriteItemsRequest::class.java))
        assertDoesNotThrow {
            TransactionWriter("transactions", "transaction_idempotency", 86_400, duplicateClient)
                .registerTransaction(transaction())
        }

        val failingClient = mock(DynamoDbClient::class.java)
        val failure = cancellation(null)
        doThrow(failure)
            .`when`(failingClient).transactWriteItems(any(TransactWriteItemsRequest::class.java))
        assertSame(
            failure,
            assertThrows<TransactionCanceledException> {
                TransactionWriter("transactions", "transaction_idempotency", 86_400, failingClient)
                    .registerTransaction(transaction())
            },
        )
    }

    @Test
    fun `provider performs strongly consistent lookup and maps account`() {
        val client = mock(DynamoDbClient::class.java)
        `when`(client.getItem(any(GetItemRequest::class.java)))
            .thenReturn(GetItemResponse.builder().item(account().getAttributeMap()).build())

        val result = AccountProvider("accounts", client).getAccountWithBalance(ACCOUNT_ID)

        assertAccountEquals(account(), result)
        val captor = ArgumentCaptor.forClass(GetItemRequest::class.java)
        verify(client).getItem(captor.capture())
        assertEquals("accounts", captor.value.tableName())
        assertEquals(ACCOUNT_ID.toString(), captor.value.key()["id"]?.s())
        assertTrue(captor.value.consistentRead())
    }

    @Test
    fun `provider reports account not found`() {
        val client = mock(DynamoDbClient::class.java)
        `when`(client.getItem(any(GetItemRequest::class.java)))
            .thenReturn(GetItemResponse.builder().build())

        val exception = assertThrows<AccountNotFoundException> {
            AccountProvider("accounts", client).getAccountWithBalance(ACCOUNT_ID)
        }

        assertEquals("Account '$ACCOUNT_ID' not found", exception.message)
    }

    @Test
    fun `DynamoDB mappers preserve every persisted field`() {
        val accountAttributes = account().getAttributeMap()
        val transactionAttributes = transaction().getAttributeMap(ACCOUNT_ID.toString())

        assertEquals(setOf("id", "ownerId", "status", "balance", "createdAt", "updatedAt"), accountAttributes.keys)
        assertAccountEquals(account(), accountAttributes.toAccount())
        assertEquals(TRANSACTION_ID.toString(), transactionAttributes["id"]?.s())
        assertEquals("CREDIT", transactionAttributes["type"]?.s())
        assertEquals("BRL", transactionAttributes["currency"]?.s())
        assertEquals("APPROVED", transactionAttributes["status"]?.s())
        assertEquals(transaction().timestamp.toString(), transactionAttributes["timestamp"]?.n())
    }

    @Test
    fun `account mapper rejects each missing DynamoDB attribute`() {
        val valid = account().getAttributeMap()
        valid.keys.forEach { field ->
            val exception = assertThrows<InvalidDbParameterException> {
                (valid - field).toAccount()
            }
            assertTrue(exception.message!!.contains(field))
        }
    }

    private fun captureTransactionRequest(client: DynamoDbClient): TransactWriteItemsRequest {
        val captor = ArgumentCaptor.forClass(TransactWriteItemsRequest::class.java)
        verify(client).transactWriteItems(captor.capture())
        return captor.value
    }

    private fun assertAccountEquals(expected: Account, actual: Account) {
        assertEquals(expected.id, actual.id)
        assertEquals(expected.ownerId, actual.ownerId)
        assertEquals(expected.status, actual.status)
        assertEquals(expected.balance.amount, actual.balance.amount)
        assertEquals(expected.balance.currency, actual.balance.currency)
        assertEquals(expected.createdAt, actual.createdAt)
        assertEquals(expected.updatedAt, actual.updatedAt)
    }

    private fun cancellation(vararg codes: String?): TransactionCanceledException =
        TransactionCanceledException.builder()
            .message("cancelled")
            .cancellationReasons(codes.map { code -> CancellationReason.builder().code(code).build() })
            .build()
}
