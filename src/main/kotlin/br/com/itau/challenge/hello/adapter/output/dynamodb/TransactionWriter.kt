package br.com.itau.challenge.hello.adapter.output.dynamodb

import br.com.itau.challenge.hello.adapter.output.dynamodb.util.getAttributeMap
import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.domain.model.Transaction
import br.com.itau.challenge.hello.port.output.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException

@Service
class TransactionWriter(

    @Value("\${dynamodb.transaction-table-name}") private val transactionTableName: String,
    private val dynamoDbClient: DynamoDbClient,
) : TransactionRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun registerTransaction(newTransaction: Transaction) {
        logger.info("Registering transaction ${newTransaction.id}")

        val transactionPut = newTransaction
            .toPutRequest(transactionTableName)

        val request = TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .put(transactionPut)
                    .build()
            )
            .build()

        try {
            dynamoDbClient.transactWriteItems(request)
            logger.info("Transação ${newTransaction.id} criada com sucesso.")

        } catch (e: TransactionCanceledException) {
            val reason = e.cancellationReasons().firstOrNull()?.code()

            if (reason == "ConditionalCheckFailed") {
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




