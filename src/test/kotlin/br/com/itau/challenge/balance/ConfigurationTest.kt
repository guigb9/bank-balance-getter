package br.com.itau.challenge.balance

import br.com.itau.challenge.balance.adapter.input.kafka.config.JacksonConfig
import br.com.itau.challenge.balance.adapter.input.kafka.config.KafkaConfig
import br.com.itau.challenge.balance.adapter.output.dynamodb.config.DynamoDbConfig
import br.com.itau.challenge.balance.adapter.output.dynamodb.config.DynamoDbTableInitializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigurationTest {

    @Test
    fun `table initializer creates schemas and enables idempotency ttl`() {
        val client = mock(DynamoDbClient::class.java)

        DynamoDbTableInitializer(client, "accounts", "transactions", "transaction_idempotency").initializeTables()

        val captor = ArgumentCaptor.forClass(CreateTableRequest::class.java)
        verify(client, times(3)).createTable(captor.capture())
        val accountRequest = captor.allValues.first()
        val transactionRequest = captor.allValues[1]
        val idempotencyRequest = captor.allValues.last()
        assertEquals("accounts", accountRequest.tableName())
        assertEquals(listOf("id"), accountRequest.keySchema().map { it.attributeName() })
        assertEquals("transactions", transactionRequest.tableName())
        assertEquals(listOf("accountId", "timestamp"), transactionRequest.keySchema().map { it.attributeName() })
        assertEquals("transaction_idempotency", idempotencyRequest.tableName())
        assertEquals(listOf("id"), idempotencyRequest.keySchema().map { it.attributeName() })

        val ttlCaptor = ArgumentCaptor.forClass(UpdateTimeToLiveRequest::class.java)
        verify(client).updateTimeToLive(ttlCaptor.capture())
        assertEquals("transaction_idempotency", ttlCaptor.value.tableName())
        assertEquals("expiresAt", ttlCaptor.value.timeToLiveSpecification().attributeName())
        assertEquals(true, ttlCaptor.value.timeToLiveSpecification().enabled())
    }

    @Test
    fun `table initializer ignores tables that already exist`() {
        val client = mock(DynamoDbClient::class.java)
        doThrow(ResourceInUseException.builder().message("exists").build())
            .`when`(client).createTable(any(CreateTableRequest::class.java))

        assertDoesNotThrow {
            DynamoDbTableInitializer(client, "accounts", "transactions", "transaction_idempotency").initializeTables()
        }
        verify(client, times(3)).createTable(any(CreateTableRequest::class.java))
    }

    @Test
    fun `table initializer propagates unexpected DynamoDB failures`() {
        val client = mock(DynamoDbClient::class.java)
        val failure = IllegalStateException("unavailable")
        doThrow(failure).`when`(client).createTable(any(CreateTableRequest::class.java))

        assertEquals(
            failure,
            assertThrows<IllegalStateException> {
                DynamoDbTableInitializer(client, "accounts", "transactions", "transaction_idempotency").initializeTables()
            },
        )
    }

    @Test
    fun `DynamoDB config creates local client`() {
        val client = DynamoDbConfig().dynamoDbClient("http://localhost:8000", "us-east-1")
        try {
            assertNotNull(client)
        } finally {
            client.close()
        }
    }

    @Test
    fun `Jackson config creates Kotlin capable mapper`() {
        val mapper = JacksonConfig().objectMapper()
        val json = mapper.writeValueAsString(account())

        assertNotNull(mapper)
        assert(json.contains(ACCOUNT_ID.toString()))
    }

    @Test
    fun `Kafka config creates recoverer error handler and listener factory`() {
        @Suppress("UNCHECKED_CAST")
        val template = mock(KafkaTemplate::class.java) as KafkaTemplate<Any, Any>
        @Suppress("UNCHECKED_CAST")
        val consumerFactory = mock(ConsumerFactory::class.java) as ConsumerFactory<String, Any>
        val config = KafkaConfig(template)
        val handler = config.kafkaErrorHandler()

        assertNotNull(config.deadLetterPublishingRecoverer())
        assertNotNull(handler)
        assertNotNull(config.kafkaListenerContainerFactory(consumerFactory, handler))
    }
}
