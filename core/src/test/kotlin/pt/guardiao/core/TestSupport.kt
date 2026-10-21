package pt.guardiao.core

import kotlinx.coroutines.runBlocking
import pt.guardiao.core.model.Completeness
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RiskAssessment

fun msg(
    body: String,
    source: MessageSource = MessageSource.USER_SHARED,
    sender: String? = null,
    address: String? = null,
    completeness: Completeness = Completeness.COMPLETE,
) = IncomingMessage(source, body, senderDisplayName = sender, senderAddress = address, completeness = completeness)

fun analyse(m: IncomingMessage, analyzer: FraudAnalyzer = FraudAnalyzer()): RiskAssessment = runBlocking { analyzer.analyse(m) }

fun analyse(body: String): RiskAssessment = analyse(msg(body))
