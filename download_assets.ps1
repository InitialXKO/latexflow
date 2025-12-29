# Download MyScript iink Recognition Assets

$AssetsVersion = "4.2"
$BaseUrl = "https://download.myscript.com/iink/recognitionAssets_iink_$AssetsVersion"
$TargetDir = "app/src/main/assets"
$TempDir = "tmp-assets"

if (!(Test-Path $TargetDir)) { New-Item -ItemType Directory -Path $TargetDir }
if (!(Test-Path "$TargetDir/conf")) { New-Item -ItemType Directory -Path "$TargetDir/conf" }
if (!(Test-Path $TempDir)) { New-Item -ItemType Directory -Path $TempDir }

$Files = @(
    "myscript-iink-recognition-math.zip",
    "myscript-iink-recognition-math2.zip",
    "myscript-iink-recognition-text-en_US.zip"
)

foreach ($File in $Files) {
    $Url = "$BaseUrl/$File"
    $Dest = "$TempDir/$File"
    Write-Host "Downloading $Url..."
    Invoke-WebRequest -Uri $Url -OutFile $Dest
    
    Write-Host "Extracting $File..."
    Expand-Archive -Path $Dest -DestinationPath $TempDir -Force
}

Write-Host "Copying assets to $TargetDir..."
$RecognitionAssetDir = "$TempDir/recognition-assets"
if (Test-Path $RecognitionAssetDir) {
    Copy-Item -Path "$RecognitionAssetDir/*" -Destination $TargetDir -Recurse -Force
}

# Cleanup
# Remove-Item -Path $TempDir -Recurse -Force

Write-Host "Done! Assets are in $TargetDir"
