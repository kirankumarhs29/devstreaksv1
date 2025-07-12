# 🚀 DevStreaks – Gamified Kotlin Developer Learning App

**DevStreaks** is a Kotlin Multiplatform app that helps Gen Z developers build consistent coding habits through daily challenges, gamified XP, AI-powered feedback, and community streak sharing.

---

## 📱 Features

- ✅ **Daily Developer Challenges**
- 🧠 **AI Feedback** on journal reflections & code
- 🛤️ **Challenge Paths** like Android, Backend, AI
- 📈 **Progress Tracker** with XP & streaks
- 🤖 **AI Resume Reviewer** (coming soon)
- 🌍 **Community Feed** (in progress)
- 🎯 **Learning Intent Onboarding**
- 🔐 **Login / Signup**
- 🧑‍💻 **Profile with Preferences**
- 🚀 **Gamification System (XP + Streaks)**

---

## 🧩 Tech Stack

| Platform          | Technology              |
|------------------|--------------------------|
| UI               | Compose Multiplatform    |
| Shared Logic     | Kotlin Multiplatform     |
| Android & iOS    | Android Studio + Xcode   |
| State Management | `StateFlow`              |
| Dependency Injection | `Koin`              |
| Local DB         | `SQLDelight`             |
| AI Integration   | Gemini LLM (via API)     |
| Auth             | Custom AuthService       |

---

## 🔧 Project Structure
composeApp/
├── features/ # Home, Journal, Paths, etc.
├── auth/ # Login/Signup logic
├── llm/ # Gemini integration
├── database/ # SQLDelight DB
├── di/ # Koin modules
├── navigation/ # App routes & navigation
├── ComposeNavGraph.kt
iosApp/ # iOS entry point
androidApp/ # Android-specific code




---

## 🚀 Getting Started

### ✅ Prerequisites

- [Android Studio Hedgehog+](https://developer.android.com/studio)
- JDK 17
- CocoaPods (for iOS builds on macOS)
- Xcode (for iOS build, optional)

### 🧑‍💻 Android Setup (Windows/macOS)

```bash
git clone https://github.com/kirankumarhs29/devstreaks.git
cd devstreaks
./gradlew build
./gradlew installDebug


🍏 iOS Setup (macOS Only)
bash
Copy
Edit
cd iosApp
pod install --repo-update
cd ..
./gradlew :composeApp:syncFramework
open iosApp/iosApp.xcworkspace

💰 Monetization Plan
Free Tier: Basic challenges + limited streaks

Premium ₹RRR/month: All paths, AI resume help, feedback

Future: B2B bootcamp integrations, campus packs

📅 Launch Roadmap
✅ MVP Completed (June 2025)

🚀 Soft Launch: July 15

📲 Instagram + LinkedIn for growth

🤝 Contributing
Open to collaborators for:

UI/UX polish

AI feedback optimization

Community feed integration

Submit issues or PRs on GitHub

📸 Screens (WIP Preview)
Screen	Description
🏠 Home	Dashboard with today's challenge, XP
🛤️ Paths	Stack-specific challenge tracks
📝 Journal	Reflect + AI summary
🌍 Feed	Community streaks (coming soon)
📊 Progress	XP & performance tracking

🙌 Made with ❤️ by Kiran Kumar H S

