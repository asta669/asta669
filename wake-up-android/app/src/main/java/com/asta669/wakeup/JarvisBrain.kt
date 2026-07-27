package com.asta669.wakeup

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Generates the spoken morning brief. Uses Google's free Gemini API when a key
 *  is set; otherwise (or on any error) falls back to a built-in template so the
 *  brief always works. */
object JarvisBrain {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** Callback delivers the brief text on a background thread. */
    fun generate(name: String, routine: String, events: String,
                 apiKey: String, model: String, onResult: (String) -> Unit) {
        val time = currentTime()
        val day = currentDay()

        if (apiKey.isBlank()) {
            onResult(fallbackBrief(name, routine, events, time, day))
            return
        }

        val prompt = buildPrompt(name, routine, events, time, day)
        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
        }.toString().toRequestBody(JSON)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "$model:generateContent?key=$apiKey"

        val request = Request.Builder().url(url).post(body).build()
        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(fallbackBrief(name, routine, events, time, day))
            }

            override fun onResponse(call: Call, response: Response) {
                val text = try {
                    response.use {
                        if (!it.isSuccessful) return@use null
                        val root = JSONObject(it.body?.string() ?: "")
                        root.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim()
                    }
                } catch (e: Exception) {
                    null
                }
                onResult(text?.takeIf { it.isNotBlank() }
                    ?: fallbackBrief(name, routine, events, time, day))
            }
        })
    }

    private fun buildPrompt(name: String, routine: String, events: String,
                            time: String, day: String): String {
        val evLine = if (events.isBlank()) "aucun rendez-vous prévu" else events
        val routineLine = if (routine.isBlank()) "aucune routine particulière" else routine
        return """
            Tu es Jarvis, l'assistant personnel de $name. Parle en français, avec
            élégance, chaleur et bienveillance. Vouvoie l'utilisateur et appelle-le
            « Monsieur ». Rédige un briefing matinal court (4 à 6 phrases), motivant,
            à lire à voix haute — pas de listes à puces, pas d'emojis, un ton naturel
            et oral.

            Contexte :
            - Heure : $time
            - Jour : $day
            - Rendez-vous du jour : $evLine
            - Routine à encourager : $routineLine

            Commence par saluer Monsieur, mentionne l'heure et le jour, rappelle
            brièvement les rendez-vous s'il y en a, encourage la routine, et termine
            par une phrase motivante pour bien démarrer la journée.
        """.trimIndent()
    }

    /** Offline / no-key brief — still personal, built from the same data. */
    fun fallbackBrief(name: String, routine: String, events: String,
                      time: String, day: String): String {
        val sb = StringBuilder()
        sb.append("Bonjour $name. Il est $time, nous sommes $day. ")
        if (events.isNotBlank()) {
            sb.append("Au programme aujourd'hui : $events. ")
        } else {
            sb.append("Aucun rendez-vous prévu aujourd'hui, la journée est à vous. ")
        }
        if (routine.isNotBlank()) {
            sb.append("N'oubliez pas votre routine : $routine. ")
        }
        val closings = listOf(
            "Debout, la journée vous attend. Excellente journée, Monsieur.",
            "Prenez une grande inspiration et lancez-vous. Belle journée à vous.",
            "Vous êtes prêt. Faites-en une journée mémorable, Monsieur.",
            "Un pas après l'autre, et tout ira bien. Bonne journée, Monsieur."
        )
        sb.append(closings[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % closings.size])
        return sb.toString()
    }

    private fun currentTime(): String {
        val c = Calendar.getInstance()
        return String.format("%dh%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    private fun currentDay(): String {
        val days = arrayOf("dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi")
        return days[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
    }
}
