# UserFlowDemo

Prototype implementation for the "Art of Noticing" mobile application user flows.

## Overview
This repository contains development work for implementing core user flows using Android Jetpack Compose and modern Android architecture components. The app focuses on capturing creative observations through a flexible, draft-based diary system.

## User Flow 1 – Entry Creation + Media Capture
Implementation includes:

- Draft-first entry creation system
- Title input with validation (required for publishing)
- Image attachment via system picker
- Color extraction using tap-based pixel sampling
- Automatic timestamp generation
- Entry persistence using Room database
- Entry display on home screen with media indicators

## User Flow 2 – Entry Interaction (Edit, Delete, View)
Implementation includes:

- Entry detail screen with full view
- Edit flow using draft reuse (entry → draft → update)
- Delete with confirmation dialog
- Undo delete using Snackbar
- Navigation between home, detail, and edit screens

## Architecture
The project follows an MVVM architecture:

UI (Jetpack Compose)  
→ ViewModel (StateFlow)  
→ Repository  
→ Room Database

Key design decisions:

- Draft-first workflow for all create/edit actions
- Separation of UI, state, and data layers
- State-driven navigation
- Local persistence (offline-first)

## Tech Stack
- Kotlin
- :contentReference[oaicite:0]{index=0}
- Android ViewModel
- StateFlow
- :contentReference[oaicite:1]{index=1}

## UX Features
- Draft persistence across app restarts
- Auto-save on lifecycle changes
- Discard confirmation for unsaved changes
- Delete confirmation + undo recovery
- Minimal, card-based grid UI

## Status
Prototype / active development stage

Current focus:
- Expanding media support (audio, texture)
- Improving visual feedback for entries
- Refining creative capture workflow