$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$applications = @(
    'apps\volume-controller',
    'apps\prime-tving-redirect'
)

foreach ($application in $applications) {
    $projectDirectory = Join-Path $repositoryRoot $application
    Write-Host "Building $application"
    & gradle --no-daemon -p $projectDirectory assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed: $application"
    }
}

Write-Host 'All applications built successfully.'
