package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceCounterHelper(
    private val context: Context,
    private val onCommandRecognized: (VoiceCommand) -> Unit,
    private val onStatusChanged: (VoiceStatus) -> Unit
) {

    sealed class VoiceStatus {
        object Idle : VoiceStatus()
        object Listening : VoiceStatus()
        data class Recognized(val text: String) : VoiceStatus()
        data class Error(val message: String) : VoiceStatus()
    }

    sealed class VoiceCommand {
        data class Add(val amount: Int) : VoiceCommand()
        data class Subtract(val amount: Int) : VoiceCommand()
        object Reset : VoiceCommand()
        data class SetTarget(val target: Int) : VoiceCommand()
        data class Unknown(val rawText: String) : VoiceCommand()
    }

    private var speechRecognizer: SpeechRecognizer? = null

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isRecognitionAvailable) {
            onStatusChanged(VoiceStatus.Error("Speech recognition is not available on this device."))
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onStatusChanged(VoiceStatus.Listening)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        onStatusChanged(VoiceStatus.Error(message))
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0].lowercase(Locale.getDefault()).trim()
                            onStatusChanged(VoiceStatus.Recognized(text))
                            val cmd = parseCommand(text)
                            onCommandRecognized(cmd)
                        } else {
                            onStatusChanged(VoiceStatus.Idle)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'add 1', 'add 5', 'subtract 1', 'reset'...")
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onStatusChanged(VoiceStatus.Error("Failed to start speech recognizer: ${e.message}"))
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceCounter", "Error stopping voice recognizer", e)
        } finally {
            speechRecognizer = null
            onStatusChanged(VoiceStatus.Idle)
        }
    }

    private fun parseCommand(text: String): VoiceCommand {
        val clean = text.trim().lowercase(Locale.getDefault())

        // Reset commands
        if (clean == "reset" || clean == "clear" || clean == "zero" || clean.contains("reset counter")) {
            return VoiceCommand.Reset
        }

        // Add commands
        if (clean.contains("add") || clean.contains("plus") || clean.contains("increase")) {
            val amount = extractNumber(clean) ?: 1
            return VoiceCommand.Add(amount)
        }

        // Subtract commands
        if (clean.contains("subtract") || clean.contains("minus") || clean.contains("decrease") || clean.contains("remove")) {
            val amount = extractNumber(clean) ?: 1
            return VoiceCommand.Subtract(amount)
        }

        // Set target commands: "set target 100", "target 50", etc.
        if (clean.contains("target") || clean.contains("goal")) {
            val amount = extractNumber(clean)
            if (amount != null && amount > 0) {
                return VoiceCommand.SetTarget(amount)
            }
        }

        // Just numbers, e.g. "one", "five", "ten" -> treat as add
        val directNumber = extractNumber(clean)
        if (directNumber != null && directNumber > 0) {
            return VoiceCommand.Add(directNumber)
        }

        return VoiceCommand.Unknown(text)
    }

    private fun extractNumber(str: String): Int? {
        val digits = Regex("\\d+").find(str)?.value?.toIntOrNull()
        if (digits != null) return digits

        // Word-to-number dictionary
        return when {
            str.contains("one hundred") || str.contains("100") -> 100
            str.contains("fifty") || str.contains("50") -> 50
            str.contains("twenty") || str.contains("20") -> 20
            str.contains("ten") || str.contains("10") -> 10
            str.contains("five") || str.contains("5") -> 5
            str.contains("four") || str.contains("4") -> 4
            str.contains("three") || str.contains("3") -> 3
            str.contains("two") || str.contains("2") -> 2
            str.contains("one") || str.contains("1") -> 1
            else -> null
        }
    }
}
