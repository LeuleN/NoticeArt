# UserFlowDemo

Prototype implementation for the "Art of Noticing" mobile application user flows.

## Overview
This repository contains development work for implementing core user flows using Android Jetpack Compose and modern Android architecture components. The app focuses on capturing creative observations through a flexible, draft-based diary system.

---

## User Flow 1 – Entry Creation + Media Capture

Implementation includes:

- Draft-first entry creation system
- Title input with validation (required for publishing)
- Image attachment via system picker
- Multiple image support per entry
- Color extraction using tap-based pixel sampling (eyedropper)
- Multi-color capture per image with duplicate prevention
- Color preview (top 3) with full palette view in detail screen
- Observation input (multi-line, no limit)
- Automatic timestamp generation
- Entry persistence using Room database
- Entry display on home screen with media previews

---

## User Flow 2 – Entry Interaction (Edit, Delete, View)

Implementation includes:

- Entry detail screen with full view (image, colors, observations)
- Edit flow using draft reuse (entry → draft → update)
- Change detection for unsaved edits
- Auto-save during lifecycle interruptions
- Delete with confirmation dialog
- Undo delete using Snackbar
- Navigation between home, detail, and edit screens

---

## Draft System (Core Behavior)

- Draft automatically created when starting a new entry
- Draft persists across app restarts
- Only one active draft at a time

Safety behavior:

- Empty draft → automatically deleted
- Non-empty draft → discard confirmation shown
- Interruption (app close, background) → auto-save draft
- Reopening app → draft is restored

---

## Media System

### Image Support
- Select images from device gallery
- Multiple images per entry
- Immediate preview after selection
- No overwrite issues (images persist correctly)

### Color Capture
- Tap on image to extract colors
- Multiple colors per image supported
- Duplicate colors prevented
- Individual color deletion supported
- Colors stored per image (not globally)

---

## UX Features

- Draft persistence across app restarts
- Auto-save on lifecycle changes
- Discard confirmation for unsaved changes
- Delete confirmation with undo recovery
- Change detection during editing
- Scrollable observation input with no limit
- Grid-based home screen layout (2 columns)
- Entry cards display image/color preview, title, and date

---

## Architecture

The project follows an MVVM architecture:

UI (Jetpack Compose)  
→ ViewModel (StateFlow)  
→ Repository  
→ Room Database

Key design decisions:

- Draft-first workflow for all create/edit actions
- Separation of UI, state, and data layers
- State-driven updates
- Local persistence (offline-first)

---

## Tech Stack

- Kotlin
- Jetpack Compose
- Android ViewModel
- StateFlow
- Room Database

---

## Current Status

Prototype / active development stage

### Completed
- Entry creation flow (end-to-end)
- Draft system with auto-save
- Multi-image support
- Color capture system (stable)
- Edit, delete, and undo flows
- Observation input

### In Progress
- Media UX refinements
- Preview consistency across screens

### Remaining
- Audio recording feature
- Texture / crop feature
- Media indicators on home screen
- UI polish for final demo

---

## Summary

The application currently supports a full entry lifecycle including creation, editing, deletion, and recovery, with integrated media capture (images and colors) and strong data safety mechanisms. The current focus is on refining user experience and completing remaining media features.