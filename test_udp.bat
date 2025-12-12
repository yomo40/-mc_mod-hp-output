@echo off
chcp 65001 >nul
echo ========================================
echo   HP Output UDP 监听器
echo   监听端口: 39571
echo   按 Ctrl+C 退出
echo ========================================
echo.

powershell -NoProfile -Command "$udp = New-Object System.Net.Sockets.UdpClient(39571); $ep = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0); Write-Host '正在监听 UDP 端口 39571...' -ForegroundColor Green; while ($true) { $data = $udp.Receive([ref]$ep); $msg = [System.Text.Encoding]::UTF8.GetString($data); $json = $msg | ConvertFrom-Json; $time = Get-Date -Format 'HH:mm:ss.fff'; switch ($json.type) { 'health' { Write-Host \"[$time] ❤️  HEALTH: $($json.health)/$($json.maxHealth) ($([math]::Round($json.percentage * 100))%%)\" -ForegroundColor Cyan } 'damage' { Write-Host \"[$time] 💔 DAMAGE: -$($json.damage) from '$($json.source)' | Health: $($json.health)\" -ForegroundColor Red } 'heal' { Write-Host \"[$time] 💚 HEAL: +$($json.amount) | Health: $($json.health)\" -ForegroundColor Green } 'death' { Write-Host \"[$time] 💀 DEATH: $($json.source)\" -ForegroundColor Magenta } } }"

pause
