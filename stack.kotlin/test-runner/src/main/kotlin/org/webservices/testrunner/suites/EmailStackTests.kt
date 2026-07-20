package org.webservices.testrunner.suites

import io.ktor.client.statement.*
import io.ktor.http.*
import org.webservices.testrunner.framework.*
import java.net.Socket
import java.net.InetSocketAddress
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

suspend fun TestRunner.emailStackTests() = suite("Email Stack Tests") {

    // Mailserver connectivity tests
    test("Mailserver: SMTP port is reachable") {
        val parts = endpoints.mailserver.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 25

        try {
            Socket(host, port).use { socket ->
                socket.isConnected shouldBe true
                println("      ✓ Connected to SMTP server at $host:$port")
            }
        } catch (e: Exception) {
            throw AssertionError("Cannot connect to SMTP server at $host:$port: ${e.message}")
        }
    }

    test("Mailserver: SMTP greeting is valid") {
        val parts = endpoints.mailserver.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 25

        try {
            Socket(host, port).use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val greeting = reader.readLine()

                greeting shouldContain "220"
                greeting shouldContain "SMTP"
                println("      ✓ SMTP greeting: $greeting")
            }
        } catch (e: Exception) {
            fail("SMTP greeting check failed: ${e.message}")
        }
    }

    test("Mailserver: EHLO command accepted") {
        val parts = endpoints.mailserver.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 25

        try {
            Socket(host, port).use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                reader.readLine() // Consume greeting
                writer.println("EHLO test.webservices.local")

                val response = reader.readLine()
                response shouldContain "250"
                println("      ✓ EHLO command accepted")
            }
        } catch (e: Exception) {
            fail("EHLO test failed: ${e.message}")
        }
    }

    test("Mailserver: STARTTLS capability available") {
        val parts = endpoints.mailserver.split(":")
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 25

        try {
            Socket(host, port).use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                reader.readLine() // Consume greeting
                writer.println("EHLO test.webservices.local")

                var hasStartTls = false
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("250 ") || line!!.contains("250-")) {
                        if (line!!.contains("STARTTLS")) {
                            hasStartTls = true
                        }
                        if (!line!!.contains("250-")) break
                    }
                }

                if (hasStartTls) {
                    println("      ✓ STARTTLS capability available")
                } else {
                    fail("STARTTLS not advertised by SMTP server at $host:$port")
                }
            }
        } catch (e: Exception) {
            fail("STARTTLS check failed: ${e.message}")
        }
    }

    test("Mailserver: Submission port (587) is accessible") {
        val parts = endpoints.mailserver.split(":")
        val host = parts[0]

        try {
            Socket(host, 587).use { socket ->
                socket.isConnected shouldBe true
                println("      ✓ Submission port 587 is accessible")
            }
        } catch (e: Exception) {
            fail("Submission port 587 not accessible: ${e.message}")
        }
    }

    test("Email Stack: SMTP and IMAP ports are distinct") {
        val mailParts = endpoints.mailserver.split(":")
        val mailHost = mailParts[0]

        require(25 != 993) { "SMTP and secure IMAP ports must remain distinct" }

        try {
            Socket(mailHost, 25).use { socket ->
                socket.isConnected shouldBe true
            }
            Socket(mailHost, 993).use { socket ->
                socket.isConnected shouldBe true
            }
            println("      ✓ SMTP and secure IMAP ports are reachable on distinct ports")
        } catch (e: Exception) {
            fail("SMTP/IMAP reachability check failed: ${e.message}")
        }
    }

    test("Email Stack: Secure SMTP (465) availability") {
        val mailParts = endpoints.mailserver.split(":")
        val mailHost = mailParts[0]

        try {
            Socket(mailHost, 465).use { socket ->
                socket.isConnected shouldBe true
                println("      ✓ Secure SMTP port 465 is accessible")
            }
        } catch (e: Exception) {
            fail("Secure SMTP port 465 is not accessible: ${e.message}")
        }
    }

    test("Email Stack: plaintext IMAP port (143) is not exposed") {
        val mailParts = endpoints.mailserver.split(":")
        val mailHost = mailParts[0]

        val plaintextImapReachable = runCatching {
            Socket().use { socket -> socket.connect(InetSocketAddress(mailHost, 143), 2000) }
        }.isSuccess
        require(!plaintextImapReachable) { "Plaintext IMAP port 143 must not be exposed" }
        println("      ✓ Plaintext IMAP port 143 is not exposed")
    }

    test("Email Stack: Secure IMAP (993) availability") {
        val mailParts = endpoints.mailserver.split(":")
        val mailHost = mailParts[0]

        try {
            Socket(mailHost, 993).use { socket ->
                socket.isConnected shouldBe true
                println("      ✓ Secure IMAP port 993 is accessible")
            }
        } catch (e: Exception) {
            fail("Secure IMAP port 993 is not accessible: ${e.message}")
        }
    }
}
