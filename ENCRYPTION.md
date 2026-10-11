# Database encryption

This app uses **SQLCipher** so Room data is encrypted at rest. There is no account and no network sync.

## What’s in place

| | |
| --- | --- |
| Algorithm | AES-256 (SQLCipher) |
| Library | [sqlcipher-android](https://github.com/sqlcipher/sqlcipher-android) **4.6.1** |
| Passphrase | 256-bit random value, Base64-encoded |
| Passphrase storage | `EncryptedSharedPreferences` (`secure_expense_prefs`) via `EncryptedPrefs` |
| Master key | Android Keystore, `MasterKey.KeyScheme.AES256_GCM` |
| Database file | `expense_database` (Room v7, WAL) |
| Survives app update | Yes |
| Survives uninstall / clear data | **No** — the Keystore-backed passphrase is gone |

Theme and biometric-lock flags live in a second encrypted prefs file (`encrypted_app_preferences`). Auto-backup **enablement** is stored in plain `SharedPreferences` (`auto_backup_state`) so the worker can check the toggle without opening SQLCipher.

## How it works

```
User data → Room DAOs → SQLCipher (AES-256) → encrypted files on disk
                              ↑
                    256-bit passphrase
                              ↑
              EncryptedSharedPreferences + Keystore MasterKey
```

1. First launch: `SecureKeyGenerator` creates a 256-bit passphrase with `SecureRandom` and stores it through `EncryptedPrefs`.
2. `ExpenseDatabase.getDatabase()` loads that passphrase, builds a SQLCipher `SupportOpenHelperFactory`, and opens Room as usual.
3. Later launches reuse the same passphrase (cached in memory after the first decrypt).

There are **no hardcoded keys**. A failed read of an existing prefs file is **not** treated as “no key” — that would mint a new passphrase and make the database unreadable.

## What is protected

- Expense rows (title, amount, category, description, dates)
- Monthly budgets and per-month budget exclusions
- User-managed categories
- Database indexes and SQLCipher temp/WAL files
- Theme and biometric-lock preferences

## What is not

- **Automatic / manual backup JSON** in `Download/Expense Tracker` (or wherever the user saved an export) — plaintext so it still works after uninstall
- App code (release builds **do** use R8 minify + shrink; see `app/r8-rules.pro`)
- Screenshots (the app does **not** set `FLAG_SECURE`)
- Optional biometric lock — a UI gate only; it does not wrap the database
- Rooted / compromised devices
- Android Auto Backup / device transfer: `android:allowBackup="true"` and the XML rules files are still the Studio stubs. Cloud backup of an encrypted DB without a restorable Keystore key is not a supported recovery path.

## Data recovery

| Event | Result |
| --- | --- |
| App uninstall | Passphrase deleted → local DB unrecoverable |
| Clear app data | Same |
| App update | Passphrase kept → DB still opens |
| Lost device | Data stays encrypted on that device |

To keep data across reinstalls, **export from Settings** or turn on **automatic backup**. Those files are unencrypted JSON (backup format v1–v3). Treat them as sensitive.

`SecureKeyGenerator.clearPassphrase` and `ExpenseDatabase.closeDatabase` **do not exist**. To reset encryption, clear app data or uninstall (this deletes expenses unless you imported from a JSON backup first).

## Code

| File | Role |
| --- | --- |
| `util/EncryptedPrefs.kt` | Shared `MasterKey` + `EncryptedSharedPreferences` cache |
| `util/SecureKeyGenerator.kt` | Generate / load SQLCipher passphrase |
| `data/ExpenseDatabase.kt` | SQLCipher `openHelperFactory`, migrations 1→7 |
| `data/PreferenceRepository.kt` | Encrypted theme / biometric prefs |
| `app/r8-rules.pro` | Keep SQLCipher, Room entities, Tink / security-crypto |

## Verify the DB is encrypted

`applicationId` and the Kotlin namespace are both `com.snagar.expensetracker`. An install that was previously `com.example.expancemanager` is a different app to Android, so its Keystore-backed passphrase does not carry over. Export a JSON backup from the old install before replacing it.

```bash
adb pull /data/data/com.snagar.expensetracker/databases/expense_database
sqlite3 expense_database
# Expect: file is not a database
```

Prefs values should also be unreadable:

```bash
adb pull /data/data/com.snagar.expensetracker/shared_prefs/secure_expense_prefs.xml
```

## Production notes already in the build

Release: `isMinifyEnabled = true`, `isShrinkResources = true`, `r8-rules.pro`.

Still optional (not implemented): `FLAG_SECURE`, root detection.

## Troubleshooting

**Database won’t open** — passphrase mismatch or corruption. Do not generate a new key over an existing file. Restore from JSON backup after a clean install, or clear app data if you accept losing local data.

**Slow queries** — treat like any Room DB: indexes (`date`, `category`, excluded-category name), transactions for bulk import (`TransactionRunner`).

**Keystore / EncryptedSharedPreferences failure on first launch** — there is no plaintext-prefs fallback. The error should surface rather than silently rotating the key.

---

**Last updated:** October 2026  
**SQLCipher:** 4.6.1  
**Room schema:** 7
