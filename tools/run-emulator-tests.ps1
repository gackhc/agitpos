$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    if ($env:AGITPOS_GRADLE_JAVA_HOME) { $env:JAVA_HOME = $env:AGITPOS_GRADLE_JAVA_HOME }
    & .\gradlew.bat :app:testDebugUnitTest -PfirebaseEmulatorTests=true --tests 'ggack.agitpos.data.FirebaseLedgerIntegrationTest' --no-daemon --no-configuration-cache --console=plain
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
