# NoticeArt

Final implementation of the **“Art of Noticing” (NoticeArt)** mobile application.

---

## Overview

NoticeArt is a mobile creative diary system designed to help users capture, organize, and revisit visual observations through multiple forms of media.

The application is built around the interaction model:

**Notice → Capture → Reflect → Save → Revisit**

Users create flexible entries that support images, colors, textures, and audio while maintaining strong data safety through a draft-based workflow.

---

## Problem

Creative observations are often lost because traditional tools (notes apps, camera apps) do not support structured capture of visual elements like colors, textures, and context together.

NoticeArt solves this by providing a **media-first system** that allows users to capture and organize visual inspiration in a single, consistent workflow.

---

## Key Features

- Draft-first entry system (prevents data loss)
- Multi-image support per entry (no overwrite)
- Per-image color extraction (manual + automatic)
- Texture capture and OpenCV-based detection
- Audio recording and playback support
- Edit-safe workflow (no accidental overwrites)
- Delete with undo recovery
- Entry filtering and favorites
- Export to structured PDF

---

## Setup & Deployment Instructions

### 📱 Option 1: Install the App (Recommended)
<img src="./screenshots/noticeart_link.png" width="300">

Download the APK:
https://drive.google.com/file/d/1eorOxQnASYotOsx3S1g7wiapobNuUrk6/view

**Steps:**
1. Open the link on your Android device
2. Tap **Download**
3. If prompted, select **Download anyway**
4. Open the downloaded file
5. If installation is blocked:
  - Enable **Install from this source**
6. Tap **Install**
7. Open the app

**Note:**
- Android devices only
- No login required
- Tested on Android 9 – Android 14

---

### 💻 Option 2: Run from Source (Android Studio)

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle
4. Connect a physical device OR start an emulator
5. Press ▶ Run

**Recommended:** Android 11+ for best performance

---

### 🚀 How to Use the App

1. Enter your name on the welcome screen
2. Tap **New Entry**
3. Add media (image or audio)
4. Use tools to extract colors or textures
5. Add observations
6. Save the entry
7. View entries on the home screen
8. Export to PDF (optional)

---

### ⚠️ Notes

- This is a mobile-only application (Android)
- No user accounts or login required
- Ensure permissions are granted for media access

---


# Official Walkthrough - Notice Art
<img src="./screenshots/noticeart_walkthrough.png">

## Screenshots
The following screenshots highlight key features and workflows of the application.

---
### Welcome Screen - Onboarding

<img src="./screenshots/welcome_screen.png" width="200">

---

### Home Screen — Entry Preview Grid

<img src="./screenshots/homescreen.png" width="200">

---

### Entry Creation — New Draft

<img src="./screenshots/new_entry_complete.png" width="200">

---

### Adding Media — Image Selection

<img src="./screenshots/capture_media.png" width="200">

---

### Color Capture — Eyedropper + Palette

<img src="./screenshots/color_capture.png" width="200">

---

### Texture Capture — Crop Interface

<img src="./screenshots/texture_capture.png" width="200">

---

### Entry Detail — Full View

<img src="./screenshots/entry_detail.png" width="200">

---

### Image Detail — Color & Texture Preview

<img src="./screenshots/entry_detail_image_preview.png" width="200">

---

### Filtering & Favorites

<img src="./screenshots/filter_favorite_option.png" width="200">

---

### Export to PDF

<img src="./screenshots/export_pdf.png" width="200">

---

## User Flow 1 – Entry Creation + Media Capture

### Implementation includes:

- Draft-first entry creation system
- Title input with validation (required for publishing)
- Multi-image attachment (append-only, no overwrite)
- 2-column media grid with live preview
- Per-image media handling (no shared/global state)

---

### Color Capture & Suggestion System

#### Manual (Eyedropper)

- Tap-based pixel sampling
- Extracts exact HEX color from image
- Multiple colors per image
- Duplicate prevention
- Individual deletion supported

#### Automatic (Palette API)

- Uses Android Palette API
- Color clustering + frequency analysis
- Returns dominant colors (adjustable count)

**Behavior:**
- Ranked by population (pixel dominance)
- Balanced palette
- Slight variation from manual picks expected

---

### Texture Capture & Suggestion System

#### Manual Texture Crop

- Square crop (enforced)
- Drag + resize interaction
- Saved as user-defined texture

#### Automatic Texture Detection (OpenCV)

- Grayscale conversion
- Laplacian edge detection
- Variance scoring

**Detection Process:**

1. Image divided into grid regions
2. Regions scored by detail level
3. Spatial filtering prevents clustering
4. Top regions selected

---

### Media Behavior

- Media is confirmed → then locked
- Images cannot be replaced directly

Allowed actions:
- Remove media
- Extract colors
- Extract textures

---

## User Flow 2 – Entry Interaction (Edit, Delete, View)

### Implementation includes:

- Entry detail screen with:
  - Images
  - Color palettes
  - Texture previews
  - Observations
  - Audio

- Edit flow:
  - Entry → draft copy → update

- Change detection:
  - Prevents accidental loss
  - Shows discard dialog when needed

- Auto-save:
  - Lifecycle-aware
  - No duplicate entries

- Delete system:
  - Confirmation dialog
  - Undo via Snackbar

---

## Draft System

- Draft auto-created on “+”
- Persisted in Room
- Survives app restarts

### Safety Behavior

- Empty draft → auto-deleted
- Modified draft → discard confirmation
- Auto-save on interruption
- Draft restored on reopen

---

## Media System

### Image Support

- Multiple images per entry (`List<String>` URIs)
- Append-only (no overwrite)

### Data Integrity

- Colors and textures tied to source image
- No cross-image leakage
- Removing image removes associated data

---

## Audio System

- Record and attach audio to entries
- Playback supported in entry detail

**Limitations:**
- Single playback at a time
- Pause resets playback

---

## Additional Features

### Entry Filtering & Favorites

- Filter by:
  - Alphabetical
  - Favorites

- Favorite system:
  - Toggle heart icon
  - Prioritized filtering

---

## Architecture


```
UI (Jetpack Compose)
→ ViewModel (StateFlow)
→ Repository
→ Room Database
```

### Key Decisions

- Draft-first workflow
- Per-image media modeling
- State-driven UI
- Offline-first persistence
- Clear separation of concerns

---

## Tech Stack

- Kotlin
- Jetpack Compose
- ViewModel
- StateFlow
- Room Database
- Android Palette API
- OpenCV

---

## Current Status

**Final — Fully implemented, stable, and demo-ready**

### Completed

- Full entry lifecycle
- Draft system
- Multi-image support
- Per-image color system
- Texture detection + crop
- Audio integration
- Media grid with preview
- Entry filtering + favorites
- Export to PDF
- Data integrity guarantees

---

## Accepted Limitations

- Texture duplicates may occur
- Auto textures not editable
- Detection resets between sessions
- No cloud sync (offline-first)

---

## Team

- Leule Negatu
- Timothy Kim
- Sara Trufant
- Shirin Mohammadian

---

## Summary

NoticeArt is a fully implemented creative capture system that supports:

- Multi-modal media capture (images, colors, textures, audio)
- Intelligent color and texture suggestions
- Strong data safety through a draft-first workflow
- Consistent behavior across all screens

The system demonstrates a complete, stable, and user-safe workflow from capture to export.
