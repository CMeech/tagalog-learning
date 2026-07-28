A project for setting up Anki to ingest Tagalog vocab and scripts for power-learning the language.

The plan is to include audio generation as well via some API service.


# Architecture

SQLite
    ↓
Python/Go/Node application
    ↓
AI Provider
    ↓
TTS Provider
    ↓
Generate media
    ↓
Generate CSV
    ↓
Anki Import