package br.com.itau.challenge.balance.adapter.input.web.mappers

import br.com.itau.challenge.balance.adapter.input.web.dto.AccountBalanceResponse
import br.com.itau.challenge.balance.adapter.input.web.dto.BalanceResponse
import br.com.itau.challenge.balance.domain.model.Account
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toJavaInstant

fun Account.toResponse() = AccountBalanceResponse(
    id = this.id,
    owner = this.ownerId,
    balance = BalanceResponse(
        currency = this.balance.currency,
        amount = this.balance.amount
    ),
    updatedAt = Instant.fromEpochMilliseconds(this.updatedAt/1000)
        .toJavaInstant()
        .atZone(ZoneId.of("America/Sao_Paulo"))
        .toLocalDateTime(),
)