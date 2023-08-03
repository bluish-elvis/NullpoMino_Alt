git repack && git prune-packed && git reflog expire --expire=now --expire-unreachable=now --all && git gc --aggressive --prune=now
