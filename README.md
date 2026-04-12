# UserFlowDemo

Prototype implementation for the **“Art of Noticing” (NoticeArt)** mobile application user flows.

---

## Overview

This repository contains development work for implementing core user flows using Android Jetpack Compose and modern Android architecture.

The application is a creative diary system built around the interaction model:

**Notice → Capture → Reflect → Save → Revisit**

Users create flexible entries that support multiple forms of media while maintaining strong data safety through a draft-based workflow.

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

The app supports both **manual color picking** and **automatic color palette generation**.

#### Manual (Eyedropper)

- Tap-based pixel sampling
- Extracts exact HEX color from image
- Multiple colors per image
- Duplicate prevention
- Individual deletion supported

#### Automatic (Palette API)

- Uses Android Palette API to analyze image bitmap
- Performs **color clustering + frequency analysis**
- Identifies most visually dominant colors
- Returns top N colors (adjustable range)

Behavior:

- Colors are ranked by **population (pixel dominance)**
- Produces a balanced palette (not random pixels)
- Slight variation from manual picks is expected (cluster vs exact pixel)

Result:

- Fast, on-device color extraction (no API/network)
- Consistent and deterministic output
- User can refine by adding/removing colors manually

---

### Texture Capture & Suggestion System

The app supports **manual texture cropping** and **automatic texture detection**.

#### Manual Texture Crop

- Square crop (enforced for consistency)
- Gesture-based interaction:
    - Drag to move
    - Resize via corners
- Saved as user-defined texture

#### Automatic Texture Detection (OpenCV)

- Converts image to grayscale
- Applies **Laplacian filter (edge detection)**
- Computes **variance scores** to measure texture intensity

Detection Process:

1. Image divided into grid regions
2. Each region scored based on texture detail
3. Regions categorized:
    - High detail (edges, patterns)
    - Medium detail
    - Low detail (gradients, lighting)
4. Spatial filtering prevents clustering (no duplicates in same area)
5. Top N regions selected (adjustable 4–15)

Behavior:

- Produces **visually diverse texture samples**
- Avoids redundant or overlapping selections
- Balanced representation of the image

---

### Media Behavior

- Media is confirmed → then locked (immutable)
- Users cannot replace images directly

Allowed actions:
- Remove media
- Extract colors
- Extract textures

Inline actions:
- Extract Colors
- Extract Textures
- Remove

---

## User Flow 2 – Entry Interaction (Edit, Delete, View)

### Implementation includes:

- Entry detail screen with:
    - Images
    - Color palettes
    - Texture previews
    - Observations

- Edit flow using draft reuse:
    - Entry → copy → draft → update (no mutation)

- Change detection:
    - Prevents accidental loss
    - Triggers discard dialog only when needed

- Auto-save behavior:
    - Lifecycle-aware
    - No duplicate entries
    - No draft pollution during editing

- Delete system:
    - Confirmation dialog
    - Undo via Snackbar

### Navigation behavior

- Edit → Save → returns to detail
- Edit → Back → discard dialog → returns to detail
- New Entry → Back → draft handling logic

---

## Draft System

- Draft auto-created on “+”
- Persisted in Room
- Survives app restarts
- Only one active draft

### Safety behavior

- Empty draft → auto-delete
- Non-empty draft → discard confirmation
- Lifecycle interruption → auto-save
- Reopen app → draft restored

---

## Media System

### Image Support

- Multiple images per entry (`List<String>` URIs)
- Append-only (no overwrite)
- Works across drafts, edits, and saved entries

### Data Integrity (Critical)

- Colors and textures are tied to their source image
- No cross-image leakage
- Removing an image removes:
    - Associated colors
    - Associated textures

### Media Grid

```
[ + ] [ image1 ]
[ image2 ] [ image3 ]
```

- “+” always top-left
- Dynamic expansion
- Immediate preview

---

## Color & Texture Preview

### Entry View

- Segmented preview strips (not blended)
- Vertical + horizontal dividers
- Up to 3 preview items shown

### Detail View

- Full palette via bottom sheet
- Horizontal scrolling
- Clean separation between preview and full view

---

## UX Features

- Draft persistence across restarts
- Auto-save on lifecycle changes
- Discard confirmation (only when needed)
- Delete confirmation + undo
- Inline media actions (no extra navigation)
- Persistent scroll behavior
- Grid-based home layout (2 columns)

Entry cards display:
- Title
- Date
- Media indicators

---

## Additional Features (Recent Updates)

### Entry Filtering & Favorites

- Entries can be filtered by:
    - Alphabetical order
    - Favorites

- Favorite system:
    - Toggle heart icon on entry detail screen
    - Favorited entries can be filtered and prioritized

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
- Clear separation of UI and data layers

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

Prototype → **Feature-complete, demo-ready**

### Completed

- Full entry lifecycle (create, edit, delete, undo)
- Draft system with safety
- Multi-image support
- Per-image color system
- Texture detection + crop system
- Media grid with preview
- Inline media actions
- Entry filtering + favorites
- Data integrity guarantees

### In Progress

- Media UX polish
- Persistence edge cases

### Remaining

- Audio improvements
- Final UI consistency pass

---

## Summary

The application supports a complete creative workflow:

- Multi-modal capture (images, colors, textures)
- Smart palette + texture suggestion systems
- Strong data integrity (no mismatched media)
- Draft-based safety system (no data loss)
- Scalable architecture
- Real-time visual feedback

The system has evolved into a fully interactive creative capture tool aligned with its core design philosophy.