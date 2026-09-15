package br.com.itau.challenge.hello.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Instant

data class Account(
    val id: UUID,
    val ownerId: UUID,
    val status: AccountStatusEnum,
    val balance: Balance,
    val createdAt: Instant,
    val updatedAt: Instant,
)