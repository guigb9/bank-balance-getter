package br.com.itau.challenge.balance.adapter.output.dynamodb.exception

class InvalidDbParameterException(parameterName: String) : RuntimeException("Invalid parameter $parameterName")