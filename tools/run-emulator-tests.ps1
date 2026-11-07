$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    & .\gradlew.bat :app:testDebugUnitTest -PfirebaseEmulatorTests=true --tests 'ggack.agitpos.data.FirebaseLedgerIntegrationTest' --no-daemon --no-configuration-cache --console=plain
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
