# Release-minified capture/export/import matrix

Date: 2026-11-11

Each row used the minified, debug-signed release sample: install → enable SDK →
open reporter → capture → image editor → exact review → approve reviewed image →
SAF export → pull → desktop `validate`/`inspect`/`render`.

| API | Device | Image | Desktop result |
|---|---|---|---|
| 26 | `rp-api26` emulator | 900×1600 | valid, render PASS |
| 30 | `rp-api30` emulator | 900×1600 | valid, render PASS |
| 31 | physical low/mid-range device | 702×1600 | valid, render PASS |
| 37 | `Android_17` emulator | 720×1600 | valid, render PASS |

Representative report IDs:

```text
API 26  19e4b092-54a3-4396-b791-bd2535550777
API 30  df17cb41-f2df-4154-81d3-b6a67bf03a2f
API 31  0ac35eed-c53a-4307-b29f-d67523222edd
API 37  1913c9dc-4d72-4349-a1cb-bbb477fc89fb
```

All inspected parcels reported `variant: release`, an image entry and
`state: collected`. The exported images were downscaled to a 1600 px longest
edge. Synthetic report files were removed from the devices after validation.

This covers the declared API 26/30/current support matrix and the physical
low/mid-range device for the release capture/mask/export/import flow.
