package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

class VoiceCounterHelper(
    private val context: Context,
    private val getCurrentCount: () -> Int,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit,
    private val onStatusChanged: (VoiceStatus) -> Unit
) {

    sealed class VoiceStatus {
        object Stopped : VoiceStatus()
        data class Listening(val heardText: String = "", val isSpeaking: Boolean = false) : VoiceStatus()
        data class Recognized(val text: String) : VoiceStatus()
        data class Error(val message: String) : VoiceStatus()
        object PermissionRequired : VoiceStatus()
    }

    sealed class VoiceCommand {
        data class Add(val amount: Int) : VoiceCommand()
        data class Subtract(val amount: Int) : VoiceCommand()
        object Reset : VoiceCommand()
        data class SetTarget(val target: Int) : VoiceCommand()
        data class SetExact(val value: Int) : VoiceCommand()
        data class Unknown(val rawText: String) : VoiceCommand()
    }

    @Volatile
    var isVoiceCounterActive: Boolean = false
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastResultKey: String = ""
    private var lastResultTime: Long = 0L
    private var lastHeardDisplay: String = ""

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            isVoiceCounterActive = false
            onStatusChanged(VoiceStatus.PermissionRequired)
            return
        }

        if (!isRecognitionAvailable) {
            isVoiceCounterActive = false
            onStatusChanged(VoiceStatus.Error("Speech recognition is not available on this device."))
            return
        }

        // Activate continuous listening session
        isVoiceCounterActive = true
        lastHeardDisplay = ""
        onStatusChanged(VoiceStatus.Listening(heardText = "Listening..."))

        mainHandler.removeCallbacksAndMessages(null)
        launchSpeechRecognizer()
    }

    private fun launchSpeechRecognizer() {
        if (!isVoiceCounterActive) return

        // Always run on main looper
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { launchSpeechRecognizer() }
            return
        }

        // Clean up any previous recognizer safely
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("VoiceCounter", "Error clearing recognizer: ${e.message}")
        }
        speechRecognizer = null

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (isVoiceCounterActive) {
                            onStatusChanged(VoiceStatus.Listening(heardText = lastHeardDisplay))
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        if (isVoiceCounterActive) {
                            onStatusChanged(VoiceStatus.Listening(heardText = lastHeardDisplay, isSpeaking = true))
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        Log.d("VoiceCounter", "SpeechRecognizer error: $error")

                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            isVoiceCounterActive = false
                            onStatusChanged(VoiceStatus.PermissionRequired)
                            return
                        }

                        if (!isVoiceCounterActive) {
                            onStatusChanged(VoiceStatus.Stopped)
                            return
                        }

                        // For timeouts, pauses or no-match (user temporarily silent):
                        // Automatically restart recognizer with small delay to stay continuously listening
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                            SpeechRecognizer.ERROR_NO_MATCH -> 150L
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 400L
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 1000L
                            else -> 300L
                        }

                        scheduleNextRecognition(delay)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val now = System.currentTimeMillis()
                            val key = matches.take(3).joinToString("||").lowercase(Locale.getDefault())

                            // Duplicate recognition prevention within 1200ms
                            if (key != lastResultKey || (now - lastResultTime) > 1200L) {
                                lastResultKey = key
                                lastResultTime = now

                                val (cmd, recognizedPhrase) = parseMatches(matches)
                                lastHeardDisplay = "Heard: $recognizedPhrase"
                                onStatusChanged(VoiceStatus.Recognized(recognizedPhrase))
                                onCommandRecognized(cmd, recognizedPhrase)
                            }
                        }

                        if (isVoiceCounterActive) {
                            scheduleNextRecognition(150L)
                        } else {
                            onStatusChanged(VoiceStatus.Stopped)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (!isVoiceCounterActive) return
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = partials?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onStatusChanged(VoiceStatus.Listening(heardText = "Hearing: $text...", isSpeaking = true))
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceCounter", "Failed to launch recognizer", e)
            if (isVoiceCounterActive) {
                scheduleNextRecognition(500L)
            } else {
                onStatusChanged(VoiceStatus.Error("Voice counter error: ${e.message}"))
            }
        }
    }

    private fun scheduleNextRecognition(delayMs: Long) {
        if (!isVoiceCounterActive) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isVoiceCounterActive) {
                launchSpeechRecognizer()
            }
        }, delayMs)
    }

    fun stopListening() {
        isVoiceCounterActive = false
        mainHandler.removeCallbacksAndMessages(null)

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceCounter", "Error stopping voice recognizer", e)
        } finally {
            speechRecognizer = null
            onStatusChanged(VoiceStatus.Stopped)
        }
    }

    fun destroy() {
        stopListening()
    }

    private fun parseMatches(matches: List<String>): Pair<VoiceCommand, String> {
        val currentCount = getCurrentCount()

        // 1. Check all candidates for explicit commands first
        for (phrase in matches) {
            val clean = phrase.trim().lowercase(Locale.getDefault())

            // Reset
            if (clean == "reset" || clean == "clear" || clean == "zero" ||
                clean.contains("reset counter") || clean.contains("clear all") ||
                clean == "रीसेट" || clean == "साफ" || clean == "शून्य" || clean == "ज़ीरो" || clean == "जीरो"
            ) {
                return Pair(VoiceCommand.Reset, phrase)
            }

            // Set target: "set target 100", "target 50", "लक्ष्य 100"
            if (clean.contains("target") || clean.contains("goal") || clean.contains("लक्ष्य")) {
                val target = extractNumber(clean)
                if (target != null && target > 0) {
                    return Pair(VoiceCommand.SetTarget(target), phrase)
                }
            }

            // Subtract / Minus: "subtract one", "minus five", "एक घटाओ", "कम करो"
            if (clean.contains("subtract") || clean.contains("minus") || clean.contains("decrease") ||
                clean.contains("remove") || clean.contains("घटाओ") || clean.contains("कम करो")
            ) {
                val amount = extractNumber(clean) ?: 1
                return Pair(VoiceCommand.Subtract(amount), phrase)
            }

            // Add phrases: "add one", "plus five", "count one", "एक जोड़ो", "दो जोड़ो"
            if (clean.contains("add") || clean.contains("plus") || clean.contains("count") ||
                clean.contains("increase") || clean.contains("जोड़ो") || clean.contains("जोड़ो") ||
                clean.contains("बढ़ाओ") || clean.contains("गिनो") || clean.contains("और")
            ) {
                val amount = extractNumber(clean) ?: 1
                return Pair(VoiceCommand.Add(amount), phrase)
            }
        }

        // 2. Direct numbers: "one", "two", "three", "four", "एक", "दो", "तीन", "चार", "पाँच"
        for (phrase in matches) {
            val clean = phrase.trim().lowercase(Locale.getDefault())
            val number = extractNumber(clean)
            if (number != null && number > 0) {
                // If user speaks sequential counting number (e.g. at 0 says 1, at 1 says 2, at 2 says 3...):
                // Set directly to that number!
                return if (number == currentCount + 1) {
                    Pair(VoiceCommand.SetExact(number), phrase)
                } else if (number == 1) {
                    // Saying "one" / "ek" to count an item: adds 1
                    Pair(VoiceCommand.Add(1), phrase)
                } else {
                    // Direct number spoken, e.g. "two" or "five"
                    Pair(VoiceCommand.Add(number), phrase)
                }
            }
        }

        // Unknown
        val first = matches.firstOrNull() ?: ""
        return Pair(VoiceCommand.Unknown(first), first)
    }

    private fun extractNumber(str: String): Int? {
        val clean = str.trim().lowercase(Locale.getDefault())

        // 1. Direct digits
        val digits = Regex("\\d+").find(clean)?.value?.toIntOrNull()
        if (digits != null) return digits

        // 2. Hindi Devanagari numbers
        when {
            clean.contains("एक सौ") || clean.contains("सौ") -> return 100
            clean.contains("पचास") -> return 50
            clean.contains("चालीस") -> return 40
            clean.contains("तीस") -> return 30
            clean.contains("पच्चीस") -> return 25
            clean.contains("बीस") -> return 20
            clean.contains("उन्नीस") -> return 19
            clean.contains("अठारह") -> return 18
            clean.contains("सत्रह") -> return 17
            clean.contains("सोलह") -> return 16
            clean.contains("पंद्रह") -> return 15
            clean.contains("चौदह") -> return 14
            clean.contains("तेरह") -> return 13
            clean.contains("बारह") -> return 12
            clean.contains("ग्यारह") -> return 11
            clean.contains("दस") -> return 10
            clean.contains("नौ") -> return 9
            clean.contains("आठ") -> return 8
            clean.contains("सात") -> return 7
            clean.contains("छह") || clean.contains("छः") || clean.contains("छे") -> return 6
            clean.contains("पाँच") || clean.contains("पांच") || clean.contains("पाच") -> return 5
            clean.contains("चार") -> return 4
            clean.contains("तीन") -> return 3
            clean.contains("दो") -> return 2
            clean.contains("एक") -> return 1
        }

        // 3. English numbers & phonetic matches
        return when {
            clean.contains("one hundred") || clean.contains("hundred") -> 100
            clean.contains("ninety") -> 90
            clean.contains("eighty") -> 80
            clean.contains("seventy") -> 70
            clean.contains("sixty") -> 60
            clean.contains("fifty") -> 50
            clean.contains("forty") -> 40
            clean.contains("thirty") -> 30
            clean.contains("twenty") -> 20
            clean.contains("nineteen") -> 19
            clean.contains("eighteen") -> 18
            clean.contains("seventeen") -> 17
            clean.contains("sixteen") -> 16
            clean.contains("fifteen") -> 15
            clean.contains("fourteen") -> 14
            clean.contains("thirteen") -> 13
            clean.contains("twelve") -> 12
            clean.contains("eleven") -> 11
            clean.contains("ten") || clean.contains("dus") || clean.contains("das") -> 10
            clean.contains("nine") || clean.contains("nau") -> 9
            clean.contains("eight") || clean.contains("ate") || clean.contains("aath") -> 8
            clean.contains("seven") || clean.contains("saat") -> 7
            clean.contains("six") || clean.contains("chhah") || clean.contains("che") -> 6
            clean.contains("five") || clean.contains("paanch") || clean.contains("panch") -> 5
            clean.contains("four") || clean.contains("for") || clean.contains("fore") || clean.contains("char") || clean.contains("chaar") -> 4
            clean.contains("three") || clean.contains("tree") || clean.contains("free") || clean.contains("teen") || clean.contains("tin") -> 3
            clean.contains("two") || clean.contains("to") || clean.contains("too") || clean == "do" -> 2
            clean.contains("one") || clean.contains("won") || clean.contains("ek") || clean == "ikk" -> 1
            else -> null
        }
    }
}
