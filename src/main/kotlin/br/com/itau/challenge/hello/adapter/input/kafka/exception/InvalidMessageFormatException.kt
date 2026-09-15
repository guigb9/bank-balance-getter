package br.com.itau.challenge.hello.adapter.input.kafka.exception

class InvalidMessageFormatException(field: String) : RuntimeException("Field '$field' is not a valid format")