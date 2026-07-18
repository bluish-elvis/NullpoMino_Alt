"build","out","target" | %{$d=$_; "","nullpomino-core\", "nullpomino-run\" | %{ "$_$d"}} | %{
  mkdir $_ -Force | Out-Null
  Set-Content -Path $_ -Stream com.dropbox.ignored -Value 1
}

".git", ".gradle", ".idea\workspace.xml"|%{Set-Content -Path $_ -Stream com.dropbox.ignored -Value 1}
