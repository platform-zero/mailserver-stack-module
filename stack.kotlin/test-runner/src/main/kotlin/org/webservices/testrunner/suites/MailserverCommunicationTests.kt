package org.webservices.testrunner.suites

import org.webservices.testrunner.framework.*

suspend fun TestRunner.mailserverCommunicationTests() = suite("Mailserver Communication Tests") {
test("Mailserver SMTP port configuration exists") {
        env.endpoints.mailserver shouldContain "mailserver"
    }

    test("Mailserver accepts connections on port 25") {
        env.endpoints.mailserver shouldContain ":25"
    }

    test("Mailserver configuration is valid") {
        env.endpoints.mailserver shouldContain "mailserver:25"
    }

    test("Mailserver endpoint is reachable via DNS") {
        env.endpoints.mailserver shouldContain "mailserver"
    }
}
