package com.github.gkdrateit.service.course

import com.github.gkdrateit.database.Course
import com.github.gkdrateit.database.CourseModel
import com.github.gkdrateit.database.Courses
import com.github.gkdrateit.database.Teacher
import com.github.gkdrateit.service.ApiResponse
import com.github.gkdrateit.service.ResponseStatus
import io.javalin.testtools.JavalinTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Request
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class Read : TestBase() {
    @Test
    fun read() = JavalinTest.test(apiServer.app) { server, client ->
        val qTeacherId = transaction { Teacher.all().first().id.value }
        // Create some course entries
        if (transaction { Course.find { Courses.name like "测试课程%" }.empty() }) {
            transaction {
                Course.new {
                    code = "000000000"
                    codeSeq = "A"
                    name = "测试课程-1"
                    teacherId = qTeacherId
                    semester = "spring"
                    credit = BigDecimal.valueOf(1.5)
                    degree = 0
                }
                Course.new {
                    code = "000000001"
                    codeSeq = "B"
                    name = "测试课程-2"
                    teacherId = qTeacherId
                    semester = "spring"
                    credit = BigDecimal.valueOf(1.5)
                    degree = 0
                }
            }
        }
        val formBody = FormBody.Builder()
            .add("_action", "read")
            .add("name", "测试")
            .build()
        val req = Request.Builder()
            .url("http://localhost:${server.port()}/api/course")
            .post(formBody)
            .build()
        client.request(req).use {
            val bodyStr = it.body!!.string()
            assertEquals(it.code, 200, bodyStr)
            val body = Json.decodeFromString<ApiResponse<List<CourseModel>>>(bodyStr)
            assertEquals(body.status, ResponseStatus.SUCCESS, bodyStr)
            body.data!!.forEach {
                assertTrue {
                    it.name.startsWith("测试")
                }
            }
        }
        // Delete them
        transaction {
            Courses.deleteWhere { Courses.name like "测试课程%" }
        }
        assertTrue {
            transaction { Course.find { Courses.name like "测试课程%" }.empty() }
        }
    }
}