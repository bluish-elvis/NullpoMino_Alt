mkdir target nullpomino-core\target nullpomino-run\target
Set-Content -Path target -Stream com.dropbox.ignored -Value 1
Set-Content -Path nullpomino-core\target -Stream com.dropbox.ignored -Value 1
Set-Content -Path nullpomino-run\target -Stream com.dropbox.ignored -Value 1
