# Todo App Design Spec

> A personal Android to-do list app with task management, calendar reminders, subtasks, categories, dual-links, and advanced repeat rules. Inspired by Claude's warm, refined visual style.

## Overview

A free, privacy-first to-do list app for personal use on Android. All data stored locally on the device. Reminders handled by the Android system calendar. Visual design inspired by Claude's warm aesthetic with Source Han Serif (思源宋体) as the primary Chinese typeface.

## Technical Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Database:** Room (structured data) + DataStore (settings/preferences)
- **Architecture:** MVVM + Repository pattern
- **Minimum SDK:** API 26 (Android 8.0)
- **Target SDK:** Latest stable
- **Build System:** Gradle with Kotlin DSL

## Architecture

```
UI Layer (Jetpack Compose)
  └── TaskPage · CategoryPage · LinkPage · SettingsPage · CreateEditPage
       │
ViewModel Layer
  └── TaskViewModel · CategoryViewModel · SettingsViewModel
       │
Repository Layer
  └── TaskRepository · CategoryRepository · CalendarRepository
       │
Data Layer
  └── Room Database · DataStore · Android Calendar API
```

- **Room** stores all structured data: tasks, categories, subtasks, task links
- **DataStore** stores lightweight user preferences: theme, sort order, calendar sync toggle
- **Android Calendar API** handles reminders — each task with a reminder time creates a system calendar event
- **Repository pattern** isolates data sources so ViewModels don't care where data comes from

## Data Model

### Category

| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| name | String | Display name |
| parentId | Long? (FK → Category.id) | Null = top-level category, non-null = sub-tag |
| color | String | Hex color code |
| icon | String | Icon identifier |
| sortOrder | Int | For drag-to-reorder |
| createdAt | Long | Unix timestamp |

Two-level structure: top-level categories can have sub-tags as children. All categories are user-created and freely editable/deletable.

### Task

| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| title | String | Task name, required |
| description | String? | Optional notes/details |
| categoryId | Long? (FK → Category.id) | Null = uncategorized |
| parentId | Long? (FK → Task.id) | Null = top-level task, non-null = subtask |
| dueDate | Long? | Due date (Unix timestamp) |
| reminderTime | Long? | Reminder time (Unix timestamp) |
| repeatRule | String? | Repeat rule string (see Repeat Rules section) |
| priority | Int | 0=none, 1=low, 2=medium, 3=high |
| sortOrder | Int | For custom ordering |
| isCompleted | Boolean | Completion status |
| calendarEventId | Long? | Linked system calendar event ID |
| createdAt | Long | Unix timestamp |
| completedAt | Long? | When marked complete |

Subtasks are one level deep (no recursive nesting). A subtask's `parentId` points to its parent task.

### TaskLink (Dual-Link)

| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| sourceTaskId | Long (FK → Task.id) | The linking task |
| targetTaskId | Long (FK → Task.id) | The linked task |
| createdAt | Long | Unix timestamp |

Bidirectional: when querying links for a task, search both `sourceTaskId` and `targetTaskId`. Cannot link a task to itself.

## Pages & Navigation

Bottom navigation bar with 4 tabs, plus task creation/edit as overlay pages.

### ① Task Home (Default Page)

- **Header:** Current date + count of pending tasks
- **Tab Switch:** "Timeline" view / "Category" view
- **Timeline View:** All tasks sorted by due date
- **Category View:** Tasks grouped under collapsible category sections
- **Filters:** Priority, category, completion status
- **Floating Action Button (+):** Opens quick-create bottom sheet
- **Task Card Displays:** Title, category tag, time, repeat indicator, subtask progress bar

### ② Task Detail / Edit Page

- Edit title and description
- Select category/sub-tag
- Set due date and reminder time
- Set repeat rule (including advanced rules)
- Set priority
- Manage subtasks (add/check/delete)
- Manage task links (search and link other tasks)
- Linked tasks shown at bottom, tap to navigate

### ③ Category Management Page

- Two-level tree display
- Drag to reorder
- Edit name, color, icon per category
- Long-press to delete (prompts to migrate or co-delete tasks)

### ④ Link View Page

- List view showing all tasks and their linked tasks
- Tap to navigate to task detail
- Provides overview of task relationships

### ⑤ Settings Page

- Theme: Light / Dark / Follow System
- Sort preference: By time / Priority / Creation time
- Calendar sync toggle
- Data export (JSON backup to device storage)
- Data import (restore from JSON backup)

### ⑥ Quick Create Task (Bottom Sheet)

- Tap `+` to open bottom sheet
- Quick title input
- Optional time and category
- "More options" navigates to full edit page

## Calendar Integration & Reminders

### Flow

1. User creates/edits a task with a reminder time
2. CalendarRepository calls Android Calendar API to create an event in the user's default calendar
3. Event details: title = task title, time = reminder time, alarm = system notification, notes = task description + deep link
4. The `calendarEventId` is stored on the Task for future sync
5. System calendar fires notification at the right time
6. User taps notification → deep link opens app to that task's detail page

### Rules

- **First launch:** Request calendar read/write permission with onboarding guidance
- **Task linked to event:** Editing/deleting a task updates/deletes the corresponding calendar event
- **Repeat tasks:** Calendar event gets matching repeat rule, system handles recurrence
- **Deep link:** Calendar event notes contain a link that navigates to the task detail page in-app
- **No reminder time:** Tasks with only a date (no time) default to 09:00 on that day
- **No date or time:** No calendar event created, only shown in-app
- **Calendar disabled:** App works normally, just no calendar events. Settings page shows re-enable prompt
- **Event deleted externally:** Task unaffected, event recreated on next edit

## Repeat Rule Engine

### Storage Format

Rules stored as structured strings in `Task.repeatRule`:

| Rule Type | Example | Meaning |
|-----------|---------|---------|
| Daily | `FREQ=DAILY` | Every day |
| Weekly | `FREQ=WEEKLY;BYDAY=MO,WE,FR` | Every Mon, Wed, Fri |
| Monthly | `FREQ=MONTHLY;BYMONTHDAY=15` | 15th of every month |
| Yearly | `FREQ=YEARLY;BYMONTH=6;BYDAY=1` | June 1st every year |
| Custom interval | `FREQ=DAILY;INTERVAL=3` | Every 3 days |
| Nth workday | `FREQ=MONTHLY;BYWORKDAY=2` | 2nd workday of month |
| Last weekday | `FREQ=MONTHLY;BYDAY=-1FR` | Last Friday of month |

### UI

- Quick select buttons: Daily, Weekly, Monthly, Yearly
- Advanced panel: interval input, weekday checkboxes, special rules (Nth workday, last weekday X)

### Lifecycle

- Completing a repeat task → app calculates next occurrence date and creates a new task instance
- Each instance is independent with its own state, subtasks, and completion status
- Individual instances can be deleted without affecting future ones
- Calendar events created per instance

## Visual Design

### Design Language

Inspired by Claude's warm, refined aesthetic:

- **Warm palette:** Cream and beige backgrounds instead of cold white
- **Restrained color use:** Let content speak, accent colors only where meaningful
- **Serif typography:** Source Han Serif (思源宋体) as primary Chinese typeface for elegance
- **Generous whitespace:** Comfortable spacing for unhurried interaction
- **Clear hierarchy:** Font size, weight, and color establish information layers

### Color Palette

| Name | Hex | Usage |
|------|-----|-------|
| Warm White | #FAF7F2 | Background (light) |
| Cream | #F5F0E8 | Secondary background |
| Ink | #2D2A24 | Primary text |
| Amber | #C4713B | Primary accent / interactive |
| Stone | #8A8478 | Secondary text |
| Indigo | #7C5CAD | Category: Work |
| Teal | #4A8C6F | Category: Life |

Dark mode uses warm dark tones (#1A1816 background, #252220 card surfaces) with adjusted accent colors.

### Typography

| Role | Font | Size | Weight |
|------|------|------|--------|
| Title | Source Han Serif SC | 24-26px | Bold (700) |
| Body | Source Han Serif SC | 15-16px | Regular (400) |
| Caption | Source Han Serif SC | 12-13px | Regular (400) |
| Tag/Label | Inter / System Sans | 10-11px | Semibold (600), Uppercase |

## Error Handling & Data Safety

### Backup & Restore

- Settings page: "Export Backup" → all data exported as JSON to device storage
- Settings page: "Import Backup" → restore from JSON file
- Simple, reliable, zero cost

### Deletion Protection

- Delete task → confirmation dialog: "Also delete linked tasks and subtasks?"
- Delete category with tasks → options: "Move tasks to another category" or "Delete all tasks too"
- All deletions support **undo** via Snackbar (5-second window)

### Calendar Sync Error Handling

- Permission revoked → app works normally, no calendar events created, settings shows re-enable prompt
- Calendar event missing (user deleted externally) → task unaffected, event recreated on next edit

### Input Validation

- Task title cannot be empty
- Subtasks limited to 1 level (no recursive nesting)
- Cannot link a task to itself
- Invalid repeat rule → user-facing error message

## Cost

**Zero.** Everything is free:

- Android Studio: free
- Kotlin / Jetpack Compose / Room / DataStore: free, open source
- Android Calendar API: free system API
- No backend server needed
- No third-party paid services
- Distribution: sideload APK via USB or QR code (no Google Play $25 fee)
