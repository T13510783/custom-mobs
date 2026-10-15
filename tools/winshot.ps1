param([string]$title = 'Minecraft*', [string]$out, [int]$width = 640, [int]$quality = 60)
# Screenshot of one window (e.g. the Minecraft test client) as a small JPEG.
# Brings the window to the front, copies its screen area, scales it to $width px wide.
# Usage: tools\winshot.ps1 -title 'Minecraft*' -out shot.jpg
Add-Type -AssemblyName System.Drawing
Add-Type @'
using System; using System.Runtime.InteropServices;
public static class WinShot {
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
}
'@ -ErrorAction SilentlyContinue
$p = Get-Process | Where-Object { $_.MainWindowTitle -like $title -and $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if (-not $p) { throw "no window like '$title'" }
$h = $p.MainWindowHandle
[WinShot]::ShowWindow($h, 9) | Out-Null   # restore if minimized
[WinShot]::SetForegroundWindow($h) | Out-Null
Start-Sleep -Milliseconds 300
$r = New-Object WinShot+RECT
[WinShot]::GetWindowRect($h, [ref]$r) | Out-Null
$w = $r.R - $r.L; $hh = $r.B - $r.T
$bmp = New-Object Drawing.Bitmap $w, $hh
$g = [Drawing.Graphics]::FromImage($bmp); $g.CopyFromScreen($r.L, $r.T, 0, 0, $bmp.Size); $g.Dispose()
$sh = [int]($hh * $width / $w)
$small = New-Object Drawing.Bitmap $width, $sh
$g = [Drawing.Graphics]::FromImage($small); $g.InterpolationMode = 'HighQualityBicubic'; $g.DrawImage($bmp, 0, 0, $width, $sh); $g.Dispose()
$enc = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
$ep = New-Object Drawing.Imaging.EncoderParameters 1
$ep.Param[0] = New-Object Drawing.Imaging.EncoderParameter ([Drawing.Imaging.Encoder]::Quality), ([long]$quality)
$small.Save($out, $enc, $ep); $bmp.Dispose(); $small.Dispose()
"saved $out ($w x $hh window)"
