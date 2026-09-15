package br.com.itau.challenge.balance.adapter.output.dynamodb.config

import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory

@Configuration
class DynamoDbTableInitializer(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${dynamodb.account-table-name}") private val accountTableName: String,
    @Value("\${dynamodb.transaction-table-name}") private val transactionTableName: String
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun initializeTables() {
        createAccountTable()
        createTransactionTable()
    }

    private fun createAccountTable() {
        val request = CreateTableRequest.builder()
            .tableName(accountTableName)
            .attributeDefinitions(
                AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
            )
            .keySchema(
                KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build()
            )
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build()

        executeCreation(accountTableName, request)
    }

    private fun createTransactionTable() {
        val request = CreateTableRequest.builder()
            .tableName(transactionTableName)
            .attributeDefinitions(
                AttributeDefinition.builder().attributeName("accountId").attributeType(ScalarAttributeType.S).build(),
                AttributeDefinition.builder().attributeName("timestamp").attributeType(ScalarAttributeType.N).build()
            )
            .keySchema(
                KeySchemaElement.builder().attributeName("accountId").keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName("timestamp").keyType(KeyType.RANGE).build()
            )
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build()

        executeCreation(transactionTableName, request)
    }

    private fun executeCreation(tableName: String, request: CreateTableRequest) {
        try {
            dynamoDbClient.createTable(request)
            logger.info("Tabela '$tableName' criada com sucesso no DynamoDB!")
        } catch (e: ResourceInUseException) {
            logger.info("A tabela '$tableName' já existe. Ignorando criação.")
        } catch (e: Exception) {
            logger.error("Erro inesperado ao tentar criar a tabela '$tableName'", e)
            throw e
        }
    }
}
