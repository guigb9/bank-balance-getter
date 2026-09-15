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
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import java.math.BigDecimal
import java.net.URI
import java.util.Currency
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamoDbAdaptersIntegrationTest {

    private val suffix = UUID.randomUUID().toString().replace("-", "")
    private val accountTable = "accounts_it_$suffix"
    private val transactionTable = "transactions_it_$suffix"
    private val idempotencyTable = "transaction_idempotency_it_$suffix"

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

        createTable(accountTable, "id" to ScalarAttributeType.S)
        createTable(
            transactionTable,
            "accountId" to ScalarAttributeType.S,
            "timestamp" to ScalarAttributeType.N,
        )
        createTable(idempotencyTable, "id" to ScalarAttributeType.S)
    }

    @AfterAll
    fun tearDown() {
        listOf(accountTable, transactionTable, idempotencyTable).forEach { tableName ->
            dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build())
        }
        dynamoDbClient.close()
    }

    @Test
    fun `account writer persists newest balance and provider reads it consistently`() {
        val accountId = UUID.randomUUID()
        val writer = AccountWriter(accountTable, dynamoDbClient)
        val provider = AccountProvider(accountTable, dynamoDbClient)

        writer.updateBalance(account(accountId, updatedAt = 2, amount = "200.00"))
        writer.updateBalance(account(accountId, updatedAt = 1, amount = "100.00"))

        val persisted = provider.getAccountWithBalance(accountId)

        assertEquals(0, BigDecimal("200.00").compareTo(persisted.balance.amount))
        assertEquals(2, persisted.updatedAt)
    }

    @Test
    fun `transaction writer stores one transaction for duplicate id with different timestamp`() {
        val accountId = UUID.randomUUID()
        val transactionId = UUID.randomUUID()
        val writer = TransactionWriter(
            transactionTable,
            idempotencyTable,
            86_400,
            dynamoDbClient,
        )

        writer.registerTransaction(transaction(transactionId, accountId, timestamp = 1_000))
        writer.registerTransaction(transaction(transactionId, accountId, timestamp = 2_000))

        val transactions = dynamoDbClient.query(
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

        assertEquals(1, transactions.count())
        assertEquals("1000", transactions.items().single()["timestamp"]?.n())

        val marker = dynamoDbClient.getItem(
            GetItemRequest.builder()
                .tableName(idempotencyTable)
                .key(
                    mapOf(
                        "id" to AttributeValue.builder().s(transactionId.toString()).build(),
                    ),
                )
                .consistentRead(true)
                .build(),
        )

        assertTrue(marker.hasItem())
        assertTrue(marker.item()["expiresAt"]!!.n().toLong() > 0)
    }

    private fun createTable(
        tableName: String,
        partitionKey: Pair<String, ScalarAttributeType>,
        sortKey: Pair<String, ScalarAttributeType>? = null,
    ) {
        val attributes = buildList {
            add(attribute(partitionKey))
            sortKey?.let { add(attribute(it)) }
        }
        val keys = buildList {
            add(key(partitionKey.first, KeyType.HASH))
            sortKey?.let { add(key(it.first, KeyType.RANGE)) }
        }

        dynamoDbClient.createTable(
            CreateTableRequest.builder()
                .tableName(tableName)
                .attributeDefinitions(attributes)
                .keySchema(keys)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build(),
        )
    }

    private fun attribute(definition: Pair<String, ScalarAttributeType>) =
        AttributeDefinition.builder()
            .attributeName(definition.first)
            .attributeType(definition.second)
            .build()

    private fun key(name: String, type: KeyType) =
        KeySchemaElement.builder()
            .attributeName(name)
            .keyType(type)
            .build()

    private fun account(
        id: UUID,
        updatedAt: Long,
        amount: String,
    ) = Account(
        id = id,
        ownerId = UUID.randomUUID(),
        status = AccountStatusEnum.ENABLED,
        balance = Balance(currency = "BRL", amount = BigDecimal(amount)),
        createdAt = 1,
        updatedAt = updatedAt,
    )

    private fun transaction(
        id: UUID,
        accountId: UUID,
        timestamp: Long,
    ) = Transaction(
        id = id,
        type = TransactionTypeEnum.CREDIT,
        amount = BigDecimal("10.00"),
        currency = Currency.getInstance("BRL"),
        status = TransactionStatusEnum.APPROVED,
        timestamp = timestamp,
        accountId = accountId,
    )
}
