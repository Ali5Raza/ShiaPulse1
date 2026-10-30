#!/usr/bin/env python3
"""Fail fast on structural Quran bundle and deterministic mapping regressions."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
QURAN = ROOT / "app/src/main/assets/Quran"
EXPECTED_COUNTS = [
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128,
    111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73,
    54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49,
    62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28,
    28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
    15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
]
JUZ_START_REFS = [
    "1:1", "2:142", "2:253", "3:93", "4:24", "4:148", "5:82", "6:111",
    "7:88", "8:41", "9:93", "11:6", "12:53", "15:1", "17:1", "18:75", "21:1",
    "23:1", "25:21", "27:56", "29:46", "33:31", "36:28", "39:32", "41:47",
    "46:1", "51:31", "58:1", "67:1", "78:1",
]
JUZ_START_IDS = [
    1, 149, 260, 386, 517, 641, 751, 900, 1042, 1201, 1328, 1479, 1649, 1803,
    2030, 2215, 2484, 2674, 2876, 3215, 3386, 3564, 3733, 4090, 4265, 4511,
    4706, 5105, 5242, 5673,
]
JUZ_TOTALS = [
    148, 111, 126, 131, 124, 110, 149, 142, 159, 127, 151, 170, 154, 227, 185,
    269, 190, 202, 339, 171, 178, 169, 357, 175, 246, 195, 399, 137, 431, 564,
]


def main() -> int:
    errors: list[str] = []
    index_path = QURAN / "quran_index.json"
    try:
        index = json.loads(index_path.read_text(encoding="utf-8"))["surahs"]
    except Exception as exc:  # noqa: BLE001 - CI should report any malformed bundle.
        print(f"Quran index could not be loaded: {exc}", file=sys.stderr)
        return 1

    if [item.get("number") for item in index] != list(range(1, 115)):
        errors.append("Surah IDs are not exactly 1–114")
    if [item.get("verseCount") for item in index] != EXPECTED_COUNTS:
        errors.append("Surah ayah counts differ from the canonical counts")
    if sum(EXPECTED_COUNTS) != 6236:
        errors.append("Internal expected-count constant is invalid")

    expected_files = {f"surah_{number:03}.json" for number in range(1, 115)}
    actual_files = {path.name for path in QURAN.glob("surah_*.json")}
    for missing in sorted(expected_files - actual_files):
        errors.append(f"Missing Surah asset: {missing}")
    for orphan in sorted(actual_files - expected_files):
        errors.append(f"Orphan Surah asset: {orphan}")

    references: set[str] = set()
    audio_urls: set[str] = set()
    global_ids: dict[str, int] = {}
    next_global_id = 1
    for info in index:
        surah_number = info.get("number")
        expected_count = info.get("verseCount")
        path = QURAN / f"surah_{surah_number:03}.json"
        try:
            root = json.loads(path.read_text(encoding="utf-8"))
            verses = root["verses"]
        except Exception as exc:  # noqa: BLE001
            errors.append(f"Unreadable Surah {surah_number}: {exc}")
            continue
        if root.get("number") != surah_number:
            errors.append(f"Wrong Surah ID in {path.name}")
        if len(verses) != expected_count:
            errors.append(f"Wrong ayah count in Surah {surah_number}")
        seen_in_surah: set[int] = set()
        for position, ayah in enumerate(verses, start=1):
            number = ayah.get("number")
            reference = f"{surah_number}:{number}"
            if number != position:
                errors.append(f"Non-sequential Arabic/translation ayah ID at {reference}")
            if number in seen_in_surah:
                errors.append(f"Duplicate ayah ID at {reference}")
            seen_in_surah.add(number)
            if reference in references:
                errors.append(f"Duplicate global ayah record at {reference}")
            references.add(reference)
            global_ids[reference] = next_global_id
            next_global_id += 1
            for field, label in (("arabic", "Arabic"), ("english", "Qarai"), ("urdu", "Jawadi")):
                if not isinstance(ayah.get(field), str) or not ayah[field].strip():
                    errors.append(f"Missing {label} text at {reference}")
            audio = (
                "https://verses.quran.foundation/Alafasy/mp3/"
                f"{surah_number:03}{number:03}.mp3"
            )
            if audio in audio_urls:
                errors.append(f"Duplicate audio mapping at {reference}")
            audio_urls.add(audio)

    if len(references) != 6236:
        errors.append(f"Expected 6,236 unique ayahs, found {len(references)}")
    if len(audio_urls) != 6236:
        errors.append(f"Expected 6,236 unique ayah audio mappings, found {len(audio_urls)}")

    actual_juz_starts = [global_ids.get(reference) for reference in JUZ_START_REFS]
    if actual_juz_starts != JUZ_START_IDS:
        errors.append(f"Juz boundaries differ from verified Quran Foundation IDs: {actual_juz_starts}")
    actual_juz_totals = [
        (JUZ_START_IDS[position + 1] if position + 1 < 30 else 6237) - start
        for position, start in enumerate(JUZ_START_IDS)
    ]
    if actual_juz_totals != JUZ_TOTALS or sum(JUZ_TOTALS) != 6236:
        errors.append("Juz ranges do not partition all 6,236 ayahs with verified totals")

    try:
        manifest = json.loads((QURAN / "source_manifest.json").read_text(encoding="utf-8"))
        if manifest.get("surahCount") != 114 or manifest.get("verseCount") != 6236:
            errors.append("Source manifest counts are inconsistent")
        for translation in manifest.get("translations", []):
            if translation.get("crossSourceMismatches", 0) != 0:
                errors.append(f"Source manifest reports mismatches for {translation.get('id')}")
    except Exception as exc:  # noqa: BLE001
        errors.append(f"Source manifest could not be validated: {exc}")

    if errors:
        print("Quran data integrity validation FAILED:", file=sys.stderr)
        for error in errors[:100]:
            print(f"- {error}", file=sys.stderr)
        if len(errors) > 100:
            print(f"- … {len(errors) - 100} more errors", file=sys.stderr)
        return 1

    print(
        "Quran data integrity validation passed: "
        "114 Surahs, 6,236 sequential ayahs, Arabic/Qarai/Jawadi IDs and text, "
        "6,236 unique audio mappings, and 30 verified Juz ranges."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
