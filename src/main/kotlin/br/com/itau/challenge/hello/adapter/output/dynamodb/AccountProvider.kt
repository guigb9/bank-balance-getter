package br.com.itau.challenge.hello.adapter.output.dynamodb

import br.com.itau.challenge.hello.adapter.output.dynamodb.exception.AccountNotFoundException
import br.com.itau.challenge.hello.adapter.output.dynamodb.util.toAccount
import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.port.output.AccountBalanceProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import java.util.UUID

@Service
class AccountProvider(
    @Value("\${dynamodb.account-table-name}") private val accountTableName: String,
    private val dynamoDbClient: DynamoDbClient,
) : AccountBalanceProvider {


    override fun getAccountWithBalance(accountId: UUID) : Account {
        val request = GetItemRequest.builder()
            .tableName(accountTableName)
            .key(
                mapOf(
                    "id" to AttributeValue.builder()
                        .s(accountId.toString())
                        .build()
                )
            )
            .consistentRead(true)
            .build()

        val response = dynamoDbClient.getItem(request)

        if (!response.hasItem()) {
            throw AccountNotFoundException(accountId)
        }

        return response.item().toAccount()
    }
}