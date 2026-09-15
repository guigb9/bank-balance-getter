package br.com.itau.challenge.hello.domain.model

import java.math.BigDecimal

class Balance(
    val currency: String,
    val amount: BigDecimal,
)