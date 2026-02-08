# 📱 Interview Preparation – Android + Backend Service. A complete Android + Node.js backend project designed to help developers prepare for technical interviews. 
The Android app provides categorized interview questions with a clean Jetpack Compose UI, while the backend supports AI‑powered features, authentication, OCR, and more.

# 🌟 Features 
### **Android App** 
- Jetpack Compose UI (Material 3)
- MVVM architecture
- Categorized interview questions 
- Expandable Q&A cards 
- Firebase Authentication (Email/Password + Google) 

**Backend Service** 
- Node.js + Express
- Firebase Admin authentication
- OpenAI integration (GPT‑4o‑mini)
- OCR support (ffmpeg, Poppler, Tesseract)

# 🔥 1. Firebase Setup (Required)

## **1.1 Enable Authentication Providers**

In **Firebase Console → Authentication → Sign‑in method**:

- Enable **Email/Password**
- Enable **Google**

## **1.2 Add `google-services.json` to Android**

## **1.3 Backend: Firebase Admin Service Account**

In **Firebase Console → Project Settings → Service Accounts**:

1. Click **Generate new private key**
2. Download the JSON file

# 🖥️ 2. Backend Setup (Local)

## **2.1 Navigate to backend folder**

## 2.2 Install dependencies
- npm install
- Install ffmpeg
- Install Poppler (Recommended for PDF OCR fallback)
- Install Tesseract OCR (Recommended)

## 2.3 Create .env file
Add the following:
PORT=8080

# OpenAI
OPENAI_API_KEY=YOUR_OPENAI_KEY
OPENAI_MODEL=gpt-4o-mini
OPENAI_TRANSCRIBE_MODEL=gpt-4o-mini-transcribe

# Optional OCR helpers (recommended on Windows)
POPPLER_BIN=C:\tools\poppler\Library\bin
TESSERACT_BIN=C:\Program Files\Tesseract-OCR

# Firebase Admin Credentials (path to the JSON key file)
GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\serviceAccount.json

# Optional limits
DAILY_TRANSCRIPT_LIMIT=10

# ▶️ 3. Start the Backend
npm start

# 📱 4. Configure Android to Call the Local Backend
## Choose the correct base URL
- If using Android Emulator: http://10.0.2.2:8080/
- If using a physical device: http://<YOUR_PC_IP>:8080/ (Ensure phone and PC are on the same Wi‑Fi).
- Find your PC’s local IP: ipconfig

# 🔄5. Update Base URL in Android Code
  In Android Studio: Press Ctrl + Shift + F. Search for:127.0.0.1, baseUrl, 8080. Replace with:
- Emulator: http://10.0.2.2:8080/
- Physical device: http://<YOUR_PC_IP>:8080/



