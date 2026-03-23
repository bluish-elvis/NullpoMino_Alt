git repack && git prune-packed
git reflog expire --expire=now --expire-unreachable=now --all
git fsck --unreachable --full
git gc --aggressive --prune=now
git tag
