package br.com.itau.challenge.balance.adapter.input.kafka.mapper

import br.com.itau.challenge.balance.adapter.input.kafka.dto.TransactionMessage
import br.com.itau.challenge.balance.domain.model.Transaction
import br.com.itau.challenge.balance.domain.model.TransactionStatusEnum
import br.com.itau.challenge.balance.domain.model.TransactionTypeEnum
import java.security.InvalidParameterException
import java.util.Currency
import java.util.UUID

fun TransactionMessage.toEntity(accountId: String?) = Transaction(
    id = id?.let { UUID.fromString(it) } ?: throw InvalidParameterException(),
    type = type?.let { TransactionTypeEnum.valueOf(it) }  ?: throw InvalidParameterException(),
    amount = amount?.let { it } ?: throw InvalidParameterException(),
    currency = currency?.let { Currency.getInstance(it) } ?: throw InvalidParameterException(),
    status = status?.let { TransactionStatusEnum.valueOf(it) } ?: throw InvalidParameterException(),
    timestamp = timestamp ?: throw InvalidParameterException(),
    accountId = accountId?.let { UUID.fromString(it) } ?: throw InvalidParameterException()
)
