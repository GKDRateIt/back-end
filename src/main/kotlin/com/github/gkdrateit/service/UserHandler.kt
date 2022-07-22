package com.github.gkdrateit.service

import com.github.gkdrateit.database.User
import com.github.gkdrateit.database.UserModel
import com.github.gkdrateit.database.Users
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserHandler : CrudApiBase() {
    override val path: String
        get() = "/user"

    override fun handleCreate(param: Map<String, String>): ApiResponse<String> {
        arrayOf("email", "hashedPassword", "nickname", "startYear", "group").forEach { key ->
            if (param[key] == null) {
                return missingParamError(key)
            }
        }

        val nickNameDec = try {
            String(base64Decoder.decode(param["nickname"]))
        } catch (e: IllegalArgumentException) {
            return base64Error("nickname")
        }

        return try {
            transaction {
                User.new {
                    email = param["email"]!!
                    hashedPassword = param["hashedPassword"]!!
                    nickname = nickNameDec
                    startYear = param["startYear"]!!
                    group = param["group"]!!
                }
            }
            success()
        } catch (e: Throwable) {
            databaseError(e.message ?: "")
        }
    }

    override fun handleRead(param: Map<String, String>): ApiResponse<List<UserModel>> {
        val query = Users.selectAll()
        param["nickname"]?.let {
            query.andWhere { Users.nickname like "$it%" }
        }
        param["email"]?.let {
            val prefix = it.substringBefore('@')
            val postfix = it.substringAfter('@')
            query.andWhere { Users.email like "$prefix%@$postfix" }
        }
        transaction {
            query.map { User.wrapRow(it).toModel() }
        }.let {
            return successReply(it)
        }
    }

    override fun handleUpdate(param: Map<String, String>): ApiResponse<String> {
        return notImplementedError()
    }

    override fun handleDelete(param: Map<String, String>): ApiResponse<String> {
        return notImplementedError()
    }
}