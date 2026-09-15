package br.com.itau.challenge.balance.adapter.output.dynamodb

import br.com.itau.challenge.balance.adapter.output.dynamodb.util.getAttributeMap
import br.com.itau.challenge.balance.domain.model.Transaction
import br.com.itau.challenge.balance.port.output.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException
import java.time.Instant

@Service
class TransactionWriter(

    @Value("\${dynamodb.transaction-table-name}") private val transactionTableName: String,
    @Value("\${dynamodb.transaction-idempotency-table-name}") private val transactionIdempotencyTableName: String,
    @Value("\${dynamodb.transaction-idempotency-ttl-seconds}") private val idempotencyTtlSeconds: Long,
    private val dynamoDbClient: DynamoDbClient,
) : TransactionRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun registerTransaction(newTransaction: Transaction) {
        logger.info("Registering transaction ${newTransaction.id}")

        val now = Instant.now().epochSecond
        val transactionPut = newTransaction.toPutRequest(transactionTableName)
        val idempotencyPut = newTransaction.toIdempotencyPutRequest(
            tableName = transactionIdempotencyTableName,
            now = now,
            expiresAt = now + idempotencyTtlSeconds,
        )

        val request = TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .put(transactionPut)
                    .build(),
                TransactWriteItem.builder()
                    .put(idempotencyPut)
                    .build(),
            )
            .build()

        try {
            dynamoDbClient.transactWriteItems(request)
            logger.info("Transação ${newTransaction.id} criada com sucesso.")

        } catch (e: TransactionCanceledException) {
            val idempotencyReason = e.cancellationReasons().getOrNull(1)?.code()

            if (idempotencyReason == "ConditionalCheckFailed") {
                logger.warn(
                    "[Indepotency] Ignoring transaction creation ${newTransaction.id}. "
                )
                return
            }
            throw e
        }
    }
}


fun Transaction.toPutRequest(tableName: String): Put {
    return Put.builder()
        .tableName(tableName)
        .item(getAttributeMap(accountId.toString()))
        .conditionExpression("attribute_not_exists(id)")
        .build()
}

fun Transaction.toIdempotencyPutRequest(
    tableName: String,
    now: Long,
    expiresAt: Long,
): Put {
    return Put.builder()
        .tableName(tableName)
        .item(
            mapOf(
                "id" to AttributeValue.builder().s(id.toString()).build(),
                "expiresAt" to AttributeValue.builder().n(expiresAt.toString()).build(),
            ),
        )
        .conditionExpression("attribute_not_exists(id) OR expiresAt < :now")
        .expressionAttributeValues(
            mapOf(
                ":now" to AttributeValue.builder().n(now.toString()).build(),
            ),
        )
        .build()
}

