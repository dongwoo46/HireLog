package com.hirelog.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class HirelogApplication

fun main(args: Array<String>) {
	runApplication<HirelogApplication>(*args)
}
