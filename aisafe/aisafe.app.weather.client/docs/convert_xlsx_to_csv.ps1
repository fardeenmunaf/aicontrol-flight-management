$xlsx_path = "C:\Users\Tomas\OneDrive\Documentos\GitHub\sem4pi2526-sem4pi2526_2dk5\aisafe\aisafe.app.weather.client\docs"

$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false

try {
    # Convert CW_WeatherData_v0.xlsx
    $cwFile = Join-Path $xlsx_path "CW_WeatherData_v0.xlsx"
    $cwCsv = Join-Path $xlsx_path "CW_WeatherData_v0.csv"
    $wb = $excel.Workbooks.Open($cwFile)
    $wb.SaveAs($cwCsv, 6)
    $wb.Close($false)
    Write-Host "✓ CW_WeatherData_v0.xlsx -> CW_WeatherData_v0.csv"

    # Convert HP_WeatherData_v0.xlsx
    $hpFile = Join-Path $xlsx_path "HP_WeatherData_v0.xlsx"
    $hpCsv = Join-Path $xlsx_path "HP_WeatherData_v0.csv"
    $wb = $excel.Workbooks.Open($hpFile)
    $wb.SaveAs($hpCsv, 6)
    $wb.Close($false)
    Write-Host "✓ HP_WeatherData_v0.xlsx -> HP_WeatherData_v0.csv"
}
finally {
    $excel.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($excel) | Out-Null
    [System.GC]::Collect()
}

Write-Host "Conversão concluída!"

