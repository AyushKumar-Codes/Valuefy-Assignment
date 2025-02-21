# Smart Voice Assistant for Professionals

A sophisticated Android application that converts meeting conversations into actionable tasks, automatically extracts calendar events, and generates meeting summaries. Built with Kotlin for seamless productivity enhancement.

## Features

- **Real-time Speech Recognition**: Converts spoken conversations into text
- **Intelligent Task Extraction**: Automatically identifies tasks and action items
- **Smart Date Parsing**: Recognizes date and time mentions in natural language
- **Calendar Integration**: Directly adds extracted tasks to device calendar
- **Meeting Summaries**: Generates structured summaries of conversations
- **Share Functionality**: Easy sharing of transcripts and summaries

## Screenshots

[Add your app screenshots here]

## Technical Stack

- **Language**: Kotlin
- **Platform**: Android
- **Minimum SDK**: 21 (Android 5.0)
- **Speech Recognition**: Android SpeechRecognizer API
- **Calendar Integration**: Android Calendar Provider

## Installation

1. Clone the repository
```bash
git clone https://github.com/AyushKumar-Codes/Valuefy-Assignment.git
```

2. Open the project in Android Studio

3. Build and run the application

## Required Permissions

The app requires the following permissions:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

## Usage

1. **Start Recording**
   ```
   - Launch the app
   - Tap "Start Recording" button
   - Speak clearly for best results
   ```

2. **Extract Tasks**
   ```
   - Tap "Extract Actions" to analyze the conversation
   - Review automatically identified tasks and dates
   ```

3. **Calendar Integration**
   ```
   - Tap "Add to Calendar" to create calendar events
   - Review and confirm event details
   ```

4. **Share Results**
   ```
   - Use the share button to export transcripts and summaries
   ```

## Code Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/voiceassistant/
│   │   │       ├── MainActivity.kt
│   │   │       └── [Other Kotlin files]
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml
│   │   │   └── [Other resource files]
│   │   └── AndroidManifest.xml
│   └── test/
└── build.gradle
```

## Key Components

```kotlin
// Speech Recognition
speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

// Task Parser
private fun parseTasksFromText(text: String): List<Task>

// Calendar Manager
private fun addTasksToCalendar()

// UI Components
private fun initializeViews()
```

## Future Enhancements

```
- [ ] Cloud backup for transcripts
- [ ] Multiple language support
- [ ] Custom task categories
- [ ] Meeting templates
- [ ] AI-powered task prioritization
- [ ] Team collaboration features
```

## Contributing

```bash
# Fork the repository
# Create your feature branch
git checkout -b feature/AmazingFeature

# Commit your changes
git commit -m 'Add some AmazingFeature'

# Push to the branch
git push origin feature/AmazingFeature

# Open a Pull Request
```

## License

```
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
```

## Contact

```
Ayush Kumar
Email: [Your Email]
Project Link: https://github.com/AyushKumar-Codes/Valuefy-Assignment
```

## Acknowledgments

```
- Android Speech Recognition API
- Calendar Provider API
- Kotlin Programming Language
- Android Studio IDE
```
