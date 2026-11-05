cd C:\Users\11\controle-gastos\api
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)\s*=\s*(.+)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim().Trim('"', "'")
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}
.\mvnw.cmd spring-boot:run
