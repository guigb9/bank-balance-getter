package br.com.itau.challenge.hello.port.output.data

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class AccountBalanceOutputPort(
    val accountId: UUID,
    val ownerId: UUID,
    val balance: BigDecimal,
    val updatedAt: LocalDateTime
)