package br.com.itau.challenge.balance.domain.model

import java.util.UUID

data class Account(
    val id: UUID,
    val ownerId: UUID,
    val status: AccountStatusEnum,
    val balance: Balance,
    val createdAt: Long,
    val updatedAt: Long,
)