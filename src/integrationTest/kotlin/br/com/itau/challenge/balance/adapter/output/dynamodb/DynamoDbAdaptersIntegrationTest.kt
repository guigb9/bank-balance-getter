package br.com.itau.challenge.balance.adapter.output.dynamodb

import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.domain.model.AccountStatusEnum
import br.com.itau.challenge.balance.domain.model.Balance
import br.com.itau.challenge.balance.domain.model.Transaction
import br.com.itau.challenge.balance.domain.model.TransactionStatusEnum
import br.com.itau.challenge.balance.domain.model.TransactionTypeEnum
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.math.BigDecimal
import java.net.URI
import java.util.Currency
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SEEDED_ACCOUNT_ID = "5b19c8b6-0cc4-4c72-a989-0c2ee15fa975"
private const val SEEDED_OWNER_ID = "315e3cfe-f4af-4cd2-b298-a449e614349a"
private const val SEEDED_TRANSACTION_ID = "8e8ae808-b154-48b5-9f3e-553935cc4543"
private const val SEEDED_TIMESTAMP = 1_751_641_364_589_998L

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamoDbAdaptersIntegrationTest {

    private val accountTable = System.getenv("DYNAMODB_ACCOUNT_TABLE_NAME") ?: "accounts"
    private val transactionTable = System.getenv("DYNAMODB_TRANSACTION_TABLE_NAME") ?: "transactions"
    private val idempotencyTable =
        System.getenv("DYNAMODB_TRANSACTION_IDEMPOTENCY_TABLE_NAME") ?: "transaction_idempotency"

    private lateinit var dynamoDbClient: DynamoDbClient

    @BeforeAll
    fun setUp() {
        dynamoDbClient = DynamoDbClient.builder()
            .endpointOverride(URI.create(System.getenv("DYNAMODB_ENDPOINT") ?: "http://localhost:8000"))
            .region(Region.of(System.getenv("DYNAMODB_REGION") ?: "us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")),
            )
            .build()
    }

    @AfterAll
    fun tearDown() {
        dynamoDbClient.close()
    }

    @Test
    fun `provider reads seeded account and writer ignores an older balance`() {
        val accountId = UUID.fromString(SEEDED_ACCOUNT_ID)
        val provider = AccountProvider(accountTable, dynamoDbClient)
        val writer = AccountWriter(accountTable, dynamoDbClient)

        val seededAccount = provider.getAccountWithBalance(accountId)

        assertEquals(accountId, seededAccount.id)
        assertEquals(UUID.fromString(SEEDED_OWNER_ID), seededAccount.ownerId)
        assertEquals(AccountStatusEnum.ENABLED, seededAccount.status)
        assertEquals(0, BigDecimal("183.12").compareTo(seededAccount.balance.amount))
        assertEquals("BRL", seededAccount.balance.currency)
        assertEquals(SEEDED_TIMESTAMP, seededAccount.updatedAt)

        writer.updateBalance(
            Account(
                id = accountId,
                ownerId = UUID.fromString(SEEDED_OWNER_ID),
                status = AccountStatusEnum.ENABLED,
                balance = Balance(currency = "BRL", amount = BigDecimal.ZERO),
                createdAt = 1_634_874_339_000_000,
                updatedAt = SEEDED_TIMESTAMP - 1,
            ),
        )

        val accountAfterOlderEvent = provider.getAccountWithBalance(accountId)
        assertEquals(0, BigDecimal("183.12").compareTo(accountAfterOlderEvent.balance.amount))
        assertEquals(SEEDED_TIMESTAMP, accountAfterOlderEvent.updatedAt)
    }

    @Test
    fun `seeded transaction and ttl marker prevent duplicate transaction id`() {
        val accountId = UUID.fromString(SEEDED_ACCOUNT_ID)
        val transactionId = UUID.fromString(SEEDED_TRANSACTION_ID)
        val writer = TransactionWriter(
            transactionTable,
            idempotencyTable,
            86_400,
            dynamoDbClient,
        )

        val seededTransactions = transactionsFor(accountId)
        assertEquals(1, seededTransactions.count())
        assertEquals(SEEDED_TRANSACTION_ID, seededTransactions.items().single()["id"]?.s())
        assertEquals(SEEDED_TIMESTAMP.toString(), seededTransactions.items().single()["timestamp"]?.n())

        val marker = dynamoDbClient.getItem(
            GetItemRequest.builder()
                .tableName(idempotencyTable)
                .key(
                    mapOf(
                        "id" to AttributeValue.builder().s(SEEDED_TRANSACTION_ID).build(),
                    ),
                )
                .consistentRead(true)
                .build(),
        )
        assertTrue(marker.hasItem())
        assertTrue(marker.item()["expiresAt"]!!.n().toLong() > 0)

        writer.registerTransaction(
            Transaction(
                id = transactionId,
                type = TransactionTypeEnum.CREDIT,
                amount = BigDecimal("999.99"),
                currency = Currency.getInstance("BRL"),
                status = TransactionStatusEnum.APPROVED,
                timestamp = SEEDED_TIMESTAMP + 1,
                accountId = accountId,
            ),
        )

        assertEquals(1, transactionsFor(accountId).count())
    }

    private fun transactionsFor(accountId: UUID) =
        dynamoDbClient.query(
            QueryRequest.builder()
                .tableName(transactionTable)
                .keyConditionExpression("accountId = :accountId")
                .expressionAttributeValues(
                    mapOf(
                        ":accountId" to AttributeValue.builder().s(accountId.toString()).build(),
                    ),
                )
                .consistentRead(true)
                .build(),
        )
}
