package com.github.gkdrateit.service

import com.github.gkdrateit.config.RateItConfig
import com.github.gkdrateit.database.User
import com.github.gkdrateit.database.Users
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class EmailVerificationController : CrudApiBase() {
    data class Code(val code: String, val created: Long) {
        constructor(code: String) : this(code, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
    }

    // Verification codes are not stored into databases.
    companion object {
        val props = Properties()
        val auth = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(RateItConfig.maintainerEmailAddr, RateItConfig.maintainerEmailPassword)
            }
        }
        val tempCodes = ConcurrentHashMap<String, Code>()

        const val UCAS_EMAIL_SUFFIX = "@mails.ucas.ac.cn"

        init {
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.host"] = RateItConfig.maintainerEmailSmtpHostName
            props["mail.smtp.port"] = RateItConfig.maintainerEmailSmtpHostPort.toString()
        }
    }


    override val path: String
        get() = "email-verification"

    override fun handleCreate(ctx: Context): ApiResponse<*> {
        val email = ctx.formParam("email") ?: return missingParamError("email")
        if (!email.endsWith(UCAS_EMAIL_SUFFIX)) {
            return emailIllegalError()
        }

        if (!transaction {
                User.find { Users.email eq email }.empty()
            }) {
            return error("User registered")
        }

        val code = (1..6).map { ('0'..'9').random() }.joinToString("")
        tempCodes[email] = Code(code)
        logger.info("Create code $code for $email")

        // Send email verification code.
        val session = Session.getInstance(props, auth)

        try {
            val msg = MimeMessage(session)
            msg.setFrom(InternetAddress(RateItConfig.maintainerEmailAddr))
            val address = arrayOf(InternetAddress(email))
            msg.setRecipients(Message.RecipientType.TO, address)
            msg.subject = "UCAS Rate It Email Verification"
            msg.addHeader("x-cloudmta-class", "standard")
            msg.setText(code)
            Transport.send(msg)
        } catch (ex: Throwable) {
            return error(ex.message!!)
        }

        return success()
    }

    override fun handleRead(ctx: Context): ApiResponse<*> {
        return notImplementedError()
    }

    override fun handleUpdate(ctx: Context): ApiResponse<*> {
        return notImplementedError()
    }

    override fun handleDelete(ctx: Context): ApiResponse<*> {
        return notImplementedError()
    }
}