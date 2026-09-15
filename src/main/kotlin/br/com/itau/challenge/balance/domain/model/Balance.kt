package br.com.itau.challenge.balance.domain.model

import java.math.BigDecimal

class Balance(
    val currency: String,
    val amount: BigDecimal,
)