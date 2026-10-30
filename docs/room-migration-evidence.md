# Room migration evidence inventory

## Scope and evidence limits

This inventory covers `PrayerDatabase` / `shia_prayer_database` at Room version 14. It is based on the complete 115-commit mainline history, historical Kotlin entity definitions, and archived debug APKs. It does **not** claim that every mainline version was distributed to users.

Evidence limitations:

- GitHub has no repository releases or tags mapping application releases to Room versions.
- The application has remained `versionCode = 1`, `versionName = "1.0"`; those values cannot identify Room releases.
- Historical `exportSchema` was always `false`; no historical Room JSON schemas or migrations were committed.
- Archived `.build-outputs/app-debug.apk` files are debug build artifacts, not proof of a shipped release.
- No actual user database was inspected. Only the versions below have source-level schema evidence.

## Recovered versions

| Room version | First mainline evidence | Recovered schema/change evidence |
| --- | --- | --- |
| 3 | `86fbcab8e519997adc5581f5b3455b4dbe1f7384` | First Room database declaration. Five tables: prayer logs, qada tallies, favorite quotes, Ramadan records, tasbeeh daily. The archived APK in this commit contains matching generated `CREATE TABLE` SQL. |
| 4 | `fc9a192ff26e7f5595bb24b7b49fad54b58bd054` | Adds `ziyarats`. Historical source and archived APK generated SQL agree. |
| 6 | `1a1d53e1b0490601e8033b724bdef562747a01b1` | Adds `ziyarat_visits` and `ziyarat_journals`. Archived v6 APK SQL also confirms columns and primary keys. |
| 8 | `d427baf561ef0631c70345e63bc037507e7d12bd` | Version-only change: the entity diff from v6 adds only a source comment. No table/column change is evidenced. |
| 10 | `7149427a71026013ccf7d7d66ce219ffa44b7c04` | Adds `aamal_custom_activities`, `aamal_completions`, and `hajj_umrah_checklist`. |
| 11 | `31798d376765e828959dc193164a4cf0fc3fe054` | Adds non-null `aamal_completions.timestamp`. The commit message explicitly identifies this entity change and the v11 bump. |
| 13 | `10a5d92c6e61940db8e100728286c6090b4f9647` | Adds `my_collections` and `dua_reading_logs`. |
| 14 | `9ba6eb737fc13aed77b28dbf9178f05e6754d19c` | Adds `finance_transactions`. The later Kotlin default for `dateStringHijri` does not change its Room column schema. |

The supported migration chain is therefore:

`3 → 4 → 6 → 8 → 10 → 11 → 13 → 14`

## Missing versions: 1, 2, 5, 7, 9, 12

- The repository's initial commit has no Room database; the first recovered Room declaration is already v3.
- No mainline `@Database` declaration uses v1, v2, v5, v7, v9, or v12.
- The version jumps 4→6, 6→8, 8→10, and 11→13 explain why those numbers do not appear in the recovered chain, but do not prove that no private/uncommitted build ever used them.
- There is no schema evidence from which to construct safe migrations for these versions. No migration or historical schema JSON is fabricated for them.
- With destructive fallback removed, an unsupported version fails closed instead of silently deleting its tables. Recovering a real database/schema is required before supporting one of these versions.

## 10 → 11 timestamp policy

Version 10 has no completion-time column. Version 11 adds a non-null epoch-millisecond value used by automatic Aamal reminder-hour learning. Existing rows contain a completion date but no evidence of their completion time.

The migration adds:

```sql
ALTER TABLE `aamal_completions`
ADD COLUMN `timestamp` INTEGER NOT NULL DEFAULT 0
```

Policy: **0 means the historical time is unknown**. The migration clock is deliberately not used, because assigning upgrade time as completion time would invent user activity and bias reminder learning. Rows and their completion state remain intact.

Known limitation: the current automatic reminder learner consumes timestamps without an unknown-value filter. Because Aamal code is frozen for this Room task, the effect of migrated zero timestamps on automatic reminder-hour learning remains an explicit runtime/product risk, not something this migration claims to solve.

## Destructive fallback decision

`fallbackToDestructiveMigration()` existed from the first recovered Room database through v14, while no migrations were registered. Consequently, an upgrade without a matching path could erase all persisted tables.

The evidenced chain now preserves existing tables and adds only the recovered changes. Destructive fallback is removed so that:

- every evidenced version has a non-destructive path to v14;
- an unknown version cannot trigger silent data deletion;
- schema mismatches surface as failures requiring investigation rather than being hidden by a wipe.

## Export and migration-test policy

- Current v14 uses `exportSchema = true` with KSP `room.schemaLocation` pointing to `app/schemas`.
- Historical Room JSON is not backfilled by hand; identity hashes and compiler-generated metadata must not be invented.
- `PrayerDatabaseMigrationTest` creates each evidenced old schema from historical SQL/entity evidence, seeds every existing table, then opens the real current `PrayerDatabase` with the production migrations.
- Opening v14 forces Room's own final schema validation. Tests cover all seven supported start versions, row preservation, the v10 unknown-timestamp backfill, and preservation of known v11+ timestamps.
- Compiler-generated v14 schema JSON and test execution results must come from an actual Android/Gradle build; source inspection alone is not migration certification.
