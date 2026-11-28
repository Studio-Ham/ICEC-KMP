"""스토어 글자 수 제한 확인: python3 fastlane/check_lengths.py (초과 시 exit 1)"""
import pathlib, sys
LIMITS = {"android": {"title": 30, "short_description": 80, "full_description": 4000},
          "ios": {"name": 30, "subtitle": 30, "promotional_text": 170, "keywords": 100, "description": 4000}}
root, bad = pathlib.Path(__file__).parent / "metadata", False
print("| 스토어 | 언어 | 항목 | 글자 수 | 제한 | 판정 |\n| --- | --- | --- | --- | --- | --- |")
for store, limits in LIMITS.items():
    for loc in sorted(p for p in (root / store).iterdir() if p.is_dir()):
        for key, lim in limits.items():
            n = len((loc / f"{key}.txt").read_text().rstrip("\n"))
            bad |= n > lim
            print(f"| {store} | {loc.name} | {key} | {n} | {lim} | {'통과' if n <= lim else '초과'} |")
sys.exit(bad)
