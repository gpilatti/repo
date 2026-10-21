package pt.guardiao.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pt.guardiao.core.model.Completeness
import pt.guardiao.core.model.IncomingMessage
import pt.guardiao.core.model.LinkRef
import pt.guardiao.core.model.MessageSource
import pt.guardiao.core.model.RiskLevel
import pt.guardiao.core.model.SignalType
import java.io.File

/** A labelled synthetic test case from src/test/resources/scenarios. */
data class Scenario(
    val id: String,
    val scenario: Int,
    val label: String,
    val language: String,
    val message: IncomingMessage,
    val expectedMin: RiskLevel,
    val expectedMax: RiskLevel,
    val mustInclude: List<SignalType>,
    val mustNotInclude: List<SignalType>,
    val why: String,
)

object Scenarios {
    val all: List<Scenario> by lazy { load() }

    private fun load(): List<Scenario> {
        val dir = File(javaClass.classLoader.getResource("scenarios")!!.toURI())
        return dir.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }.flatMap { file ->
            Json.parseToJsonElement(file.readText()).jsonArray.map { parse(it.jsonObject) }
        }
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

    private fun JsonObject.list(key: String): List<String> =
        (this[key] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()

    private fun parse(o: JsonObject): Scenario {
        val links = (o["links"] as? JsonArray)?.map {
            val l = it.jsonObject
            LinkRef(l.str("text"), l.str("href")!!)
        } ?: emptyList()
        val sender = o.str("sender")
        val source = MessageSource.valueOf(o.str("source")!!)
        val message = IncomingMessage(
            source = source,
            body = o.str("body")!!,
            senderDisplayName = sender,
            senderAddress = o.str("senderAddress"),
            subject = o.str("subject"),
            isGroupConversation = false,
            links = links,
            attachmentNames = o.list("attachments"),
            completeness = Completeness.valueOf(o.str("completeness")!!),
        )
        return Scenario(
            id = o.str("id")!!,
            scenario = o.str("scenario")!!.toInt(),
            label = o.str("label")!!,
            language = o.str("language")!!,
            message = message,
            expectedMin = RiskLevel.valueOf(o.str("expectedMin")!!),
            expectedMax = RiskLevel.valueOf(o.str("expectedMax")!!),
            mustInclude = o.list("mustInclude").map { SignalType.valueOf(it) },
            mustNotInclude = o.list("mustNotInclude").map { SignalType.valueOf(it) },
            why = o.str("why")!!,
        )
    }
}
