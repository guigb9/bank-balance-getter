package br.com.itau.challenge.balance.adapter.output.dynamodb

import br.com.itau.challenge.balance.adapter.output.dynamodb.util.getAttributeMap
import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.port.output.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException

@Service
class AccountWriter(
    @Value("\${dynamodb.account-table-name}") private val accountTableName: String,
    private val dynamoDbClient: DynamoDbClient,
) : AccountRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun updateBalance(account: Account) {
        logger.info("Processing conditional upsert for account: ${account.id}")


        val accountPut = account.toUpsertRequest(accountTableName)

        val request = TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .put(accountPut)
                    .build()
            )
            .build()

        try {
            dynamoDbClient.transactWriteItems(request)
            logger.info("Account ${account.id} updated/created with success")
        } catch (e: TransactionCanceledException) {
            val reason = e.cancellationReasons().firstOrNull()?.code()

            if (reason == "ConditionalCheckFailed") {
                logger.warn(
                    "[Indepotency] Ignoring updating/creation of account ${account.id}"
                )
                return
            }
            throw e
        }
    }
}


fun Account.toUpsertRequest(tableName: String) : Put {
    return Put.builder()
        .tableName(tableName)
        .item(getAttributeMap())
        .conditionExpression("attribute_not_exists(id) OR updatedAt < :updatedAtMessage")
        .expressionAttributeValues(
            mapOf(
                ":updatedAtMessage" to AttributeValue.builder().n(updatedAt.toString()).build()
            )
        )
        .build()
}