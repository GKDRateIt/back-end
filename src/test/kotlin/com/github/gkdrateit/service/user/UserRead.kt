package com.github.gkdrateit.service.user

import com.github.gkdrateit.database.TestDbAdapter
import com.github.gkdrateit.database.User
import com.github.gkdrateit.database.Users
import com.github.gkdrateit.service.ApiServer
import io.javalin.testtools.JavalinTest
import okhttp3.FormBody
import okhttp3.Request
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserRead {
    private val apiServer = ApiServer()

    @BeforeAll
    fun setup() {
        TestDbAdapter.setup()
    }

    @Test
    fun read() = JavalinTest.test(apiServer.app) { server, client ->
        if (transaction { User.find { Users.nickname like "测试用户" }.empty() }) {
            val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            val randStr = (1..10)
                .map { allowedChars.random() }
                .joinToString("")
            transaction {
                User.new {
                    email = "test_user$randStr@ucas.ac.cn"
                    hashedPassword = "112233"
                    nickname = "测试用户"
                    startYear = "2020"
                    group = "default"
                }
            }
        }
        val body = FormBody.Builder()
            .add("_action", "read")
            .add("nickname", "测试")
            .build()
        val req = Request.Builder()
            .url("http://localhost:${server.port()}/api/user")
            .post(body)
            .build()
        client.request(req).use {
            val bodyStr = it.body!!.string()
            assertEquals(it.code, 200, bodyStr)
            assertTrue {
                bodyStr.contains("SUCCESS")
            }
            assertTrue {
                bodyStr.contains("测试用户")
            }
        }
    }


}