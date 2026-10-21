package pt.guardiao.core.detect

import pt.guardiao.core.model.Confidence.HIGH
import pt.guardiao.core.model.Confidence.LOW
import pt.guardiao.core.model.Confidence.MEDIUM
import pt.guardiao.core.model.SignalType

/**
 * Phrase rules in folded European Portuguese (with common Brazilian variants)
 * and English. Sources for the patterns are listed in
 * docs/THREAT_MODEL_AND_SCENARIOS.md. A single rule never decides the risk
 * level on its own: the policy combines tactics.
 */
object Lexicon {

    const val CODE_TO_SENDER_ID = "credentials.code_to_sender"

    private const val B = """\b"""

    // Verbs used to ask someone to hand something over (imperative, tu and você forms).
    private const val GIVE =
        """(envia|envie|enviar|enviares|reencaminha|reencaminhe|reencaminhar|reenvia|reenvie|manda|mande|mandar|indica|indique|indicar|diz|diga|dizer|da-me|de-me|dar|partilha|partilhe|partilhar|confirma|confirme|confirmar|introduz|introduza|insere|insira|inserir|digita|digite|fornece|forneca|fornecer|passa|passe|preciso d[oa]s?|necessito d[oa]s?|send|give|share|tell|enter|confirm|provide|forward|read)"""

    private const val SECRET_ITEM =
        """(codigos?|pin|palavras?.?passes?|password|senhas?|credenciais|cvv|cvc|otp|token|numero do (seu |teu )?cartao|dados do (seu |teu )?cartao|codigo de (verificacao|seguranca|confirmacao|acesso|ativacao|autorizacao)|codes?|passcode|verification code|security code|card (number|details))"""

    val FAMILY_ADDRESS = PhraseRule(
        id = "family.address",
        type = SignalType.FAMILY_ADDRESS,
        confidence = LOW,
        patterns = listOf(
            """^\W*(ola|oi|olá|bom dia|boa tarde|boa noite)?\W*(mae|pai|mamae|papai|mamã|avo|avozinha|avozinho|tia|tio)\b""",
            """${B}(hi|hello|hey)\W+(mum|mom|dad|mummy|grandma|grandpa|nan)\b""",
        ),
    )

    val FAMILY_IDENTITY_CLAIM = PhraseRule(
        id = "family.identity_claim",
        type = SignalType.FAMILY_IDENTITY_CLAIM,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(sou eu|e o|e a|aqui e o|aqui e a|fala o|fala a)\W+(o |a )?(teu|tua|seu|sua|vosso|vossa)\W+(filho|filha|neto|neta|sobrinho|sobrinha|afilhado|afilhada)\b""",
            """${B}it'?s (me,? )?(your )?(son|daughter|grandson|granddaughter|child)\b""",
        ),
    )

    val NEW_NUMBER = PhraseRule(
        id = "identity.new_number",
        type = SignalType.NEW_NUMBER_CLAIM,
        confidence = HIGH,
        patterns = listOf(
            """${B}(este|esse|e) (e )?o meu (novo )?(numero|contacto|whatsapp)\b""",
            """${B}(mudei|troquei) de (numero|telemovel|telefone|celular)\b""",
            """${B}(o meu |meu )?(numero|contacto) (novo|mudou)\b""",
            """${B}(novo|nova) (numero|contacto)\b""",
            """${B}(perdi|partiu-se|parti|estraguei|avariou|caiu)\W+(\w+\W+){0,3}(telemovel|telefone|celular)\b""",
            """${B}(telemovel|telefone|celular)\W+(\w+\W+){0,2}(avariou|partiu|caiu|estragou|ficou sem|foi roubado|morreu|na agua|na sanita)\b""",
            """${B}(guarda|grava|guarde|grave) (este|o meu novo|o novo|este meu) (numero|contacto)\b""",
            """${B}(apaga|apague|elimina|elimine) o (numero|contacto) (antigo|velho)\b""",
            """${B}(new number|changed my number|my new number|lost my phone|broke my phone|phone (broke|is broken))\b""",
        ),
    )

    val BANK_CLAIM = PhraseRule(
        id = "identity.bank",
        type = SignalType.BANK_CLAIM,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(caixa geral de depositos|cgd|millennium|bcp|novo banco|novobanco|santander|bpi|montepio|credito agricola|activobank|banco ctt|bankinter|abanca|moey|unicre|cetelem|cofidis|revolut)\b""",
            """${B}(o (seu|teu) banco|servico de (apoio|seguranca) (ao cliente|bancari[oa])|departamento (de seguranca|antifraude|anti-fraude)|homebanking|banca online|your bank|bank security|fraud department)\b""",
            """${B}(mb ?way|sibs|multibanco)\b""",
        ),
    )

    val AUTHORITY_CLAIM = PhraseRule(
        id = "identity.authority",
        type = SignalType.AUTHORITY_CLAIM,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(policia judiciaria|policia de seguranca publica|psp|gnr|guarda nacional republicana|policia|agente|inspetor|inspector)\b""",
            """${B}(autoridade tributaria|financas|portal das financas|at - autoridade|seguranca social|segurança social|tribunal|ministerio publico|banco de portugal|europol|interpol|governo|via verde|imt)\b""",
            """${B}(police|tax (office|authority)|court|government|hmrc|irs)\b""",
            """${B}pj\b""",
        ),
    )

    val COURIER_CLAIM = PhraseRule(
        id = "identity.courier",
        type = SignalType.COURIER_CLAIM,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(ctt|ctt expresso|dhl|ups|fedex|gls|seur|dpd|correios|chronopost|nacex|mrw)\b""",
            """${B}(encomenda|pacote|envio|entrega)\W+(\w+\W+){0,4}(retid[oa]|pendente|suspens[oa]|nao (foi )?entregue|aguarda|em espera|devolvid[oa])""",
            """${B}(parcel|package|delivery)\W+(\w+\W+){0,4}(held|pending|on hold|could not be delivered|failed)""",
        ),
    )

    val TECH_SUPPORT_CLAIM = PhraseRule(
        id = "identity.tech_support",
        type = SignalType.TECH_SUPPORT_CLAIM,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(suporte|apoio|assistencia) tecnic[oa]\b""",
            """${B}(microsoft|windows|apple|google|whatsapp) (support|suporte|seguranca|security|team|equipa)\b""",
            """${B}(o (seu|teu) (computador|telemovel|dispositivo) (tem|esta (com|infetado))|virus detetado|tech support)\b""",
        ),
    )

    val URGENCY = PhraseRule(
        id = "pressure.urgency",
        type = SignalType.URGENCY,
        confidence = LOW,
        patterns = listOf(
            """${B}(urgente|urgentemente|urgencia|imediatamente|de imediato|ja hoje|hoje mesmo|agora mesmo|o mais rapido possivel|o quanto antes|com a maxima brevidade|sem demora)\b""",
            """${B}(nas proximas|dentro de|no prazo de|ate) \d{1,2} ?(h|horas|minutos|min)\b""",
            """${B}(ultimo aviso|ultima oportunidade|ultima notificacao|prazo termina|expira hoje)\b""",
            """${B}(urgent|urgently|immediately|right now|asap|as soon as possible|within \d{1,2} (hours|minutes)|final notice|last chance|expires today)\b""",
        ),
    )

    val THREAT = PhraseRule(
        id = "pressure.threat",
        type = SignalType.THREAT,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(conta|cartao|acesso|app|aplicacao|servico|numero)\W+(\w+\W+){0,3}(sera |vai ser |foi |sera temporariamente |esta )?(bloquead[oa]|suspens[oa]|cancelad[oa]|desativad[oa]|encerrad[oa]|restringid[oa])""",
            """${B}(multa|coima|penhora|penhorad|processo (judicial|crime|criminal)|mandado|acao judicial|accao judicial|consequencias legais|sera (detid|pres|notificad|acusad)|queixa-crime|cadastro criminal)""",
            """${B}(evitar|evite|para nao haver) (a |o )?(suspensao|bloqueio|cancelamento|desativacao|encerramento|penhora|multa|coima)\b""",
            """${B}(will be (suspended|blocked|closed|cancelled|deactivated)|legal action|arrest warrant|be arrested|penalty|fine of)\b""",
        ),
    )

    val DISTRESS = PhraseRule(
        id = "pressure.distress",
        type = SignalType.DISTRESS_STORY,
        confidence = LOW,
        patterns = listOf(
            """${B}(tive um acidente|estou no hospital|estou pres[oa]|fui detid[oa]|estou em apuros|estou aflit[oa]|problema serio|preciso muito da tua ajuda|preciso da tua ajuda|nao consigo aceder (a|ao) (minha|meu) (conta|banco|app))\b""",
            """${B}(i had an accident|i'?m in (the )?hospital|i'?m in trouble|i need your help|i can'?t access my (bank|account))\b""",
        ),
    )

    val SECRECY = PhraseRule(
        id = "secrecy.request",
        type = SignalType.SECRECY_REQUEST,
        confidence = HIGH,
        patterns = listOf(
            """${B}(nao|nem) (digas|contes|fales|diga|conte|fale|comentes|comente)\W+(\w+\W+){0,3}(a ninguem|ao pai|a mae|ao banco|a familia|aos (teus|seus) filhos|a policia)""",
            """${B}(e (um )?segredo|fica entre nos|so entre nos|confidencial|sigilo absoluto)\b""",
            """${B}(nao|nem) (ligue|ligues|contacte|contactes|va|vas) (ao|para o|a) (banco|balcao|agencia|policia)""",
            """${B}(nao (desligue|desligues) (a chamada|o telefone))\b""",
            """${B}(don'?t tell (anyone|dad|mum|mom|the bank)|keep (this|it) (a )?secret|between (you and me|us))\b""",
        ),
    )

    val CHANNEL_SWITCH = PhraseRule(
        id = "pressure.channel_switch",
        type = SignalType.CHANNEL_SWITCH,
        confidence = LOW,
        patterns = listOf(
            """${B}(fala|fale|contacta|contacte|escreve|escreva|liga|ligue|adiciona|adicione|responde|responda)(-me)?\W+(\w+\W+){0,2}(para este|neste|por este|a este|para o) (numero|contacto|whatsapp|telegram)\b""",
            """${B}(telegram|signal)\b""",
            """${B}(ligue|ligar|telefone|contacte)\W+(\w+\W+){0,3}(para o )?(numero|linha)\W+(\+|00)?\d""",
            """${B}(text|call|contact) me (on|at) (this|my new) (number|whatsapp)\b""",
        ),
    )

    val MONEY_TRANSFER = PhraseRule(
        id = "money.transfer_request",
        type = SignalType.MONEY_TRANSFER_REQUEST,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(fazes|faz|faca|facas|fazer|podes fazer|pode fazer|consegues fazer|consegue fazer|preciso que (me )?(faca|facas|faz))\W+(\w+\W+){0,3}(transferencia|pagamento|mb ?way|pix)""",
            """${B}(transfere|transfira|transferir|transferes|envia|envie|manda|mande|empresta|empreste)(-me)?\W+(\w+\W+){0,4}(dinheiro|euros?|€|\d+ ?€|valor|quantia|mb ?way)""",
            """${B}(preciso|precisava|necessito) (urgentemente )?(de )?(dinheiro|\d+ ?(€|euros?)|uma transferencia|que me (emprestes|empreste|transfiras|transfira|mandes|mande|envies|envie))""",
            """${B}(paga|pague|pagas|podes pagar|pode pagar)\W+(\w+\W+){0,3}(esta conta|uma conta|a conta|esta fatura|por mim)""",
            """${B}(manda|envia|mande|envie)\W+(\w+\W+){0,2}(por|via|pelo) mb ?way""",
            """${B}(send|transfer|wire|lend) (me )?(\w+ ){0,3}(money|funds|£|€|\$|\d+)""",
            """${B}(i need|can you send) (\w+ ){0,3}(money|a transfer|\d+ ?(euros?|pounds|dollars))""",
        ),
    )

    val PAYMENT_DEMAND = PhraseRule(
        id = "money.payment_demand",
        type = SignalType.PAYMENT_DEMAND,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(taxas?|custos?|portes?|tarifas?|direitos?) (de |do |da |dos )?(envio|entrega|alfandega|aduaneir[oa]s?|reenvio|armazenamento|desalfandegamento|desbloqueio|processamento|portagem)""",
            """${B}(pague|efetue o pagamento|regularize|regularizar|liquide|liquidar|pagar) (o valor|a divida|o montante|a taxa|a coima|a multa|o pagamento|a portagem|ate)""",
            """${B}(valor em divida|montante em divida|pagamento (pendente|em atraso)|divida (pendente|em atraso)|fatura (pendente|em atraso))\b""",
            """${B}(pay (the|a) (fee|fine|customs|charge)|outstanding (payment|balance|toll)|unpaid (toll|invoice|fee))\b""",
        ),
    )

    val UNUSUAL_PAYMENT = PhraseRule(
        id = "money.unusual_method",
        type = SignalType.UNUSUAL_PAYMENT_METHOD,
        confidence = HIGH,
        patterns = listOf(
            """${B}(cartao[- ](presente|oferta)|cartoes[- ](presente|oferta)|vales? (de )?(compras|oferta)|gift ?cards?|paysafecard|cartao steam|cartao google play|itunes)\b""",
            """${B}(bitcoin|btc|criptomoedas?|cripto|crypto|usdt|tether|ethereum|binance|carteira digital|wallet)\b""",
            """${B}(conta segura|conta de seguranca|conta (protegida|temporaria)|safe account|secure account)\b""",
            """${B}(levantamento|levantar o dinheiro|deposite (o dinheiro|em numerario)|entregar o dinheiro a um (estafeta|agente|funcionario))\b""",
        ),
    )

    val CREDENTIAL_REQUEST = PhraseRule(
        id = "credentials.request",
        type = SignalType.CREDENTIAL_REQUEST,
        confidence = HIGH,
        negatable = true,
        patterns = listOf(
            """${B}${GIVE}\W+(\w+\W+){0,5}${SECRET_ITEM}\b""",
            """${B}(o |um |esse |este )?codigo que (recebeu|recebeste|lhe (foi )?enviad|te (foi )?enviad|te enviei|lhe enviei|vai receber|vais receber|chegou)""",
            """${B}(the )?code (you (just )?received|i sent you|that was sent to you)\b""",
        ),
    )

    /**
     * Asking for a code to be sent back to the sender, or the "code sent by
     * mistake" story. This is the account-takeover pattern and is treated as
     * stronger than a generic mention of a code.
     */
    val CODE_TO_SENDER = PhraseRule(
        id = CODE_TO_SENDER_ID,
        type = SignalType.CREDENTIAL_REQUEST,
        confidence = HIGH,
        negatable = true,
        patterns = listOf(
            """${B}(envia|envie|manda|mande|reencaminha|reencaminhe|reencaminhar|reenvia|reenvie|passa|passe|diz|diga|da|de|dar|enviar)(-me|-nos| me| nos)\W+(\w+\W+){0,4}${SECRET_ITEM}\b""",
            """${B}(enviei|mandei|enviaram|mandaram|enviamos)(-te|-lhe|-me)? (um |o )?(\w+\W+){0,6}codigo\W+(\w+\W+){0,6}(por engano|sem querer)""",
            """${B}codigo\W+(\w+\W+){0,6}(por engano|sem querer)""",
            """${B}(send|give|forward|tell) (me|us) (the |your |that )?(\w+ ){0,2}(code|pin|password|passcode)\b""",
            """${B}(sent|texted) (you )?(a |the )?code (by mistake|by accident)\b""",
        ),
    )

    val ACCOUNT_VERIFICATION = PhraseRule(
        id = "credentials.account_verification",
        type = SignalType.ACCOUNT_VERIFICATION_REQUEST,
        confidence = MEDIUM,
        negatable = true,
        patterns = listOf(
            """${B}(verifique|verificar|valide|validar|atualize|atualizar|actualize|confirme|confirmar|reative|reativar|desbloqueie|desbloquear|regularize)\W+(\w+\W+){0,2}(os |a |o |as )?(seus |sua |seu |suas |teus |tua )?(dados|conta|identidade|acesso|cartao|informacao|informacoes|morada|credenciais)""",
            """${B}(clique|carregue|toque|aceda|acede|clica|carrega)\W+(\w+\W+){0,3}(link|ligacao|aqui|endereco)\b""",
            """${B}(verify|update|confirm|validate|unlock|reactivate) your (account|identity|details|information|card)\b""",
            """${B}(click|tap) (here|the link|below)\b""",
        ),
    )

    val REMOTE_ACCESS = PhraseRule(
        id = "remote.access_request",
        type = SignalType.REMOTE_ACCESS_REQUEST,
        confidence = HIGH,
        negatable = true,
        patterns = listOf(
            """${B}(anydesk|teamviewer|quick ?support|rustdesk|supremo|ammyy|ultraviewer|airdroid)\b""",
            """${B}(instale|instala|instalar|descarregue|descarrega|descarregar|baixe|baixa)\W+(\w+\W+){0,3}(aplicacao|app|programa|ficheiro|apk)\b""",
            """${B}(acesso remoto|controlo remoto|partilh(e|a|ar) (o )?(seu |teu )?ecra|remote access|screen ?shar(e|ing)|install (this|the) app)\b""",
            """\.apk\b""",
        ),
    )

    val LURE = PhraseRule(
        id = "lure.prize_refund_investment",
        type = SignalType.PRIZE_REFUND_OR_INVESTMENT,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(ganhou|ganhaste|foi (o )?(selecionad|premiad|sortead)|foste (selecionad|premiad|sortead)|premio|sorteio|vencedor)""",
            """${B}(reembolso|devolucao (de|do) (imposto|irs|iva|pagamento)|tem direito a|valor a receber|cashback)\b""",
            """${B}(heranca|herdeiro|beneficiario de)\b""",
            """${B}(investimento|investir)\W+(\w+\W+){0,4}(garantid|seguro|lucro|rendimento|retorno)""",
            """${B}(lucro|rendimento|retorno) (garantido|diario|semanal|de \d+ ?%)""",
            """${B}(ganhar dinheiro|rendimento extra|trabalho (em casa|remoto) (bem )?pago|oportunidade unica)\b""",
            """${B}(you (have )?won|winner|prize|refund|inheritance|guaranteed (return|profit)|investment opportunity|claim your)\b""",
        ),
    )

    val TOOL_MANIPULATION = PhraseRule(
        id = "tools.injection_attempt",
        type = SignalType.INSTRUCTIONS_TO_AUTOMATED_TOOLS,
        confidence = MEDIUM,
        patterns = listOf(
            """${B}(ignore|ignora|ignorem|disregard) (all |todas |as )?(the |previous |prior |anteriores )?(instructions|instrucoes|regras|rules)""",
            """${B}(system prompt|you are an? (ai|assistant|language model)|es um (assistente|modelo))""",
            """${B}(classif(y|ique|ica) (this|esta) (message|mensagem) (as|como) (safe|segura|legit|legitima))""",
            """${B}(esta mensagem (e|foi) (segura|verificada|legitima)|this message is (safe|verified|legitimate))\b""",
        ),
    )

    val ALL: List<PhraseRule> = listOf(
        FAMILY_ADDRESS, FAMILY_IDENTITY_CLAIM, NEW_NUMBER,
        BANK_CLAIM, AUTHORITY_CLAIM, COURIER_CLAIM, TECH_SUPPORT_CLAIM,
        URGENCY, THREAT, DISTRESS, SECRECY, CHANNEL_SWITCH,
        MONEY_TRANSFER, PAYMENT_DEMAND, UNUSUAL_PAYMENT,
        CREDENTIAL_REQUEST, CODE_TO_SENDER, ACCOUNT_VERIFICATION, REMOTE_ACCESS,
        LURE, TOOL_MANIPULATION,
    )
}
