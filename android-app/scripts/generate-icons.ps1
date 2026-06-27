$source = "C:\Users\javlo\.cursor\projects\c-Users-javlo-Desktop-vazifa\assets\c__Users_javlo_AppData_Roaming_Cursor_User_workspaceStorage_89080fce793c74fa0101933491c530c5_images_ChatGPT_Image_25____._2026__.__22_49_53-435d9d55-d4a8-4b29-adc2-dee17f6c2ae2.png"
$baseDir = "c:\Users\javlo\Desktop\vazifa\android-app\app\src\main\res"

Add-Type -AssemblyName System.Drawing

function Save-ResizedImage($img, $size, $path) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($img, 0, 0, $size, $size)
    $g.Dispose()
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$img = [System.Drawing.Image]::FromFile($source)

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $sizes.GetEnumerator()) {
    $folder = $entry.Key
    $size = $entry.Value
    $dir = Join-Path $baseDir $folder
    Save-ResizedImage $img $size (Join-Path $dir "ic_launcher.png")
    Save-ResizedImage $img $size (Join-Path $dir "ic_launcher_round.png")
}

$nodpiDir = Join-Path $baseDir "drawable-nodpi"
Save-ResizedImage $img 512 (Join-Path $nodpiDir "ic_launcher_foreground.png")

$img.Dispose()
Write-Output "Done. Resized icons generated in $baseDir"
