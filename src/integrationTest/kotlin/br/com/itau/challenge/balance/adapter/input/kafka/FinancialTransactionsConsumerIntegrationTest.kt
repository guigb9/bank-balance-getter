package br.com.itau.challenge.balance.adapter.input.kafka

import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.domain.model.Transaction
import br.com.itau.challenge.balance.domain.model.TransactionStatusEnum
import br.com.itau.challenge.balance.domain.model.TransactionTypeEnum
import br.com.itau.challenge.balance.port.input.FinancialTransactionProcessorUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val TOPIC = "transacoes-financeiras-processadas"

@SpringBootTest(
    properties = [
        "spring.kafka.consumer.group-id=financial-transactions-integration-test",
        "spring.kafka.consumer.auto-offset-reset=latest",
        "financial-processed-transactions.concurrency=1",
    ],
)
@Import(KafkaIntegrationTestConfig::class)
class FinancialTransactionsConsumerIntegrationTest {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<Any, Any>

    @Autowired
    private lateinit var recordingUseCase: RecordingFinancialTransactionProcessor

    @MockitoBean
    private lateinit var dynamoDbClient: DynamoDbClient

    @BeforeEach
    fun clearPreviousMessages() {
        recordingUseCase.clear()
    }

    @Test
    fun `consumer receives and maps financial transaction from real broker`() {
        val accountId = "83dc04a4-f694-4f9c-8f30-68407ab6403a"
        val ownerId = UUID.randomUUID()
        val transactionId = UUID.randomUUID()
        val timestamp = 1_751_641_364_589_998L
        val payload =
            """
            {
              "transaction": {
                "id": "$transactionId",
                "type": "CREDIT",
                "amount": 97.07,
                "currency": "BRL",
                "status": "APPROVED",
                "timestamp": $timestamp
              },
              "account": {
                "id": "$accountId",
                "owner": "$ownerId",
                "created_at": 1634874339000000,
                "status": "ENABLED",
                "balance": {
                  "amount": 183.12,
                  "currency": "BRL"
                }
              }
            }
            """.trimIndent()

        kafkaTemplate.send(TOPIC, transactionId.toString(), payload).get(10, TimeUnit.SECONDS)

        val processed = assertNotNull(recordingUseCase.awaitMessage())
        val account = processed.first
        val transaction = processed.second
        assertEquals(accountId, account.id.toString())
        assertEquals(ownerId, account.ownerId)
        assertEquals(BigDecimal("183.12"), account.balance.amount)
        assertEquals(timestamp, account.updatedAt)
        assertEquals(transactionId, transaction.id)
        assertEquals(accountId, transaction.accountId.toString())
        assertEquals(TransactionTypeEnum.CREDIT, transaction.type)
        assertEquals(TransactionStatusEnum.APPROVED, transaction.status)
        assertEquals(BigDecimal("97.07"), transaction.amount)
    }
}

@TestConfiguration(proxyBeanMethods = false)
class KafkaIntegrationTestConfig {

    @Bean
    @Primary
    fun recordingFinancialTransactionProcessor() = RecordingFinancialTransactionProcessor()
}

class RecordingFinancialTransactionProcessor : FinancialTransactionProcessorUseCase {

    private val messages = LinkedBlockingQueue<Pair<Account, Transaction>>()

    override fun processTransaction(
        account: Account,
        transaction: Transaction,
    ) {
        messages.offer(account to transaction)
    }

    fun awaitMessage(): Pair<Account, Transaction>? = messages.poll(15, TimeUnit.SECONDS)

    fun clear() {
        messages.clear()
    }
}
