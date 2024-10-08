/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */
package io.airbyte.cdk.jdbc

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.spec.InvalidKeySpecException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SSLCertificateUtilsTest {
    @Throws(
        CertificateException::class,
        IOException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class
    )
    fun testkeyStoreFromCertificateInternal(
        certString: String,
        pwd: String,
        fs: FileSystem,
        directory: String
    ) {
        val ksUri = SSLCertificateUtils.keyStoreFromCertificate(certString, pwd, fs, directory)

        val ks = KeyStore.getInstance("PKCS12")
        val inputStream = Files.newInputStream(Path.of(ksUri))
        ks.load(inputStream, pwd.toCharArray())
        Assertions.assertEquals(1, ks.size())
        Files.delete(Path.of(ksUri))
    }

    @Test
    @Throws(
        CertificateException::class,
        IOException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class
    )
    fun testkeyStoreFromCertificate() {
        testkeyStoreFromCertificateInternal(
            caPem,
            KEY_STORE_PASSWORD,
            FileSystems.getDefault(),
            SLASH_TMP
        )

        val exception: Exception =
            Assertions.assertThrows(CertificateException::class.java) {
                testkeyStoreFromCertificateInternal(
                    caPem_Bad,
                    KEY_STORE_PASSWORD,
                    FileSystems.getDefault(),
                    SLASH_TMP
                )
            }
        Assertions.assertNotNull(exception)
    }

    @Test
    @Throws(
        CertificateException::class,
        IOException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class
    )
    fun testkeyStoreFromCertificateInMemory() {
        testkeyStoreFromCertificateInternal(caPem, KEY_STORE_PASSWORD, FileSystems.getDefault(), "")

        val exception: Exception =
            Assertions.assertThrows(CertificateException::class.java) {
                testkeyStoreFromCertificateInternal(
                    caPem_Bad,
                    KEY_STORE_PASSWORD,
                    FileSystems.getDefault(),
                    ""
                )
            }
        Assertions.assertNotNull(exception)
    }

    @SuppressFBWarnings("HARD_CODE_PASSWORD")
    @Throws(
        KeyStoreException::class,
        IOException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        InterruptedException::class
    )
    fun testKeyStoreFromClientCertificateInternal(
        certString: String,
        keyString: String,
        keyStorePassword: String,
        filesystem: FileSystem,
        directory: String
    ) {
        val ksUri =
            SSLCertificateUtils.keyStoreFromClientCertificate(
                certString,
                keyString,
                keyStorePassword,
                filesystem,
                directory
            )
        val ks = KeyStore.getInstance("PKCS12")
        val inputStream = Files.newInputStream(Path.of(ksUri))
        ks.load(inputStream, KEY_STORE_PASSWORD.toCharArray())
        Assertions.assertTrue(ks.isKeyEntry(SSLCertificateUtils.KEYSTORE_ENTRY_PREFIX))
        Assertions.assertFalse(ks.isKeyEntry("cd_"))
        Assertions.assertEquals(1, ks.size())
        Files.delete(Path.of(ksUri))
    }

    @Test
    @Throws(
        CertificateException::class,
        IOException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        KeyStoreException::class,
        InterruptedException::class
    )
    fun testKeyStoreFromClientCertificate() {
        testKeyStoreFromClientCertificateInternal(
            clientPem,
            clientKey,
            KEY_STORE_PASSWORD,
            FileSystems.getDefault(),
            SLASH_TMP
        )

        val exceptionKey: Exception =
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                testKeyStoreFromClientCertificateInternal(
                    clientPem,
                    clientKey_wrong_format,
                    KEY_STORE_PASSWORD,
                    FileSystems.getDefault(),
                    SLASH_TMP
                )
            }
        Assertions.assertNotNull(exceptionKey)

        val exceptionCert: Exception =
            Assertions.assertThrows(CertificateException::class.java) {
                testKeyStoreFromClientCertificateInternal(
                    caPem_Bad,
                    clientKey,
                    KEY_STORE_PASSWORD,
                    FileSystems.getDefault(),
                    SLASH_TMP
                )
            }
        Assertions.assertNotNull(exceptionCert)
    }

    @Test
    @Throws(
        CertificateException::class,
        IOException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        KeyStoreException::class,
        InterruptedException::class
    )
    fun testKeyStoreFromClientCertificateInMemory() {
        testKeyStoreFromClientCertificateInternal(
            clientPem,
            clientKey,
            KEY_STORE_PASSWORD,
            FileSystems.getDefault(),
            ""
        )

        val exceptionKey: Exception =
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                testKeyStoreFromClientCertificateInternal(
                    clientPem,
                    clientKey_wrong_format,
                    KEY_STORE_PASSWORD,
                    FileSystems.getDefault(),
                    ""
                )
            }
        Assertions.assertNotNull(exceptionKey)

        val exceptionCert: Exception =
            Assertions.assertThrows(CertificateException::class.java) {
                testKeyStoreFromClientCertificateInternal(
                    caPem_Bad,
                    clientKey,
                    KEY_STORE_PASSWORD,
                    FileSystems.getDefault(),
                    ""
                )
            }
        Assertions.assertNotNull(exceptionCert)
    }

    companion object {
        private const val SLASH_TMP = "/tmp"
        private const val KEY_STORE_PASSWORD = "123456"
        private const val KEY_STORE_PASSWORD2 = "78910"
        val caPem: String =
            ("""
            -----BEGIN CERTIFICATE-----
            MIIDAzCCAeugAwIBAgIBATANBgkqhkiG9w0BAQsFADA8MTowOAYDVQQDDDFNeVNR
            TF9TZXJ2ZXJfOC4wLjMwX0F1dG9fR2VuZXJhdGVkX0NBX0NlcnRpZmljYXRlMB4X
            DTIyMDgwODA1NDMwOFoXDTMyMDgwNTA1NDMwOFowPDE6MDgGA1UEAwwxTXlTUUxf
            U2VydmVyXzguMC4zMF9BdXRvX0dlbmVyYXRlZF9DQV9DZXJ0aWZpY2F0ZTCCASIw
            DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKb2tDaE4TO/4xKRZ0QqpB4ho3cy
            daw85Sn8VNLa42EJgZVpSr0WCFl11Go7r0O2TMvceaWsnJU7FLhYHSR+Dlm62yVO
            0DsnMOC0kUoDnjSE/PmponWnoC79fgXV7AwKxSW4LLxYlPHQb4Kb7rv+UJ3KbxZz
            zB7JEm9WQCJ/byn1/jxQtoPGvWL2csX3RFr9QNh8UgpOBQsbebeLWNgxdYda2sz3
            kJcwk754Vj1mx6iszjLP0oHZu+RuoM+xIrpDmpPNMW/0rQl6q+vCymNxaxX8+MuW
            czRJ1hjh4cVjArp8YhJCEMVaLajVkhbzYaPRsdW1NGjh+C3eZnOm5fRi35kCAwEA
            AaMQMA4wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAWKlbtUosXVy7
            LbFEuL3c2Igs023v0mQNvtZVBl5Qpsxpc3+ybmQfksEQoPxPKmWpsnWv5Bsvt335
            /NHv1wSajHEpoyDBtF1QT2rR/kjezFpiH9AY3xwtBdZhTDlc5UBrpyv+Issn1CZF
            edcIk54Gzxifn+Et5WP8b6HV/ehdE0qQPtHDmendEaIHXg12/NE+hj3DocSVm8w/
            LUNeYd9wXefwMrEWwDn0DZSsShZmgJoppA15qOnq+FVW/bhZwRv5L4l3AJv0SGoA
            o7DXxD0VGHDA6aC4tJssZbrnoDCBPzYmt9s9GwVupuEroJHZ0Wks4pt4Wx50DUgA
            KC3v0Mo/gg==
            -----END CERTIFICATE-----""".trimIndent())

        val caPem_Bad: String =
            ("""
            -----BEGIN CERTIFICATE-----
            MIIDAzCCAeugAwIBAgIBATANBgkqhkiG9w0BAQsFADA8MTowOAYDVQQDDDFNeVNR
            TF9TZXJ2ZXJfOC4wLjMwX0F1dG9fR2VuZXJhdGVkX0NBX0NlcnRpZmljYXRlMB4X
            DTIyMDgwODA1NDMwOFoXDTMyMDgwNTA1NDMwOFowPDE6MDgGA1UEAwwxTXlTUUxf
            U2VydmVyXzguMC4zMF9BdXRvX0dlbmVyYXRlZF9DQV9DZXJ0aWZpY2F0ZTCCASIw
            DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKb2tDaE4TO/4xKRZ0QqpB4ho3cy
            daw85Sn8VNLa42EJgZVpSr0WCFl11Go7r0O2TMvceaWsnJU7FLhYHSR+Dlm62yVO
            XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
            /NHv1wSajHEpoyDBtF1QT2rR/kjezFpiH9AY3xwtBdZhTDlc5UBrpyv+Issn1CZF
            edcIk54Gzxifn+Et5WP8b6HV/ehdE0qQPtHDmendEaIHXg12/NE+hj3DocSVm8w/
            LUNeYd9wXefwMrEWwDn0DZSsShZmgJoppA15qOnq+FVW/bhZwRv5L4l3AJv0SGoA
            o7DXxD0VGHDA6aC4tJssZbrnoDCBPzYmt9s9GwVupuEroJHZ0Wks4pt4Wx50DUgA
            KC3v0Mo/gg==
            -----END CERTIFICATE-----""".trimIndent())

        val clientPem: String =
            ("""
            -----BEGIN CERTIFICATE-----
            MIIDBDCCAeygAwIBAgIBAzANBgkqhkiG9w0BAQsFADA8MTowOAYDVQQDDDFNeVNR
            TF9TZXJ2ZXJfOC4wLjMwX0F1dG9fR2VuZXJhdGVkX0NBX0NlcnRpZmljYXRlMB4X
            DTIyMDgwODA1NDMwOFoXDTMyMDgwNTA1NDMwOFowQDE+MDwGA1UEAww1TXlTUUxf
            U2VydmVyXzguMC4zMF9BdXRvX0dlbmVyYXRlZF9DbGllbnRfQ2VydGlmaWNhdGUw
            ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCV/eRPDZmrPP8d2pKsFizU
            JQkGOYDKXOilLibR1TQwN/8MToop8+mvtMi7zr/cWBDR0qTObbduWFQdK82vGppS
            ZgrRG3QWVpe8NNI9AhriVZiOmcEQqgAhbgos57Tkjy3qghNbUN1KGb3I0DnNOtvF
            RIdATbE+LxOTgCzz/Cw6DVReunQvVo9T4EC4PBBUelMWlAJLo61AQVLM3ufx4ug2
            1wbV6D/aSRooNhkwWcwk+2vabxKnOzFAQzNU7dIZlBpo6coHFwZDUxtdM2DtuLHn
            /r9CsMw8p4wtdIRXrTDmiF/xTXKnABGM8kEqPovZ6eh7He1jrzLTVANUfNQc5b8F
            AgMBAAGjDTALMAkGA1UdEwQCMAAwDQYJKoZIhvcNAQELBQADggEBAGDJ6XgLBzat
            rpLDfGHR/tZ4eFzt1Nhjzl4CyFjpUcr2e2K5XmuveJAecaQSHff2zXwfGpg/BIen
            WcPm2daIzcfN/wWg8ENMB/JE3dMq44pOmWs2g4FPDQuaH81IV0hGX4klk2XZpskJ
            moWXyGY43Xr3bbNBjyOxgBsQc4kD96ODMUKfzNMH4p9hXKAMrF9DqHwQUho5uM6M
            RnU7Uzr745xw7LKJglCgO20t4302wzsUAEPuCTcB9wJy1/cRbMmoLAdUdn6XhFb4
            pR3UDNJvXGc8by6VWrXOeB0BFeB3beMxezlTHDOWoWeJwvEfAAD/dpwHXwp5dm9L
            VjtlERcTfH8=
            -----END CERTIFICATE-----""".trimIndent())

        val clientKey: String =
            ("""
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEAlf3kTw2Zqzz/HdqSrBYs1CUJBjmAylzopS4m0dU0MDf/DE6K
            KfPpr7TIu86/3FgQ0dKkzm23blhUHSvNrxqaUmYK0Rt0FlaXvDTSPQIa4lWYjpnB
            EKoAIW4KLOe05I8t6oITW1DdShm9yNA5zTrbxUSHQE2xPi8Tk4As8/wsOg1UXrp0
            L1aPU+BAuDwQVHpTFpQCS6OtQEFSzN7n8eLoNtcG1eg/2kkaKDYZMFnMJPtr2m8S
            pzsxQEMzVO3SGZQaaOnKBxcGQ1MbXTNg7bix5/6/QrDMPKeMLXSEV60w5ohf8U1y
            pwARjPJBKj6L2enoex3tY68y01QDVHzUHOW/BQIDAQABAoIBAHk/CHyC4PKUVyHZ
            2vCy6EABRB89AogSvJkyCn1anFpSGaDoKDWrjv7S4+U1RtCme8oxPboE5N+VFUGT
            dCwVFCSBikLor1mTXAruo/hfKD5HtQ+o6HFBCuP7IMyV7RtJRnOn/F+3qXpJ/qlC
            8UaeSqNXNwHbC+jZgzibxzrfYRz3BqnBYZsSP7/piN+rk6vAGs7WeawO1adqsLS6
            Hr9GilEe+bW/CtXsah3AYVwxDnwo/c03JYBdzYkRRqLgJ9dDG/5o/88FeeKbVb+U
            ZrGV9adwa+KGwsuMTYi7pkXUosm+43hLkmYUykxFv0vfkGz8EnDh4MBtY66QMkUJ
            cQgWl6ECgYEAxVJNsxpJjEa+d737iK7ylza0GhcTI3+uNPN92u0oucictMzLIm7N
            HAUhrHoO71NDYQYJlox7BG8mjze7l6fkwGfpg2u/KsN0vIqc+F+gIQeC7kmpRxQk
            l96pxMW25VhibZJFBaDx9UeBkR9RBnI1AF3jD3+wOdua+C9CMahdTDkCgYEAwph4
            FY2gcOSpA0Xz1cOFPNuwQhy9Lh3MJsb1kt20hlTcmpp3GpBrzyggiyIlpBtBHDrP
            6FcjZtV58bv8ckKB8jklvooJkyjmowBx+L7mHZ6/7QFPDQkp/dY9dQPtWjgrPyo+
            rLIN+SoVmyKdyXXaauyjyEPAexsuxzUKq0MMIS0CgYEAirvJQYnT+DqtJAeBWKKY
            kdS2YDmlDSpyU2x3KnvgTG9OLphmojkBIRhCir/uzDngf9D84Mq4m2+CzuNCk+hJ
            nzXwKqSQ7gIqi31xy/d/4Hklh2BnEkCJUfYNqvnQFARGf/99Y+268Ndrs5svHrch
            qLZaNMV0I9nRZXnksoFLx5ECgYBJ8LFAT041V005Jy1dfit0Um2I0W64xS27VkId
            igx8NmaUgDjdaR7t2etzsofm8UwuM9KoD+QtwNPTHIDx0X+a0EgdPEojFpl8OkEU
            KUU64AVBQwwMgfzorK0xd0qKy2jzWVPzPry8flczWVXnJNbXZg9dmxDaNhvyKZ9i
            L9m+CQKBgG3kkQTtsT7k1kQt/6cqjAaBq9Koi0gbS8hWjTioqPKHVQAAEjqVkmqa
            uuD/3Knh1gCgxW4jAUokRwfM7IgVA/plQQDQaKBzcFUl94Hl+t6VuvdvtA02MboE
            7TicEc38QKFoLN2hti0Bmm1eJCionsSPiuyDYH5XnhSz7TDjV9sM
            -----END RSA PRIVATE KEY-----""".trimIndent())

        const val clientKey_wrong_format: String =
            ("""MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDBmUvDIVGZ5HsRgnXKns2fTf26pfKND45xu
            NOEWVetpvo3lGc28vVMvtPiNH/kuELxo5NesC89iotxfbOTl4I9BbjFVg3eO1nNhwmToU2f1kJJ5QFRjFw+xacIMsfBT5xy/v9U7ohZXdEk6txYkOpvhfja
            JcLDutT+NtzRdBsttgyItp5fODnk02G4bLsJ68jVH1/CXkDRvxktLR0/NctbtPVuACwA1QG9MsVbH3cE7SymIrzgI8JHwud63dQUb5iQWZ0iIDBqmF95wvg
            ox9O4QjnZCkHxo3kuYxBPaxAuMMVTohLBH/oAvo0FJt+0XF453sLPO8x3zOUnJJLhn4VHAgMBAAECggEBALQ4UB7F1YC9ARO7rouAaUnzAE/QS4qlAKU8uS
            prQQOWfTdgHvU4FsHqorPgy23PWgI3k+iBenh/kG+F5LVwRP0pZmfNQ/uspFx/aJrVfb1dZzgCxsdzMiv9MxCetPVvduRWHLqjkqoee6MyPwzzWkmXHaF1p
            WkvczdzOvyAaQyS3UPsnQzS0kt4mELGZs/E24K9vD9KfSrdRXxkk3fsLFbLrrau/mEhQ/CKX7Xl4MBchiH+lF8kHvpAc27fevrnDPToZp2cbfSc1oeeKjIM
            VmYFKytTCi5IXCNG6S0H31rNpX+5VbdZc1iJLPH7Ch6J+dRzX36R+5zSmp7OIl5gAoECgYEA5f1p/umqMW91HQ+amZoIg6gldFfGglFM5IVCrn0RRB/BrgH
            Rnpo0jo3JaOUyQMfyDz69lkpKEgejYTPGDkz3kJmpA54rBwmFitB13ZaqhzM63VzYE3hPdCqpy1VTLxW2+T5nEbLuiR4rC2Y7z+CRBmYdQUNxSq90rCpveg
            XIq4sCgYEA135M0fmeBAjTiz3f2pRt7ne64WzY4jJ0SRe6BrVA6PnI9V5+wwtRzyhee9A0awzal6t0OvAdxmnAZg3PsP1fbOPeVwXbvBKtZ4rM7adv6UdYy
            6oxjd9eULK92YnVOcZPf595WmoK28L37EHlxjP8p6lnMBk/BF9Y3N3rz2xyNLUCgYAZ8qdczTwYa7zI1JPatJg1Umk3YRfSaB3GwootaYrjJroRSb8+p6M6
            WiDZJtKuoGBc+/Uj2anVsurp8o9r2Z8sv0lkURoFpztb1/0UTQVcT5lalDkEqVQ9hPq3KB9Edqy4HiQ+yPNEoRS2KoihAXMbR7YRQOytQnJlYjxFhhWH1QK
            BgQCNFv97FyETaSgAacGQHlCfqrqr75VM/FXQqX09+RyHrUubA4ShdV7Z8Id0L0yyrlbMqRBPqnkEOKck6nQKYMpCxCsF9Sr6R4xLV8B29YK7TOBhcIxDZH
            UfBvhwXuNBkYrpd2OABCAZ5NxoTnj/vXf12l9aSZ1N4pOPAKntRAa+ZQKBgQDCPgJQfZePJGOvSIkW/TkXcHpGsexb5p900Si23BLjnMtCNMSkHuIWb60xq
            I3vLFKhrLiYzYVQ5n3C6PYLcdfiDYwruYU3zmtr/gpg/QzcsvTe5CW/hxTAkzsZsFBOquJyuyCRBGN59tH6N6ietu8zzvCc8EeJJX7N7AX0ezF7lQ==""")
    }
}
