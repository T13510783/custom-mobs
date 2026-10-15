param([string]$txtFile, [string]$outFile)
$content = (Get-Content $txtFile -Raw).Trim()
$b64 = $content -replace '^data:image/png;base64,',''
[System.IO.File]::WriteAllBytes($outFile, [System.Convert]::FromBase64String($b64))
Write-Output "Wrote $outFile"
