package br.com.itau.challenge.hello.adapter.input.web.dto

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime
import java.util.UUID

data class AccountBalanceResponse(
    val id: UUID,
    val owner: UUID,
    val balance: BalanceResponse,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val updatedAt: LocalDateTime,
)