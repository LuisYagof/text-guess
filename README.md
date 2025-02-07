# Text Guess

Text Guess is a personal Android project that experiments with Google ML Kit and Google Cloud Vision APIs for text recognition. Built with Kotlin, this app explores the capabilities of on-device and cloud-based text recognition to extract and process text from images.

## Features

- On-Device Text Recognition: Uses Google ML Kit's text-recognition library for fast, offline text extraction.
- Cloud-Based Text Recognition: Leverages Google Cloud Vision API for advanced text recognition and analysis.
- Camera Integration: Utilizes Android's CameraX library for seamless image capture and processing.
- gRPC Support: Implements gRPC for efficient communication with the Cloud Vision API.

## Technologies Used

- Kotlin: Primary programming language.
- Google ML Kit: For on-device text recognition.
- Google Cloud Vision API: For cloud-based text recognition.
- CameraX: For camera functionality.
- gRPC: For communication with Google Cloud services.

## Setup

1. Clone the repository:

```bash
git clone https://github.com/your-username/text-guess.git
```

2. Open the project in Android Studio.

3. Add your Google Cloud API credentials in this folder:

```bash
app/src/main/res/raw/credentials.json
```

4. Build and run the app on an Android device or emulator.
