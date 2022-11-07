package com.github.gkdrateit.config

import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths

object Config {
    var configFileName = run {
        val testConfigName = "test_config.json"
        val prodConfigName = "config.json"
        val testConfigFile = File(testConfigName)
        val prodConfigFile = File(prodConfigName)
        return@run if (testConfigFile.exists()) {
            testConfigName
        } else if (prodConfigFile.exists()) {
            prodConfigName
        } else {
            throw FileNotFoundException("Must provide `test_config.json` or `config.json`")
        }
    }
    private val configJson: Map<*, *> = run {
        ObjectMapper().readValue(Paths.get(configFileName).toFile(), Map::class.java)
    }

    val port: Int = run { configJson["port"]!! as Int }

    val algorithm: Algorithm = run { Algorithm.HMAC256(configJson["signSecret"]!! as String) }

    val maintainerEmailAddr: String = run { configJson["maintainerEmailAddr"]!! as String }
    val maintainerEmailPassword: String = run { configJson["maintainerEmailPassword"]!! as String }
    val maintainerEmailSmtpHostName: String = run { configJson["maintainerEmailSmtpHostName"]!! as String }
    val maintainerEmailSmtpHostPort: Int = run { configJson["maintainerEmailSmtpHostPort"]!! as Int }

    val databaseURL: String = run { configJson["databaseURL"]!! as String }
    val databaseDriver: String = run { configJson["databaseDriver"]!! as String }
    val databaseUser: String = run { configJson["databaseUser"]!! as String }
    val databasePassword: String = run { configJson["databasePassword"]!! as String }
}