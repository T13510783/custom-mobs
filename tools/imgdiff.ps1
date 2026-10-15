param([string]$fileA, [string]$fileB, [string]$outDir)
# Compare two renders saved by tools\cdp.ps1 (text files holding data:image/png;base64,...).
# Usage: imgdiff.ps1 <a.txt> <b.txt> [outDir]  -> pixel counts; outDir gets a.png, b.png, diff.png (red = differs).
Add-Type -AssemblyName System.Drawing
function Load($f) {
  $t = [IO.File]::ReadAllText($f)
  $m = [regex]::Match($t, 'data:image/png;base64,([A-Za-z0-9+/=]+)')
  if (-not $m.Success) { throw "no image in $f" }
  $bytes = [Convert]::FromBase64String($m.Groups[1].Value)
  $ms = [IO.MemoryStream]::new($bytes)
  return [System.Drawing.Bitmap]::new($ms)
}
$IA = Load $fileA; $IB = Load $fileB
"size A $($IA.Width)x$($IA.Height)  B $($IB.Width)x$($IB.Height)"
$w = [Math]::Min($IA.Width, $IB.Width); $h = [Math]::Min($IA.Height, $IB.Height)
$diff = New-Object System.Drawing.Bitmap($w, $h)
$n = 0; $big = 0; $sum = 0; $minX = $w; $minY = $h; $maxX = 0; $maxY = 0
for ($y = 0; $y -lt $h; $y++) {
  for ($x = 0; $x -lt $w; $x++) {
    $p = $IA.GetPixel($x, $y); $q = $IB.GetPixel($x, $y)
    $d = [Math]::Abs($p.R - $q.R) + [Math]::Abs($p.G - $q.G) + [Math]::Abs($p.B - $q.B)
    if ($d -gt 0) { $n++; $sum += $d }
    if ($d -gt 30) {
      $big++
      if ($x -lt $minX) { $minX = $x }; if ($y -lt $minY) { $minY = $y }
      if ($x -gt $maxX) { $maxX = $x }; if ($y -gt $maxY) { $maxY = $y }
      $diff.SetPixel($x, $y, [System.Drawing.Color]::Red)
    } else { $diff.SetPixel($x, $y, $p) }
  }
}
"pixels differing: $n   strongly (>30): $big   bbox of strong: $minX,$minY - $maxX,$maxY"
if ($outDir) {
  New-Item -ItemType Directory -Force $outDir | Out-Null # GDI+ Save fails on a missing folder
  $IA.Save((Join-Path $outDir 'a.png')); $IB.Save((Join-Path $outDir 'b.png')); $diff.Save((Join-Path $outDir 'diff.png'))
}
