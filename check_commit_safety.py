#!/usr/bin/env python3
import subprocess
import sys

def main():
    print("Running commit safety check...")
    try:
        # Get status of files
        result = subprocess.run(
            ["git", "status", "--porcelain"],
            capture_output=True,
            text=True,
            check=True
        )
    except subprocess.CalledProcessError as e:
        print(f"Error running git status: {e}")
        sys.exit(1)

    lines = result.stdout.strip().split("\n")
    safety_violation = False

    for line in lines:
        if not line:
            continue
        # Format of git status --porcelain is 'XY path' or 'XY "path"'
        parts = line.strip().split(maxsplit=1)
        if len(parts) < 2:
            continue
        status_code = parts[0]
        filepath = parts[1].strip('"')

        # Check if file is in .eneik/
        if filepath.startswith(".eneik/") or "/.eneik/" in filepath:
            print(f"VIOLATION: File '{filepath}' is under a path starting with '.eneik/'. This is strictly forbidden!")
            safety_violation = True

        # Check if root .gitignore is modified
        if filepath == ".gitignore":
            print("VIOLATION: The root '.gitignore' has been modified. This is strictly forbidden!")
            safety_violation = True

    if safety_violation:
        print("Commit safety check FAILED! Please fix the violations before committing.")
        sys.exit(1)
    else:
        print("Commit safety check PASSED. No forbidden files are staged or modified.")
        sys.exit(0)

if __name__ == "__main__":
    main()
