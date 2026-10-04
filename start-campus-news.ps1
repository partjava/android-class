param([switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONUNBUFFERED = '1'
function Step($Number, $Message) { Write-Host "`n[$Number/5] $Message" -ForegroundColor Cyan }
function Invoke-PythonCommand($Executable, $Arguments) {
    $ErrorActionPreference = 'Continue'
    & $Executable @Arguments
    $script:pythonExit = $LASTEXITCODE
}
function Invoke-PythonCode($Executable, $Code) {
    # Base64 avoids Windows PowerShell 5 stripping quotes from native arguments.
    $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Code))
    $ErrorActionPreference = 'Continue'
    & $Executable -B -c "import base64;exec(base64.b64decode('$encoded'))"
    $script:pythonExit = $LASTEXITCODE
}
try {
    Step 1 '寻找本机 Python（PATH、Python Launcher、安装注册表和常见安装目录）'
    $candidates = [Collections.Generic.List[string]]::new()
    foreach ($name in @('python.exe', 'python3.exe')) {
        foreach ($command in @(Get-Command $name -All -ErrorAction SilentlyContinue)) {
            if ($command.Source -and $command.Source -notlike '*\Microsoft\WindowsApps\*') { $candidates.Add($command.Source) }
        }
    }
    $launcher = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($launcher) {
        foreach ($line in @(& $launcher.Source -0p 2>$null)) {
            if ($line -match '([A-Za-z]:\\.+?python(?:\d+(?:\.\d+)?)?\.exe)\s*$') { $candidates.Add($Matches[1].Trim()) }
        }
    }
    foreach ($registryRoot in @('HKCU:\Software\Python\PythonCore', 'HKLM:\Software\Python\PythonCore', 'HKLM:\Software\WOW6432Node\Python\PythonCore')) {
        foreach ($version in @(Get-ChildItem -LiteralPath $registryRoot -ErrorAction SilentlyContinue)) {
            $installKey = Get-Item -LiteralPath ($version.PSPath + '\InstallPath') -ErrorAction SilentlyContinue
            if ($installKey) {
                $executable = $installKey.GetValue('ExecutablePath')
                if (!$executable) { $executable = Join-Path $installKey.GetValue('') 'python.exe' }
                if ($executable) { $candidates.Add($executable) }
            }
        }
    }
    foreach ($prefix in @($env:CONDA_PREFIX, "$env:USERPROFILE\anaconda3", "$env:USERPROFILE\miniconda3", "$env:ProgramData\anaconda3", "$env:ProgramData\miniconda3")) {
        if ($prefix) { $candidates.Add((Join-Path $prefix 'python.exe')) }
    }
    foreach ($root in @("$env:LOCALAPPDATA\Programs\Python", $env:ProgramFiles)) {
        foreach ($folder in @(Get-ChildItem -LiteralPath $root -Directory -Filter 'Python*' -ErrorAction SilentlyContinue)) { $candidates.Add((Join-Path $folder.FullName 'python.exe')) }
    }
    $pythonPath = $null
    foreach ($candidate in @($candidates | Select-Object -Unique)) {
        if (!(Test-Path -LiteralPath $candidate -PathType Leaf)) { continue }
        Write-Host "检查解释器：$candidate"
        $probe = @(Invoke-PythonCode $candidate 'import sys; print("CAMPUS_PYTHON_OK" if sys.version_info >= (3,8) else "TOO_OLD"); print(sys.executable)')
        if ($pythonExit -eq 0 -and $probe -contains 'CAMPUS_PYTHON_OK') { $pythonPath = $candidate; break }
    }
    if (!$pythonPath) { throw '未找到可用的 Python 3.8+。请安装 Python 并勾选 Add Python to PATH，然后重新双击启动。' }
    Write-Host "使用解释器：$pythonPath" -ForegroundColor Green
    Step 2 '检查爬虫文件和依赖'
    if (!(Test-Path -LiteralPath (Join-Path $PSScriptRoot 'campus_news_server.py'))) { throw '项目中缺少 campus_news_server.py，请复制完整项目。' }
    $checkCode = 'import importlib.util; mods={"flask":"flask","requests":"requests","bs4":"beautifulsoup4","urllib3":"urllib3"}; print(" ".join(pkg for mod,pkg in mods.items() if importlib.util.find_spec(mod) is None))'
    $missingText = (@(Invoke-PythonCode $pythonPath $checkCode) -join '').Trim()
    if ($pythonExit -ne 0) { throw 'Python 依赖检测失败，请查看上方错误。' }
    $missing = @(); if ($missingText) { $missing = @($missingText -split '\s+') }
    if ($missing.Count) { Write-Host "缺少：$($missing -join ', ')" -ForegroundColor Yellow } else { Write-Host 'flask、requests、beautifulsoup4、urllib3 已安装。' -ForegroundColor Green }
    if ($CheckOnly) { Write-Host '检测完成（CheckOnly：未安装依赖、未启动服务）。'; exit 0 }
    Step 3 '准备 pip 并安装缺失依赖（安装输出和下载进度如下）'
    if ($missing.Count) {
        Invoke-PythonCommand $pythonPath @('-m','pip','--version')
        if ($pythonExit -ne 0) {
            Write-Host '正在通过 ensurepip 初始化 pip…'
            Invoke-PythonCommand $pythonPath @('-m','ensurepip','--upgrade')
            if ($pythonExit -ne 0) { throw '无法初始化 pip，请修复 Python 安装。' }
        }
        $installArgs = @('-m','pip','install','--progress-bar','on','--disable-pip-version-check')
        $isVirtual = (@(Invoke-PythonCode $pythonPath 'import sys; print(int(sys.prefix != sys.base_prefix))') -join '').Trim()
        if ($isVirtual -eq '0') { $installArgs += '--user' }
        $installArgs += $missing
        $mirrorUrl = 'https://pypi.tuna.tsinghua.edu.cn/simple'
        Write-Host "使用清华镜像：$mirrorUrl" -ForegroundColor Cyan
        Invoke-PythonCommand $pythonPath ($installArgs + @('--index-url', $mirrorUrl, '--timeout', '15', '--retries', '1'))
        if ($pythonExit -ne 0) {
            Write-Host '镜像安装失败，正在回退官方源：https://pypi.org/simple' -ForegroundColor Yellow
            Invoke-PythonCommand $pythonPath ($installArgs + @('--index-url', 'https://pypi.org/simple', '--timeout', '15', '--retries', '1'))
        }
        if ($pythonExit -ne 0) { throw '清华镜像和官方源均安装失败，请检查网络及上方 pip 输出；修复后重新双击即可继续。' }
    } else { Write-Host '无需下载，跳过安装。' }
    Step 4 '验证依赖可以导入'
    Invoke-PythonCode $pythonPath 'import flask,requests,bs4,urllib3; print("依赖验证成功")'
    if ($pythonExit -ne 0) { throw '依赖已找到但无法导入，请查看错误并修复当前 Python 环境。' }
    Step 5 '启动校园新闻服务（保持本终端打开，Ctrl+C 停止）'
    Write-Host '模拟器地址：http://10.0.2.2:5000'
    Write-Host '真机地址：电脑的局域网 IP + :5000；在 App 网络配置里填写。'
    Invoke-PythonCommand $pythonPath @('-B','-u',(Join-Path $PSScriptRoot 'campus_news_server.py'))
    if ($pythonExit -ne 0) { throw "服务启动失败或异常退出（退出码 $pythonExit），请查看上方日志。如端口占用，请关闭重复服务后重试。" }
} catch {
    Write-Host "`n[失败] $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
