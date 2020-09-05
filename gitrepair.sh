 git filter-branch -f --prune-empty --index-filter "git rm -r --cached --ignore-unmatch ./Icon$'\r'" HEAD
