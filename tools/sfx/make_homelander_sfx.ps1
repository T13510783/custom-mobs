# Makes Homelander's sound effects: tools/sfx/homelander_sfx.py writes .wav files to %TEMP%,
# then ffmpeg turns them into sounds/homelander/<name>.ogg (mono Vorbis: mod + browser) and .mp3 (browser backup).
# Usage: tools\sfx\make_homelander_sfx.ps1   (needs python + ffmpeg; tools\export_mod_assets.ps1 -mob homelander copies them into the mod)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$tmp = Join-Path $env:TEMP 'homelander_sfx'
New-Item -ItemType Directory -Force $tmp | Out-Null
& python (Join-Path $PSScriptRoot 'homelander_sfx.py') $tmp
$ff = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ff) { $ff = (Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe | Select-Object -First 1).FullName }
$out = Join-Path $root 'sounds\homelander'
foreach ($n in 'punch', 'grab', 'whoosh', 'slap', 'laser') {
  & $ff -v error -y -i "$tmp\$n.wav" -ac 1 -c:a libvorbis -q:a 4 "$out\$n.ogg"
  & $ff -v error -y -i "$tmp\$n.wav" -ac 1 -c:a libmp3lame -b:a 96k "$out\$n.mp3"
}
Get-ChildItem $out | Select-Object Name, Length | Format-Table -AutoSize
