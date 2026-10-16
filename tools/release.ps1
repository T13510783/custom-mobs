param([switch]$Bump, [switch]$DryRun)
# ───────── Publish a mod release on GitHub ─────────
# Version = mod/gradle.properties mod_version (X.Y.Z-alpha, every part 0..99). Changelog = mod/release_notes.md
# with exactly the sections "## Added", "## Removed", "## Fixed" (in that order, empty = "- Nothing").
#   tools\release.ps1 -Bump     count the version up first (0.0.9 -> 0.0.10, 0.0.99 -> 0.1.0, 0.99.99 -> 1.0.0; 99.99.99 = last)
#   tools\release.ps1 -DryRun   check + build only, publish nothing
# Steps: check version + notes -> gradlew build (tests run) -> tag vX.Y.Z-alpha on the pushed HEAD commit
# -> GitHub release with custom-mobs-X.Y.Z-alpha.jar. GitHub keeps every release; locally builds/ keeps the last 5 jars.
# Needs: gh (logged in), the current commit pushed. Release from main after the PR is merged.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$propsFile = Join-Path $root 'mod\gradle.properties'
$notesFile = Join-Path $root 'mod\release_notes.md'

# ───────── Version ─────────
$props = [IO.File]::ReadAllText($propsFile)
if ($props -notmatch '(?m)^mod_version=(\d{1,2})\.(\d{1,2})\.(\d{1,2})-alpha\s*$') { throw "mod_version in mod/gradle.properties is not X.Y.Z-alpha" }
$v = @([int]$Matches[1], [int]$Matches[2], [int]$Matches[3])
if ($Bump) {
  if ($v[2] -lt 99) { $v[2]++ }
  elseif ($v[1] -lt 99) { $v[1]++; $v[2] = 0 }
  elseif ($v[0] -lt 99) { $v[0]++; $v[1] = 0; $v[2] = 0 }
  else { throw 'v99.99.99-alpha is the last version' }
}
$ver = '{0}.{1}.{2}-alpha' -f $v[0], $v[1], $v[2]
$tag = "v$ver"
if ($Bump -and -not $DryRun) {
  $props = [regex]::Replace($props, '(?m)^mod_version=.*$', "mod_version=$ver")
  [IO.File]::WriteAllText($propsFile, $props, (New-Object Text.UTF8Encoding $false))
  Write-Host "Version -> $ver (commit + push mod/gradle.properties before releasing)"
}

# ───────── Changelog: only Added / Removed / Fixed ─────────
$notes = [IO.File]::ReadAllText($notesFile) -replace "`r", ''
$heads = [regex]::Matches($notes, '(?m)^#+\s*(.+?)\s*$') | ForEach-Object { $_.Groups[1].Value }
if (($heads -join ',') -ne 'Added,Removed,Fixed') { throw "release_notes.md needs exactly the sections Added, Removed, Fixed (found: $($heads -join ', '))" }
$body = $notes.Trim() + "`n"

# ───────── Already released? ─────────
$null = & gh release view $tag 2>$null
if ($LASTEXITCODE -eq 0) { throw "$tag is already on GitHub: run with -Bump for the next version" }

# ───────── Build (runs the tests) ─────────
& (Join-Path $root 'mod\gradlew.bat') -p (Join-Path $root 'mod') build --console=plain | Select-Object -Last 5
if ($LASTEXITCODE -ne 0) { throw 'gradlew build failed' }
$jar = Join-Path $root "mod\build\libs\custom-mobs-$ver.jar"
if (-not (Test-Path $jar)) { throw "no $jar (did the version change without a rebuild?)" }

# ───────── Publish ─────────
$sha = (& git -C $root rev-parse HEAD).Trim()
$pushed = & git -C $root branch -r --contains $sha
if (-not $pushed) { throw "commit $sha is not pushed yet: push first" }
$tmp = Join-Path $env:TEMP "custom-mobs-notes-$ver.md"
[IO.File]::WriteAllText($tmp, $body, (New-Object Text.UTF8Encoding $false))
if ($DryRun) { Write-Host "Dry run: would publish $tag ($([math]::Round((Get-Item $jar).Length / 1KB)) KB) on $sha"; Get-Content $tmp; exit 0 }
& gh release create $tag $jar --title $tag --notes-file $tmp --target $sha
if ($LASTEXITCODE -ne 0) { throw 'gh release create failed' }
Remove-Item $tmp -Confirm:$false
Write-Host "Released $tag"
