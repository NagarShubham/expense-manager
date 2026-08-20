# Expense

**Private, offline expense tracking for Android** — monthly budgets, custom categories, and spending insights. Data never leaves the device. Amounts are in Indian Rupees (₹).

Built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Hilt**, **Room**, and **SQLCipher**.

[Features](#features) · [Architecture](#architecture) · [Screens](#screens) · [Data](#data-model) · [Build](#build--run) · [Security](#security) · [Docs](#further-reading)

---

## Why this app

| | |
| --- | --- |
| **Offline-first** | No account, no network, no analytics SDK. Room is the source of truth. |
| **Encrypted at rest** | SQLCipher AES-256; passphrase in Android Keystore + EncryptedSharedPreferences. |
| **INR-native** | Indian grouping (lakh/crore) and the ₹ symbol throughout. |
| **Yours to shape** | Rename, reorder, and add categories; exclude some from the monthly budget. |

Uninstalling the app **destroys the encryption key**. Export a backup from Settings before you wipe the install.

---

## Features

### Track
- Browse expenses **by month** (previous / next, jump to today).
- Add, edit, delete (swipe-to-delete with confirmation).
- Title, amount, category, date, optional description.

### Budget
- Set an **expected monthly amount** (Settings → Monthly budget).
- Home shows used vs remaining or overspent, plus progress.
- **Exclude categories** from budget math; they still appear in lists and breakdowns.

### Categories
- **17 defaults** (emoji + English names), fully editable.
- Home: **top 5** for the month, then “view all”.
- Drill into one category for that month.

### Insights
Open **Settings → Insights → Spending insights**.

| Period | Meaning |
| --- | --- |
| Last 6 months | Rolling six calendar months including the current one |
| This year | Calendar year-to-date (January through the current month) |
| Custom | Material 3 date-range picker (exact start/end days) |

Metrics: **total**, **monthly average** (divides by every month in the range, including ₹0 months), **highest month**, **lowest month**. The **current calendar month is omitted from lowest** so a partial month is not treated as cheapest.

### Settings
- Dark / light theme  
- Optional **biometric or device PIN** lock on launch  
- JSON **export / import** (Storage Access Framework — no storage permission)  
- Import **merge** or **replace**; backups v1–v3 (expenses → +budgets/exclusions → +categories)

---

## Architecture

MVVM with a single-activity Compose UI. Repositories expose **Kotlin Flow**; ViewModels combine them into `StateFlow` UI state. Hilt wires database, backup, and preferences.

```mermaid
flowchart LR
  UI["Compose screens"] --> VM["ViewModels + StateFlow"]
  VM --> Repo["Repositories"]
  Repo --> Room["Room DAOs"]
  Room --> SQL["SQLCipher AES-256"]
  SQL --> KS["Keystore + Encrypted prefs"]
  VM --> Backup["BackupManager JSON"]
  Backup --> SAF["SAF file picker"]
```

```
app/src/main/java/com/example/expancemanager/
├── data/          entities, DAOs, repositories, preferences
├── di/            Hilt modules
├── nav/           Navigation 3 routes
├── ui/screen/     Home, expenses, reports, settings, budget, categories, lock
├── ui/components/ shared Compose widgets
├── ui/theme/      Material 3 theme
├── viewmodel/
├── util/          dates, insights, backup, biometrics, keys
├── ExpenseManagerApplication.kt
└── MainActivity.kt
```

---

## Screens

| Screen | What you do |
| --- | --- |
| Home | Month nav, budget/total, categories, recent expenses, add FAB |
| Add / Edit | Form with category and date |
| Detail | Full record, edit, delete |
| All categories | Share of spend for the month |
| Category expenses | Transactions in one category |
| Spending insights | Period chips, optional calendar range, summary + categories |
| Settings | Theme, lock, budget, insights, manage categories, backup |
| Monthly budget | Amount, clear, exclusions |
| Manage categories | Add, edit, reorder, delete |
| Biometric lock | Shown when lock is enabled |

---

## Data model

Local Room database (version 7), encrypted. Indexes on `expenses.date` and `expenses.category`.

| Table | Role |
| --- | --- |
| `expenses` | `id`, `title`, `amount`, `category`, `description`, `date`, `createdAt` |
| `monthly_budgets` | Expected amount per month + year |
| `budget_excluded_categories` | Names omitted from budget used / remaining |
| `categories` | User name, emoji, sort order |

**Default categories:** Bills & Utilities 💡 · Transportation 🚗 · Food & Dining 🍔 · Personal Care 💆 · EMI 💳 · Baby 👶 · Groceries 🛒 · Investments 📈 · Travel ✈️ · Shopping 🛍️ · Entertainment 🎬 · Healthcare 🏥 · Education 📚 · Rent 🏠 · Insurance 🛡️ · Gifts 🎁 · Other 💰

Backup JSON is versioned:

1. Expenses only  
2. + monthly budgets and exclusions  
3. + user categories  

Older files still import; missing lists are treated as empty.

---

## Stack

| | Version |
| --- | --- |
| Kotlin | 2.2.0 |
| AGP | 8.13.1 |
| Compose BOM | 2026.03.00 |
| Room | 2.8.4 |
| SQLCipher | 4.6.1 |
| Navigation 3 | 1.1.5 |
| Hilt | 2.57.1 |
| Gson | 2.11.0 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| JDK | 11 |

Also: KSP, ktlint, R8 (release), ABI splits (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`), 16 KB page-size alignment for Android 15+.

---

## Build & run

**Need:** Android Studio (current stable), JDK 11+, Android SDK 36.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Open the project in Android Studio, sync Gradle, and run on a device or emulator (API 26+).

---

## Security

1. On first launch, a random 256-bit passphrase is generated.  
2. It is stored in EncryptedSharedPreferences, wrapped by the Android Keystore (hardware-backed when the device allows).  
3. SQLCipher encrypts the database file; Room usage is unchanged.

There are **no hardcoded keys**. App updates keep the key; **uninstall does not**.

Optional **biometric lock** is a UI gate; it does not replace database encryption.

Full write-up: [ENCRYPTION.md](ENCRYPTION.md).

---

## Further reading

- [ENCRYPTION.md](ENCRYPTION.md) — key lifecycle, SQLCipher, uninstall implications  
- [16KB_PAGE_SIZE.md](16KB_PAGE_SIZE.md) — Play 16 KB page-size requirements and ABI splits  

---

## Roadmap

Not in the app yet:

- Recurring expenses  
- Search, tags, charts  
- CSV / PDF export  
- Multiple currencies  
- Receipt photos  
- Encrypted or scheduled backups  
- Cloud sync  

---

Personal project. Kotlin + Jetpack Compose.
