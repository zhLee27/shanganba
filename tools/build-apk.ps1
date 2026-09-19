# 一键打包 / 跑测试
# 说明：Gradle 的 fork 进程在中文目录下会拿不到 worker jar，所以脚本会先在 C 盘建两个
#       只占几字节的目录联接（junction）指向 D 盘的真实目录，用英文路径来编译。
#       代码、工具链、产物仍然全部存在 D 盘。
param(
    [string]$VersionName = "",
    [string]$Notes = "",
    [int]$VersionCode = 2,
    [switch]$Debug,
    [switch]$Test
)

$ErrorActionPreference = "Stop"

# 真实目录（都在 D 盘）
$realRoot = Split-Path -Parent $PSScriptRoot
$realDev  = "D:\AI项目开发\AndroidDev"

# 英文别名（junction，只占几字节，不复制数据）
$linkBase = "C:\Users\Administrator\Documents\Codex"
$devLink  = Join-Path $linkBase "tools\dev"
$projLink = Join-Path $linkBase "build-links\shanganba"

function Ensure-Junction($link, $target) {
    if (-not (Test-Path -LiteralPath $link)) {
        New-Item -ItemType Directory -Path (Split-Path -Parent $link) -Force | Out-Null
        New-Item -ItemType Junction -Path $link -Target $target | Out-Null
    }
}
Ensure-Junction $devLink $realDev
Ensure-Junction $projLink $realRoot

# Gradle 通过 local.properties 找 SDK，必须写成英文别名路径，否则测试进程会读不到
$localProps = Join-Path $realRoot "local.properties"
$sdkForProps = ($devLink + "\android-sdk").Replace("\", "/")
Set-Content -LiteralPath $localProps -Value ("sdk.dir=" + $sdkForProps) -Encoding ASCII

$jdk = Join-Path $devLink "jdk-17\jdk-17.0.20.1+1"
$env:JAVA_HOME         = $jdk
$env:ANDROID_SDK_ROOT  = Join-Path $devLink "android-sdk"
$env:ANDROID_HOME      = $env:ANDROID_SDK_ROOT
$env:ANDROID_USER_HOME = Join-Path $devLink "android-home"
$env:GRADLE_USER_HOME  = Join-Path $devLink "gradle-home"
$env:TEMP              = Join-Path $devLink "temp"
$env:TMP               = $env:TEMP
$env:PATH              = "$jdk\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:PATH"

$gradle = Join-Path $devLink "gradle\gradle-8.11.1\bin\gradle.bat"

if ($Test) {
    Write-Host "==> 跑单元测试" -ForegroundColor Cyan
    & $gradle -p $projLink testReleaseUnitTest --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "单元测试失败" }
}

$task = if ($Debug) { "assembleDebug" } else { "assembleRelease" }
Write-Host "==> 编译 $task" -ForegroundColor Cyan
& $gradle -p $projLink $task --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle 编译失败，退出码 $LASTEXITCODE" }

$variant = if ($Debug) { "debug" } else { "release" }
$apkSrc = Get-ChildItem -Path (Join-Path $realRoot "app\build\outputs\apk\$variant") -Filter *.apk |
          Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apkSrc) { throw "没找到生成的 APK" }

$dist = Join-Path $realRoot "dist"
New-Item -ItemType Directory -Path $dist -Force | Out-Null
$apkName = if ($Debug) { "shanganba-debug.apk" } else { "shanganba.apk" }
if (-not $Debug -and $VersionName -ne "") { $apkName = "shanganba-$VersionName.apk" }
$apkDst = Join-Path $dist $apkName
Copy-Item $apkSrc.FullName $apkDst -Force

if (-not $Debug) {
    Write-Host "==> 校验签名与包信息" -ForegroundColor Cyan
    $apksigner = Join-Path $env:ANDROID_SDK_ROOT "build-tools\35.0.0\apksigner.bat"
    & $apksigner verify --print-certs $apkDst | Select-Object -First 2
    $aapt2 = Join-Path $env:ANDROID_SDK_ROOT "build-tools\35.0.0\aapt2.exe"
    & $aapt2 dump badging $apkDst | Select-String "package:|sdkVersion|targetSdkVersion|application-label" | Select-Object -First 4
}

$hash = (Get-FileHash $apkDst -Algorithm SHA256).Hash.ToLower()
$size = (Get-Item $apkDst).Length
$versionJson = [ordered]@{
    versionCode = $VersionCode
    versionName = if ($VersionName -ne "") { $VersionName } else { "1.1.0" }
    url         = ""
    sha256      = $hash
    size        = $size
    notes       = $Notes
    minSupportedVersionCode = 1
}
$versionJson | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $dist "version.json") -Encoding UTF8

Write-Host ""
Write-Host "完成：$apkDst" -ForegroundColor Green
Write-Host ("大小：{0:N2} MB" -f ($size / 1MB))
Write-Host "SHA256：$hash"
Write-Host "发布清单：$(Join-Path $dist 'version.json')"
