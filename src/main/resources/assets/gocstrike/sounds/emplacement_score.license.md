# GOC Emplacement Score — licence and provenance

`goc-emplacement-score.wav` is licensed under the SCP Foundation wiki licence,
**Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)**:
<https://creativecommons.org/licenses/by-sa/3.0/>

## Attribution

- Original work: 《风雪史诗》, an original GOC track
- Author: 西蒙Leonardo
- Source: <https://www.bilibili.com/video/BV18ipGzdE4h/>
  (【正式版/5K GOC】"风与雪，见证我们的史诗！"原创GOC曲目《风雪史诗》)

CC BY-SA 3.0 is a copyleft licence. This cut is a derivative work, so it and any
further derivative are also CC BY-SA 3.0, and every redistribution must keep this
attribution and licence notice.

## Provenance

The adjacent `goc-emplacement-score.wav` is a 118.698667-second cut (source range
0.000-118.698667 s) of the local recording `风雪史诗-改.wav` (201.599208 s, 48 kHz
stereo pcm_f32le, sha256
6bc1dae53b566a578ff791751a4837b9e6e89ffa471b307965d3432912496fc2), itself an edit
of the original track above.

The cut starts at source zero, so score time equals source time. The detonation
cue is `GocEmplacementMotion.Impact` = 73.365333 s (the measured global
positive-spectral-flux maximum of the recording). The terminal whiteout is
`GocEmplacementMotion.WhiteoutAt` = 116.032 s, where the one-bar linear fade-out
(2.666667 s) begins; it ends on `GocEmplacementMotion.Duration` = 118.698667 s.
Retiming any of those constants requires re-running
`tools/build_goc_emplacement_score.py`, which regenerates this cut and
`assets/goc-strike/emplacement-score.json` together.

Project records: `assets/goc-strike/emplacement-score.json`,
`tools/build_goc_emplacement_score.py`, `tools/analysis/STRIKE-SPEC.md`.
