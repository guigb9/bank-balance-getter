package br.com.itau.challenge.hello.adapter.output.dynamodb.util

import br.com.itau.challenge.hello.adapter.output.dynamodb.exception.InvalidDbParameterException
import br.com.itau.challenge.hello.domain.model.Account
import br.com.itau.challenge.hello.domain.model.AccountStatusEnum
import br.com.itau.challenge.hello.domain.model.Balance
import br.com.itau.challenge.hello.domain.model.Transaction
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Instant

fun Account.getAttributeMap(): Map<String, AttributeValue> {
    return mapOf(
        "id" to AttributeValue.builder().s(id.toString()).build(),
        "ownerId" to AttributeValue.builder().s(ownerId.toString()).build(),
        "status" to AttributeValue.builder().s(status.name).build(),
        "balance" to AttributeValue.builder().n(balance.amount.toPlainString()).build(),
        "createdAt" to AttributeValue.builder().n(createdAt.toString()).build(),
        "updatedAt" to AttributeValue.builder().n(updatedAt.toString()).build()
    )
}

fun Transaction.getAttributeMap(accountId: String): Map<String, AttributeValue> {
    return mapOf(
        "id" to AttributeValue.builder().s(id.toString()).build(),
        "type" to AttributeValue.builder().s(type.name).build(),
        "amount" to AttributeValue.builder().n(amount.toPlainString()).build(),
        "currency" to AttributeValue.builder().s(currency.toString()).build(),
        "status" to AttributeValue.builder().s(status.name).build(),
        "timestamp" to AttributeValue.builder().n(timestamp.toString()).build(),
        "accountId" to AttributeValue.builder().s(accountId).build(),
    )
}

fun Map<String, AttributeValue>.toAccount(): Account {
    return Account(
        id = this["id"]?.let {
            UUID.fromString(it.s())
        } ?: throw InvalidDbParameterException("id"),

        ownerId = this["ownerId"]?.let {
            UUID.fromString(it.s())
        } ?: throw InvalidDbParameterException("ownerId"),

        status = this["status"]?.let {
            AccountStatusEnum.valueOf(it.s())
        } ?: throw InvalidDbParameterException("status"),

        balance = this["balance"]?.let {
            Balance(
                amount = it.n().toBigDecimal(),
                currency = "BRL",
            )
        } ?: throw InvalidDbParameterException("balance"),

        updatedAt = this["updatedAt"]?.n()?.toLong() ?: throw InvalidDbParameterException("updatedAt"),

        createdAt = this["createdAt"]?.n()?.toLong() ?: throw InvalidDbParameterException("createdAt")
    )
}
