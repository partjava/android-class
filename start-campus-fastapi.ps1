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
        $probe = @(Invoke-PythonCode $candidate 'import sys; print("CAMPUS_PYTHON_OK" if sys.version_info >= (3,10) else "TOO_OLD"); print(sys.executable)')
        if ($pythonExit -eq 0 -and $probe -contains 'CAMPUS_PYTHON_OK') { $pythonPath = $candidate; break }
    }
    if (!$pythonPath) { throw '未找到可用的 Python 3.10+。请安装 Python 并勾选 Add Python to PATH，然后重新双击启动。' }
    Write-Host "使用解释器：$pythonPath" -ForegroundColor Green
    Step 2 '检查 FastAPI 后端依赖'
    if (!(Test-Path -LiteralPath (Join-Path $PSScriptRoot 'backend/campus_news_fastapi.py'))) { throw '项目中缺少 campus_news_fastapi.py。' }
    $checkCode = 'import importlib.util; mods={"fastapi":"fastapi","uvicorn":"uvicorn","requests":"requests","bs4":"beautifulsoup4","flask":"flask"}; print(" ".join(pkg for mod,pkg in mods.items() if importlib.util.find_spec(mod) is None))'
    $missingOutput = @(Invoke-PythonCode $pythonPath $checkCode)
    $missing = if ($missingOutput) { ($missingOutput[-1] -split '\s+') | Where-Object { $_ } } else { @() }
    if ($missing.Count -eq 0) {
        Write-Host '依赖完整，无需安装。' -ForegroundColor Green
    } else {
        Step 3 ("安装缺失依赖（{0}）" -f ($missing -join ', '))
        $mirror = 'https://pypi.tuna.tsinghua.edu.cn/simple'
        Write-Host "使用清华镜像源：$mirror"
        Invoke-PythonCommand $pythonPath (@('-m', 'pip', 'install', '-i', $mirror, '--timeout', '60') + $missing)
        if ($pythonExit -ne 0) {
            Write-Host '清华源安装失败，回退到官方源重试...' -ForegroundColor Yellow
            Invoke-PythonCommand $pythonPath (@('-m', 'pip', 'install', '--timeout', '60') + $missing)
        }
        if ($pythonExit -ne 0) { throw '依赖安装失败，请检查网络连接后重试。' }
    }
    Step 4 '验证服务模块导入'
    Invoke-PythonCode $pythonPath 'import backend.campus_news_fastapi; print("FASTAPI_IMPORT_OK")'
    if ($pythonExit -ne 0) { throw '模块导入验证失败。' }
    Write-Host 'FastAPI 环境就绪！' -ForegroundColor Green
    if ($CheckOnly) {
        Write-Host '仅检查模式完成。' -ForegroundColor Green
        exit 0
    }
    Step 5 '启动 FastAPI 校园综合服务 (监听 8000 端口)'
    Write-Host 'Swagger 文档地址：http://localhost:8000/docs' -ForegroundColor Yellow
    Write-Host 'Android 模拟器地址：http://10.0.2.2:8000/api/v1' -ForegroundColor Yellow
    Write-Host '按 Ctrl+C 可停止服务。' -ForegroundColor Cyan
    Invoke-PythonCommand $pythonPath @('-B','-u','-m','backend.campus_news_fastapi')
    exit $pythonExit
} catch {
    Write-Host "`n[错误] $_" -ForegroundColor Red
    exit 1
}
