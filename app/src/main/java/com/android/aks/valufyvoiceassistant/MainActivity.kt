package com.android.aks.valufyvoiceassistant
import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var transcriptionTextView: TextView
    private lateinit var startRecordingButton: Button
    private lateinit var extractActionButton: Button
    private lateinit var shareButton: Button
    private lateinit var addToCalendarButton: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecording = false
    private val PERMISSION_REQUEST_CODE = 123
    private val CALENDAR_PERMISSION_REQUEST_CODE = 124
    private var extractedTasks: List<Task> = emptyList()

    data class Task(
        val title: String,
        val dateTime: Calendar?,
        val description: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupSpeechRecognizer()
        setupClickListeners()
        checkPermissions()
    }

    private fun initializeViews() {
        statusTextView = findViewById(R.id.statusTextView)
        transcriptionTextView = findViewById(R.id.transcriptionTextView)
        startRecordingButton = findViewById(R.id.startRecordingButton)
        extractActionButton = findViewById(R.id.extractActionButton)
        shareButton = findViewById(R.id.shareButton)
        addToCalendarButton = findViewById(R.id.addToCalendarButton)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateStatus("Listening...")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.get(0)?.let { result ->
                    appendTranscription(result)
                }
                if (isRecording) {
                    startListening()
                }
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                updateStatus("Error: $errorMessage")
                if (isRecording) {
                    startListening()
                }
            }

            override fun onBeginningOfSpeech() {
                updateStatus("Listening...")
            }

            override fun onEndOfSpeech() {
                updateStatus("Processing...")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.get(0)?.let { result ->
                    updateStatus("Partial: $result")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
        })
    }

    private fun setupClickListeners() {
        startRecordingButton.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        extractActionButton.setOnClickListener {
            extractActions()
        }

        shareButton.setOnClickListener {
            shareTranscription()
        }

        addToCalendarButton.setOnClickListener {
            checkCalendarPermissionAndAddEvents()
        }
    }

    private fun startRecording() {
        isRecording = true
        startRecordingButton.text = "Stop Recording"
        startListening()
    }

    private fun stopRecording() {
        isRecording = false
        startRecordingButton.text = "Start Recording"
        speechRecognizer?.stopListening()
        updateStatus("Recording stopped")
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun extractActions() {
        val text = transcriptionTextView.text.toString()
        extractedTasks = parseTasksFromText(text)

        val summary = buildString {
            append("\n=== EXTRACTED TASKS ===\n")
            extractedTasks.forEach { task ->
                append("- ${task.title}\n")
                if (task.dateTime != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    append("  Date: ${dateFormat.format(task.dateTime.time)}\n")
                }
                if (task.description.isNotEmpty()) {
                    append("  Details: ${task.description}\n")
                }
                append("\n")
            }
        }

        transcriptionTextView.append(summary)
    }

    private fun parseTasksFromText(text: String): List<Task> {
        val tasks = mutableListOf<Task>()
        val sentences = text.split(".")

        for (sentence in sentences) {
            if (sentence.isEmpty()) continue

            if (sentence.toLowerCase().contains("need to") ||
                sentence.toLowerCase().contains("should") ||
                sentence.toLowerCase().contains("todo") ||
                sentence.toLowerCase().contains("task")) {

                val calendar = extractDateTime(sentence)
                val taskTitle = sentence.trim()

                tasks.add(Task(
                    title = taskTitle,
                    dateTime = calendar,
                    description = "Extracted from meeting recording"
                ))
            }
        }

        return tasks
    }

    private fun extractDateTime(text: String): Calendar? {
        val dateTimePatterns = listOf(
            "on (\\w+ \\d{1,2})( at )?(\\d{1,2}:\\d{2})?\\s*(am|pm|AM|PM)?",
            "tomorrow( at )?(\\d{1,2}:\\d{2})?\\s*(am|pm|AM|PM)?",
            "next (\\w+)( at )?(\\d{1,2}:\\d{2})?\\s*(am|pm|AM|PM)?"
        )

        val calendar = Calendar.getInstance()

        for (pattern in dateTimePatterns) {
            val matcher = Pattern.compile(pattern).matcher(text)
            if (matcher.find()) {
                when {
                    text.toLowerCase().contains("tomorrow") -> {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    text.toLowerCase().contains("next") -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    else -> {
                        try {
                            val dateStr = matcher.group(1)
                            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                            calendar.time = dateFormat.parse(dateStr) ?: return null
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }

                try {
                    val timeStr = matcher.group(3)
                    if (!timeStr.isNullOrEmpty()) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val time = timeFormat.parse(timeStr)
                        if (time != null) {
                            val timeCalendar = Calendar.getInstance()
                            timeCalendar.time = time
                            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                        }
                    }
                } catch (e: Exception) {
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                }

                return calendar
            }
        }

        return null
    }

    private fun shareTranscription() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, transcriptionTextView.text.toString())
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun checkCalendarPermissionAndAddEvents() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CALENDAR),
                CALENDAR_PERMISSION_REQUEST_CODE
            )
        } else {
            addTasksToCalendar()
        }
    }

    private fun addTasksToCalendar() {
        if (extractedTasks.isEmpty()) {
            Toast.makeText(this, "No tasks to add to calendar", Toast.LENGTH_SHORT).show()
            return
        }

        var tasksAdded = 0
        for (task in extractedTasks) {
            val startMillis = task.dateTime?.timeInMillis ?: Calendar.getInstance().timeInMillis
            val endMillis = startMillis + 60 * 60 * 1000 // 1 hour duration

            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, task.title)
                .putExtra(CalendarContract.Events.DESCRIPTION, task.description)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                .putExtra(CalendarContract.Events.ALL_DAY, false)
                .putExtra(CalendarContract.Events.HAS_ALARM, true)

            startActivity(intent)
            tasksAdded++
        }

        Toast.makeText(
            this,
            "Adding $tasksAdded tasks to calendar",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun appendTranscription(text: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        transcriptionTextView.append("[$timestamp] $text\n")
    }

    private fun updateStatus(status: String) {
        statusTextView.text = "Status: $status"
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    updateStatus("Ready to record")
                } else {
                    updateStatus("Permissions required")
                }
            }
            CALENDAR_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addTasksToCalendar()
                } else {
                    Toast.makeText(
                        this,
                        "Calendar permission required to add events",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}