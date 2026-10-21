package pt.guardiao.core

import org.junit.Test
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** One or more tests per deterministic rule, including accents, case and negation. */
class RuleTest {
    private fun signals(text: String) = analyse(text).signalTypes

    @Test fun `urgency is detected with and without accents`() {
        assertTrue(SignalType.URGENCY in signals("Responda URGENTEMENTE"))
        assertTrue(SignalType.URGENCY in signals("tem de pagar nas próximas 24 horas"))
        assertTrue(SignalType.URGENCY in signals("please reply immediately"))
    }

    @Test fun `urgency alone never raises a warning`() {
        assertEquals(RiskLevel.NO_WARNING, analyse("Isto é urgente, liga-me quando saíres do trabalho.").level)
    }

    @Test fun `new number claims`() {
        listOf(
            "Olá mãe, este é o meu novo número",
            "mudei de número, guarda este contacto",
            "o meu telemóvel caiu na água",
            "Hi dad, I lost my phone",
        ).forEach { assertTrue(SignalType.NEW_NUMBER_CLAIM in signals(it), it) }
        assertFalse(SignalType.NEW_NUMBER_CLAIM in signals("mudei de casa no mês passado"))
    }

    @Test fun `family identity claim`() {
        assertTrue(SignalType.FAMILY_IDENTITY_CLAIM in signals("Avó, sou eu o teu neto"))
        assertTrue(SignalType.FAMILY_IDENTITY_CLAIM in signals("it's me your son"))
    }

    @Test fun `impersonation of organisations`() {
        assertTrue(SignalType.BANK_CLAIM in signals("Caixa Geral de Depósitos informa"))
        assertTrue(SignalType.AUTHORITY_CLAIM in signals("Notificação da Autoridade Tributária"))
        assertTrue(SignalType.COURIER_CLAIM in signals("A sua encomenda está retida na alfândega"))
        assertTrue(SignalType.TECH_SUPPORT_CLAIM in signals("Somos do suporte técnico da Microsoft"))
    }

    @Test fun `threats`() {
        assertTrue(SignalType.THREAT in signals("A sua conta será bloqueada"))
        assertTrue(SignalType.THREAT in signals("será emitido um mandado de detenção"))
        assertTrue(SignalType.THREAT in signals("your account will be suspended"))
    }

    @Test fun `secrecy`() {
        assertTrue(SignalType.SECRECY_REQUEST in signals("não digas nada à mãe"))
        assertTrue(SignalType.SECRECY_REQUEST in signals("Não ligue ao banco, nós tratamos"))
        assertTrue(SignalType.SECRECY_REQUEST in signals("keep this secret"))
    }

    @Test fun `money requests`() {
        assertTrue(SignalType.MONEY_TRANSFER_REQUEST in signals("Consegues fazer uma transferência de 300€?"))
        assertTrue(SignalType.MONEY_TRANSFER_REQUEST in signals("manda-me 50 euros por MB WAY"))
        assertTrue(SignalType.MONEY_TRANSFER_REQUEST in signals("can you send me money"))
        assertFalse(SignalType.MONEY_TRANSFER_REQUEST in signals("Já fiz a transferência"))
    }

    @Test fun `unusual payment methods`() {
        assertTrue(SignalType.UNUSUAL_PAYMENT_METHOD in signals("compre cartões-presente da Google Play"))
        assertTrue(SignalType.UNUSUAL_PAYMENT_METHOD in signals("transfira para uma conta segura"))
        assertTrue(SignalType.UNUSUAL_PAYMENT_METHOD in signals("pagamento em bitcoin"))
    }

    @Test fun `credential requests and the negation guard`() {
        assertTrue(SignalType.CREDENTIAL_REQUEST in signals("Indique o PIN do seu cartão"))
        assertTrue(SignalType.CREDENTIAL_REQUEST in signals("envia-me o código que recebeste"))
        assertTrue(SignalType.CREDENTIAL_REQUEST in signals("please send me the verification code"))
        assertFalse(SignalType.CREDENTIAL_REQUEST in signals("Nunca partilhe este código com ninguém"))
        assertFalse(SignalType.CREDENTIAL_REQUEST in signals("Não partilhe o seu PIN"))
    }

    @Test fun `asking for a code to be sent back is critical on its own`() {
        assertEquals(RiskLevel.CRITICAL, analyse("Enviei-te um código por engano, podes mandar-me?").level)
    }

    @Test fun `remote access`() {
        assertTrue(SignalType.REMOTE_ACCESS_REQUEST in signals("instale o AnyDesk"))
        assertTrue(SignalType.REMOTE_ACCESS_REQUEST in signals("descarregue esta aplicação"))
        assertTrue(SignalType.REMOTE_ACCESS_REQUEST in signals("abra o ficheiro banco.apk"))
    }

    @Test fun `lures`() {
        assertTrue(SignalType.PRIZE_REFUND_OR_INVESTMENT in signals("Ganhou um iPhone!"))
        assertTrue(SignalType.PRIZE_REFUND_OR_INVESTMENT in signals("lucro garantido todas as semanas"))
    }

    @Test fun `iban is detected and masked in evidence`() {
        val result = analyse("Mãe, transfere para PT50 0000 0000 0000 0000 0000 0")
        val finding = result.findings.first { it.type == SignalType.BANK_ACCOUNT_NUMBER }
        assertFalse(finding.evidence.first().excerpt.contains("0000 0000"))
    }

    @Test fun `sender shown as a number is an inference only for WhatsApp`() {
        val wa = analyse(msg("olá", MessageSource.WHATSAPP_NOTIFICATION, sender = "+351 900 000 000"))
        assertTrue(SignalType.SENDER_NOT_IN_CONTACTS in wa.signalTypes)
        assertEquals(pt.guardiao.core.model.FindingKind.INFERRED, wa.findings.first().kind)
        val shared = analyse(msg("olá", MessageSource.USER_SHARED, sender = "+351 900 000 000"))
        assertFalse(SignalType.SENDER_NOT_IN_CONTACTS in shared.signalTypes)
    }

    @Test fun `sender name and domain mismatch`() {
        val r = analyse(msg("Atualize os seus dados", sender = "Banco CTT Apoio", address = "apoio@bancoctt-clientes.example"))
        assertTrue(SignalType.SENDER_NAME_DOMAIN_MISMATCH in r.signalTypes)
        val ok = analyse(msg("Extrato disponível", sender = "Millennium bcp", address = "info@millenniumbcp.pt"))
        assertFalse(SignalType.SENDER_NAME_DOMAIN_MISMATCH in ok.signalTypes)
    }

    @Test fun `display name alone never proves trust`() {
        // A familiar name with a code request is still critical.
        val r = analyse(msg("Podes enviar-me o código que te chegou?", MessageSource.WHATSAPP_NOTIFICATION, sender = "Filha"))
        assertEquals(RiskLevel.CRITICAL, r.level)
    }

    @Test fun `long messages are bounded and flagged`() {
        val r = analyse("a ".repeat(10_000) + "envia-me o PIN")
        assertTrue(r.isIncomplete)
    }

    @Test fun `empty input gives no warning, not safe`() {
        val r = analyse("")
        assertEquals(RiskLevel.NO_WARNING, r.level)
        assertEquals(pt.guardiao.core.model.Confidence.MEDIUM, r.confidence)
    }
}
