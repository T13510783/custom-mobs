param([string]$url, [string]$expr, [string]$out, [int]$timeoutSec = 600, [int]$port = 9333, [int]$size = 1000, [string[]]$extra = @(),
  [string]$shot = '', [int]$shotQuality = 60)
# $extra: more browser flags, e.g. '--allow-file-access-from-files' (read local video/audio pixels and samples).
# $shot: also save a JPEG screenshot of the page (to the scratchpad, never the project) once $expr returns.
# Headless Brave + DevTools protocol: open $url, poll $expr until it returns a non-empty string, save it to $out.
# Example: cdp.ps1 -url 'file:///C:/custom-mobs/custom-models/mobs/memes/the_king/the_king.html#cliptest' -expr "(document.getElementById('cliptest')||{}).textContent||''" -out clip.txt
$b = 'C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe'
$ud = Join-Path $env:TEMP ('brv_' + [guid]::NewGuid().ToString('N'))
Start-Process -FilePath $b -ArgumentList (@('--headless=new', '--enable-unsafe-swiftshader', "--user-data-dir=$ud",
  "--remote-debugging-port=$port", "--window-size=$size,$size", '--hide-scrollbars', '--no-first-run') + $extra + @($url))
$start = Get-Date
try {
  $ws = $null
  while (-not $ws) {
    Start-Sleep -Milliseconds 500
    try { $list = Invoke-RestMethod "http://127.0.0.1:$port/json" -TimeoutSec 5 } catch { continue }
    $page = $list | Where-Object { $_.type -eq 'page' } | Select-Object -First 1
    if ($page) { $ws = $page.webSocketDebuggerUrl }
    if (((Get-Date) - $start).TotalSeconds -gt 60) { throw 'no devtools page' }
  }
  $sock = New-Object System.Net.WebSockets.ClientWebSocket
  $ct = [Threading.CancellationToken]::None
  $sock.ConnectAsync([Uri]$ws, $ct).Wait()
  $id = 0
  function Eval([string]$e) { Send 'Runtime.evaluate' @{ expression = $e; returnByValue = $true } }
  function Send([string]$method, $params) {
    $script:id++
    $msg = @{ id = $script:id; method = $method; params = $params } | ConvertTo-Json -Compress -Depth 5
    $bytes = [Text.Encoding]::UTF8.GetBytes($msg)
    $sock.SendAsync((New-Object ArraySegment[byte] -ArgumentList (, $bytes)), 'Text', $true, $ct).Wait()
    $buf = New-Object byte[] 1048576
    while ($true) {
      $ms = New-Object IO.MemoryStream
      do {
        $r = $sock.ReceiveAsync((New-Object ArraySegment[byte] -ArgumentList (, $buf)), $ct).Result
        $ms.Write($buf, 0, $r.Count)
      } while (-not $r.EndOfMessage)
      $txt = [Text.Encoding]::UTF8.GetString($ms.ToArray())
      if ($txt -match ('"id":' + $script:id + '[,}]')) { return $txt }
    }
  }
  while ($true) {
    $res = Eval $expr | ConvertFrom-Json
    $v = $res.result.result.value
    if ($v) {
      [IO.File]::WriteAllText($out, $v); 'saved ' + $v.Length + ' chars in ' + [int]((Get-Date) - $start).TotalSeconds + ' s'
      if ($shot) { # page screenshot as JPEG, taken right after $expr returned
        $img = (Send 'Page.captureScreenshot' @{ format = 'jpeg'; quality = $shotQuality } | ConvertFrom-Json).result.data
        [IO.File]::WriteAllBytes($shot, [Convert]::FromBase64String($img)); "shot $shot"
      }
      break
    }
    if ($res.result.exceptionDetails) { 'eval error: ' + ($res.result.exceptionDetails | ConvertTo-Json -Depth 4 -Compress) }
    if (((Get-Date) - $start).TotalSeconds -gt $timeoutSec) { 'timeout'; break }
    Start-Sleep -Seconds 2
  }
  $sock.Dispose()
} finally {
  Get-CimInstance Win32_Process -Filter "Name='brave.exe'" | Where-Object { $_.CommandLine -like "*$ud*" } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
  Start-Sleep -Milliseconds 500
  Remove-Item -Recurse -Force $ud -ErrorAction SilentlyContinue
}
