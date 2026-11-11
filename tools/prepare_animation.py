"""Prepare a Unity RunCommand from the pinned original choreography (stdlib only).

Run the resulting C# command in Unity, then use --compress to package its output.
The original repository is read with git show; its working tree is never changed.
"""

import argparse
import gzip
import json
from pathlib import Path
import struct
import subprocess

COMMIT = "7f9a7c12f5c6576d129b6c5d7bae5a6336e66a2d"
ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="Original goc-strike-demo repository")
    parser.add_argument("--temp", type=Path, help="Temporary directory for the Unity bake input and command")
    parser.add_argument("--compress", action="store_true")
    args = parser.parse_args()
    if args.compress:
        raw = ROOT / "src/main/resources/assets/gocstrike/animation/emplacement.bin"
        raw.with_suffix(".bin.gz").write_bytes(gzip.compress(raw.read_bytes(), mtime=0))
        raw.unlink()
        return
    if not args.source or not args.temp:
        parser.error("--source and --temp are required when preparing a bake")
    args.temp.mkdir(parents=True, exist_ok=True)
    reference = ROOT / "tools/reference"
    for original, target in [
        ("src/GocEmplacementMotion.cs", "GocEmplacementMotion.cs"),
        ("generated/models/goc-emplacement.mer.json", "goc-emplacement.mer.json"),
    ]:
        data = subprocess.check_output(["git", "-C", str(args.source), "show", f"{COMMIT}:{original}"])
        (reference / target).write_bytes(data)
    blocks = json.loads((reference / "goc-emplacement.mer.json").read_text(encoding="utf-8-sig"))["Blocks"]
    bake_input = args.temp / "bake-input.bin"
    with bake_input.open("wb") as output:
        output.write(struct.pack("<i", len(blocks)))
        for block in blocks:
            name = block["Name"].encode("utf-8")
            output.write(struct.pack("<i", len(name)))
            output.write(name)
            output.write(struct.pack("<iii", block["ObjectId"], block["ParentId"], block["BlockType"]))
            for field in ["Position", "Rotation", "Scale"]:
                output.write(struct.pack("<3f", *(block[field][axis] for axis in "xyz")))
            properties = block["Properties"]
            output.write(struct.pack("<i", properties.get("PrimitiveType", -1)))
            color = properties.get("ColorRgba", dict(r=1, g=1, b=1, a=1))
            output.write(struct.pack("<4f", *(color[channel] for channel in "rgba")))
    motion = (reference / "GocEmplacementMotion.cs").read_text(encoding="utf-8-sig")
    # Unity RunCommand requires all helper types in its dynamic editor namespace.
    motion = motion.replace("namespace GocStrikeDemo", "namespace Unity.AI.Assistant.Agent.Dynamic.Extension.Editor")
    driver = (ROOT / "tools/BakeEmplacement.cs").read_text(encoding="utf-8")
    driver = driver.replace("GocStrikeDemo.", "")
    driver = driver.replace("__PROJECT_ROOT__", str(ROOT)).replace("__BAKE_INPUT__", str(bake_input.resolve()))
    command = motion + "\nnamespace Unity.AI.Assistant.Agent.Dynamic.Extension.Editor {\n" + driver + "\n}\n"
    (args.temp / "bake-command.cs").write_text(command, encoding="utf-8")
    print(f"Prepared {len(blocks)} nodes from {COMMIT}")
    print(args.temp / "bake-command.cs")


if __name__ == "__main__":
    main()
