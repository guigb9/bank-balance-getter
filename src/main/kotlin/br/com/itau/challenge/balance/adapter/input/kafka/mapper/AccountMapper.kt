package br.com.itau.challenge.balance.adapter.input.kafka.mapper

import br.com.itau.challenge.balance.adapter.input.kafka.dto.AccountMessage
import br.com.itau.challenge.balance.adapter.input.kafka.exception.InvalidMessageFormatException
import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.domain.model.AccountStatusEnum
import br.com.itau.challenge.balance.domain.model.Balance

import java.util.UUID

//TODO atualizar a exception

fun AccountMessage.toEntity(updatedAt: Long?) = Account(
    id = id?.let { UUID.fromString(it) } ?: throw InvalidMessageFormatException("id"),
    ownerId = owner?.let { UUID.fromString(it) } ?: throw InvalidMessageFormatException("ownerId"),
    status = status?.let { AccountStatusEnum.valueOf(it) } ?: throw InvalidMessageFormatException("status"),
    balance = balance?.let {
        Balance(
            amount = it.amount ?: throw InvalidMessageFormatException("balance.amount"),
            currency = it.currency ?: throw InvalidMessageFormatException("balance.amount")
        )
    } ?: throw InvalidMessageFormatException("balance"),
    createdAt = createdAt ?: throw InvalidMessageFormatException("createdAt"),
    updatedAt = updatedAt ?: throw InvalidMessageFormatException("updatedAt")
)