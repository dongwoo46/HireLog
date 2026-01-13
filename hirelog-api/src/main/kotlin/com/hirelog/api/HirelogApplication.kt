package com.hirelog.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HirelogApplication

fun main(args: Array<String>) {
	runApplication<HirelogApplication>(*args)
}
