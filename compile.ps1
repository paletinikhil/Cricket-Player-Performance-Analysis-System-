$projectDir = "c:\Users\Lenovo\OneDrive\Dokumen\sem 6\OOAD\ooad mini project"
cd $projectDir

# Create bin directory if it doesn't exist  
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" -Force | Out-Null }

# Collect Java files - from src/ subdirectories only (main, model, repository, service)
$javaFiles = @()
$javaFiles += Get-ChildItem -Path "src/main" -Filter "*.java" | Where-Object { $_.Name -ne "MainGui.java" -and $_.Name -ne "Mainapp.java" } | ForEach-Object { $_.FullName }
$javaFiles += Get-ChildItem -Path "src/main" -Filter "WebAppServer.java" | ForEach-Object { $_.FullName }  
$javaFiles += Get-ChildItem -Path "src/model" -Filter "*.java" | ForEach-Object { $_.FullName }
$javaFiles += Get-ChildItem -Path "src/repository" -Filter "*.java" | ForEach-Object { $_.FullName }
$javaFiles += Get-ChildItem -Path "src/service" -Filter "*.java" | ForEach-Object { $_.FullName }

Write-Host "Compiling $($javaFiles.Count) Java files..."

# Compile - pass files as array arguments
& javac -cp "lib/mysql-connector-j-9.6.0.jar" -d "bin" -sourcepath "src" --release 17 $javaFiles 2>&1

if ($LASTEXITCODE -eq 0) {
  Write-Host "Compilation successful!" -ForegroundColor Green
} else {
  Write-Host "Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
