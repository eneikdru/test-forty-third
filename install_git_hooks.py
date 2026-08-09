#!/usr/bin/env python3
import os
import stat

def main():
    hook_dir = ".git/hooks"
    if not os.path.exists(hook_dir):
        print(f"Directory {hook_dir} does not exist. Skipping git hook installation.")
        return

    pre_commit_src = "check_commit_safety.py"
    pre_commit_dest = os.path.join(hook_dir, "pre-commit")

    # Write the pre-commit hook file
    with open(pre_commit_dest, "w") as f:
        f.write("#!/bin/sh\n")
        f.write("python3 check_commit_safety.py\n")

    # Set executable permissions
    for path in [pre_commit_dest, pre_commit_src]:
        if os.path.exists(path):
            st = os.stat(path)
            os.chmod(path, st.st_mode | stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH)

    print("Git pre-commit hook successfully installed/updated.")

if __name__ == "__main__":
    main()
