package br.com.itau.challenge.balance.adapter.input.kafka

import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.domain.model.Transaction
import br.com.itau.challenge.balance.domain.model.TransactionStatusEnum
import br.com.itau.challenge.balance.domain.model.TransactionTypeEnum
import br.com.itau.challenge.balance.port.input.FinancialTransactionProcessorUseCase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val SEEDED_KAFKA_ACCOUNT_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
private const val SEEDED_KAFKA_OWNER_ID = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
private const val SEEDED_KAFKA_TRANSACTION_ID = "11111111-1111-4111-8111-111111111111"
private const val SEEDED_KAFKA_TIMESTAMP = 1_751_641_364_590_000L

@SpringBootTest(
    properties = [
        "spring.kafka.consumer.group-id=financial-transactions-integration-test-\${random.uuid}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "financial-processed-transactions.concurrency=1",
    ],
)
@Import(KafkaIntegrationTestConfig::class)
class FinancialTransactionsConsumerIntegrationTest {

    @Autowired
    private lateinit var recordingUseCase: RecordingFinancialTransactionProcessor

    @MockitoBean
    private lateinit var dynamoDbClient: DynamoDbClient

    @Test
    fun `consumer receives and maps transaction published by seed`() {
        val transactionId = UUID.fromString(SEEDED_KAFKA_TRANSACTION_ID)
        val processed = assertNotNull(recordingUseCase.awaitTransaction(transactionId))
        val account = processed.first
        val transaction = processed.second
        assertEquals(UUID.fromString(SEEDED_KAFKA_ACCOUNT_ID), account.id)
        assertEquals(UUID.fromString(SEEDED_KAFKA_OWNER_ID), account.ownerId)
        assertEquals(0, BigDecimal("100.00").compareTo(account.balance.amount))
        assertEquals(SEEDED_KAFKA_TIMESTAMP, account.updatedAt)
        assertEquals(transactionId, transaction.id)
        assertEquals(UUID.fromString(SEEDED_KAFKA_ACCOUNT_ID), transaction.accountId)
        assertEquals(TransactionTypeEnum.CREDIT, transaction.type)
        assertEquals(TransactionStatusEnum.APPROVED, transaction.status)
        assertEquals(0, BigDecimal("100.00").compareTo(transaction.amount))
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

    fun awaitTransaction(transactionId: UUID): Pair<Account, Transaction>? {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)

        while (System.nanoTime() < deadline) {
            val remaining = deadline - System.nanoTime()
            val message = messages.poll(remaining, TimeUnit.NANOSECONDS) ?: return null
            if (message.second.id == transactionId) {
                return message
            }
        }

        return null
    }
}
