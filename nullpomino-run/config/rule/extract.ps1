#Get-ChildItem *.rul| Rename-Item -NewName { $_.Name -replace '\.rul','.rul.gz' }
gzip -d *.rul
