package pt.guardiao.core.explain

import pt.guardiao.core.model.Confidence
import pt.guardiao.core.model.FindingKind
import pt.guardiao.core.model.Limitation
import pt.guardiao.core.model.RecommendedAction
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import pt.guardiao.core.model.SignalType.*

/** European Portuguese wording. Short sentences, familiar words, no technical terms. */
object PtPtCatalog : ExplanationCatalog {
    override val languageTag = "pt-PT"

    override fun headline(level: RiskLevel) = when (level) {
        RiskLevel.CRITICAL -> "PARE. Esta mensagem pode ser uma burla."
        RiskLevel.HIGH -> "Atenção: esta mensagem tem sinais de burla."
        RiskLevel.CAUTION -> "Esta mensagem merece atenção."
        RiskLevel.NO_WARNING -> "Não encontrámos sinais de alerta."
    }

    override fun summary(level: RiskLevel) = when (level) {
        RiskLevel.CRITICAL -> "Não faça nada do que a mensagem pede. Não envie dinheiro nem códigos."
        RiskLevel.HIGH -> "Não envie dinheiro ainda. Confirme primeiro, por outro meio."
        RiskLevel.CAUTION -> "Pode ser verdadeira, mas confirme antes de fazer o que pede."
        RiskLevel.NO_WARNING -> "Isto não garante que a mensagem seja verdadeira. Na dúvida, confirme com alguém de confiança."
    }

    override fun reason(type: SignalType, kind: FindingKind): String {
        val text = when (type) {
            FAMILY_ADDRESS -> "A mensagem trata-o como familiar, mas não conseguimos confirmar quem a escreveu."
            FAMILY_IDENTITY_CLAIM -> "Alguém diz ser seu familiar. Não conseguimos confirmar que é verdade."
            NEW_NUMBER_CLAIM -> "Diz que mudou de número. Esta é uma forma muito usada para enganar familiares."
            BANK_CLAIM -> "Diz falar em nome de um banco ou serviço de pagamentos."
            AUTHORITY_CLAIM -> "Diz falar em nome da polícia, das Finanças ou de outra entidade pública."
            COURIER_CLAIM -> "Fala de uma encomenda ou entrega."
            TECH_SUPPORT_CLAIM -> "Diz ser apoio técnico."
            URGENCY -> "Pede para agir com pressa."
            THREAT -> "Ameaça com bloqueios, multas ou problemas com a justiça."
            DISTRESS_STORY -> "Conta uma história de aflição para o levar a ajudar depressa."
            SECRECY_REQUEST -> "Pede segredo ou para não falar com a família ou o banco."
            CHANNEL_SWITCH -> "Pede para falar por outro número ou outra aplicação."
            MONEY_TRANSFER_REQUEST -> "Pede dinheiro ou uma transferência."
            PAYMENT_DEMAND -> "Pede um pagamento ou uma taxa."
            UNUSUAL_PAYMENT_METHOD -> "Pede pagamento de uma forma pouco habitual, como cartões-oferta, criptomoedas ou uma \"conta segura\"."
            BANK_ACCOUNT_NUMBER -> "Indica uma conta bancária para onde enviar dinheiro."
            CREDENTIAL_REQUEST -> "Pede um código, PIN ou palavra-passe. Ninguém de confiança lhe pede isto."
            ACCOUNT_VERIFICATION_REQUEST -> "Pede para confirmar ou atualizar os seus dados."
            REMOTE_ACCESS_REQUEST -> "Pede para instalar uma aplicação ou dar acesso ao seu telemóvel."
            PRIZE_REFUND_OR_INVESTMENT -> "Promete um prémio, reembolso ou ganho fácil."
            LOOKALIKE_DOMAIN -> "O link imita o endereço de uma empresa conhecida, mas não é o endereço verdadeiro."
            LINK_TEXT_MISMATCH -> "O link mostra um endereço mas leva para outro."
            PUNYCODE_OR_MIXED_SCRIPT_DOMAIN -> "O link usa letras disfarçadas para parecer outro endereço."
            IP_ADDRESS_LINK -> "O link usa números em vez de um nome de site."
            LINK_SHORTENER -> "O link está encurtado e esconde o destino."
            UNUSUAL_DOMAIN_ENDING -> "O endereço do link termina de forma pouco habitual."
            INSECURE_LINK -> "O link não usa ligação segura."
            MALFORMED_LINK -> "Há um link que não conseguimos ler."
            KNOWN_MALICIOUS_LINK -> "O link está numa lista de sites perigosos."
            SENDER_NAME_DOMAIN_MISMATCH -> "O nome do remetente não corresponde ao endereço de onde o email foi enviado."
            RISKY_ATTACHMENT -> "Tem um anexo de um tipo que pode instalar programas."
            SENDER_NOT_IN_CONTACTS -> "Parece vir de um número que não tem guardado nos contactos."
            INSTRUCTIONS_TO_AUTOMATED_TOOLS -> "A mensagem tenta enganar programas de verificação como este."
        }
        return if (kind == FindingKind.INFERRED && type != SENDER_NOT_IN_CONTACTS) "$text (sinal estimado)" else text
    }

    override fun action(action: RecommendedAction) = when (action) {
        RecommendedAction.DO_NOT_SEND_MONEY -> "Não envie dinheiro nem faça pagamentos por agora."
        RecommendedAction.CALL_KNOWN_NUMBER -> "Ligue à pessoa para o número que já tinha guardado. Não use o número desta mensagem."
        RecommendedAction.NEVER_SHARE_CODES -> "Nunca partilhe códigos, PIN ou palavras-passe. O banco e a polícia nunca os pedem."
        RecommendedAction.DO_NOT_OPEN_LINK -> "Não abra o link. Use a aplicação oficial ou o endereço que já conhece."
        RecommendedAction.CONTACT_ORGANISATION_OFFICIAL_CHANNEL -> "Contacte a entidade pelo número do seu cartão, extrato ou site oficial. Não use contactos desta mensagem."
        RecommendedAction.DO_NOT_INSTALL_APPS -> "Não instale nenhuma aplicação pedida nesta mensagem."
        RecommendedAction.TALK_TO_TRUSTED_PERSON -> "Fale com alguém de confiança antes de fazer qualquer coisa."
        RecommendedAction.IF_ALREADY_PAID_CALL_BANK -> "Se já enviou dinheiro ou dados, ligue já ao seu banco pelo número oficial."
        RecommendedAction.DO_NOT_OPEN_ATTACHMENT -> "Não abra o anexo."
        RecommendedAction.WHEN_IN_DOUBT_VERIFY -> "Se tiver dúvidas, confirme com a pessoa ou entidade por outro meio."
    }

    override fun confidence(level: RiskLevel, confidence: Confidence) = when {
        level == RiskLevel.NO_WARNING -> "Isto é uma verificação automática e pode falhar."
        confidence == Confidence.HIGH -> "Encontrámos vários sinais fortes."
        confidence == Confidence.MEDIUM -> "Encontrámos sinais claros, mas não temos a certeza."
        else -> "Encontrámos poucos sinais. Pode ser uma mensagem verdadeira."
    }

    override fun limitation(limitation: Limitation) = when (limitation) {
        Limitation.CONTENT_TRUNCATED -> "Só conseguimos ver parte da mensagem."
        Limitation.CONTENT_REDACTED -> "O telemóvel escondeu parte do conteúdo."
        Limitation.SENDER_IDENTITY_UNVERIFIABLE -> "Não conseguimos confirmar quem enviou a mensagem."
        Limitation.MESSAGE_TOO_LONG -> "A mensagem é muito longa; só analisámos o início."
        Limitation.COMPLETENESS_UNKNOWN, Limitation.NO_CONVERSATION_HISTORY,
        Limitation.LINK_REPUTATION_NOT_CHECKED, Limitation.AI_ANALYSIS_UNAVAILABLE -> null
    }
}
