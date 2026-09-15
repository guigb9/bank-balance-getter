package br.com.itau.challenge.hello.adapter.output.dynamodb.exception

class InvalidDbParameterException(parameterName: String) : RuntimeException("Invalid parameter $parameterName")