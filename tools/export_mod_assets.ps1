param([ValidateSet('the_king', 'homelander')][string]$mob = 'the_king', [string]$scratch = '')
# Copy a mob from its HTML viewer into the mod, 1:1 (run after every change to the mob's viewer page):
#   rig.json + anims.bin  (model quads, bones, springs, voice curves, all animations baked at 120 fps)
#   textures/entity/<prefix>_<variant>.png
#   sounds/*.ogg          (mono: Minecraft only plays mono sounds positionally)
#   src/test/resources/<trace>.json (the viewer's reference recording for the 1:1 test)
# Usage: tools\export_mod_assets.ps1 [-mob the_king|homelander]   (needs Brave + ffmpeg, see CLAUDE.md)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

# ───────── Per mob: viewer page, mod namespace + folders, trace file, sounds (source -> mod name) ─────────
$cfg = @{
  the_king = @{ page = 'mobs/the_king/thekingv3.html'; ns = 'theking'; dir = 'king'; tex = 'the_king'; trace = 'modtrace.json'; snd = 'sounds\the_king'
    sounds = @{ 'cussingshort.ogg' = 'voice_cussing_short.ogg'; 'cussing_long_final.ogg' = 'voice_cussing_long.ogg';
                'take_ur_stuff_and_get_out_of_my_seat_please_final.ogg' = 'voice_take_your_stuff.ogg'; 'backgroundmusic.ogg' = 'dance_music.ogg' } }
  homelander = @{ page = 'mobs/homelander/homelander.html'; ns = 'homelander'; dir = 'homelander'; tex = 'homelander'; trace = 'homelander_modtrace.json'; snd = 'sounds\homelander'
    sounds = @{ 'i_am_god.ogg' = 'voice_i_am_god.ogg' } }
}[$mob]
if (-not $scratch) { $scratch = Join-Path $env:TEMP "$($mob)_export" }
$page = 'file:///' + ($root -replace '\\', '/' -replace ' ', '%20') + '/' + $cfg.page
$res = Join-Path $root "mod\src\main\resources\assets\$($cfg.ns)"
New-Item -ItemType Directory -Force $scratch, "$res\$($cfg.dir)", "$res\textures\entity", "$res\sounds", (Join-Path $root 'mod\src\test\resources') | Out-Null

# 1. Model + animations + textures from the page's #modexport mode.
& "$PSScriptRoot\cdp.ps1" -url "$page#modexport" -expr "(document.getElementById('modexport')||{}).textContent||''" -out "$scratch\modexport.json" -timeoutSec 120 | Out-Null
$raw = [IO.File]::ReadAllText("$scratch\modexport.json")
$j = $raw | ConvertFrom-Json
# rig.json = the "rig" object exactly as the page wrote it (cut from the raw text: no PowerShell re-formatting of numbers).
$start = $raw.IndexOf('{', $raw.IndexOf('"rig"')); $depth = 0; $end = $start
for ($i = $start; $i -lt $raw.Length; $i++) { $ch = $raw[$i]; if ($ch -eq '{') { $depth++ } elseif ($ch -eq '}') { $depth--; if ($depth -eq 0) { $end = $i; break } } }
[IO.File]::WriteAllText("$res\$($cfg.dir)\rig.json", $raw.Substring($start, $end - $start + 1), (New-Object Text.UTF8Encoding $false))
[IO.File]::WriteAllBytes("$res\$($cfg.dir)\anims.bin", [Convert]::FromBase64String($j.bin))
foreach ($p in $j.textures.PSObject.Properties) {
  $b64 = $p.Value.Substring($p.Value.IndexOf(',') + 1)
  [IO.File]::WriteAllBytes("$res\textures\entity\$($cfg.tex)_$($p.Name).png", [Convert]::FromBase64String($b64))
}

# 2. Reference recording for the 1:1 test.
& "$PSScriptRoot\cdp.ps1" -url "$page#modtrace" -expr "(document.getElementById('modtrace')||{}).textContent||''" -out (Join-Path $root "mod\src\test\resources\$($cfg.trace)") -timeoutSec 300 | Out-Null

# 3. Sounds: mono Ogg Vorbis.
$ff = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ff) { $ff = (Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe | Select-Object -First 1).FullName }
$snd = Join-Path $root $cfg.snd
foreach ($k in $cfg.sounds.Keys) { & $ff -v error -y -i (Join-Path $snd $k) -ac 1 -c:a libvorbis -q:a 4 (Join-Path "$res\sounds" $cfg.sounds[$k]) }

Get-ChildItem "$res\$($cfg.dir)", "$res\textures\entity", "$res\sounds" | Select-Object Name, Length | Format-Table -AutoSize
