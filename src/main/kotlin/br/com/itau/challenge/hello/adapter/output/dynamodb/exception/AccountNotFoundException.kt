package br.com.itau.challenge.hello.adapter.output.dynamodb.exception

import java.util.UUID

class AccountNotFoundException(accountId: UUID) : RuntimeException("Account '$accountId' not found")