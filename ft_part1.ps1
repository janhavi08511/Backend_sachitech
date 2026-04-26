$BASE = "http://localhost:8080"
$PASS = 0; $FAIL = 0; $WARN = 0
$LOG  = [System.Collections.Generic.List[string]]::new()

function Log { param($status,$test,$detail)
  $sym = if($status -eq "PASS"){"[+]"}elseif($status -eq "FAIL"){"[X]"}else{"[!]"}
  $line = "$sym [$status] $test | $detail"
  $col  = if($status -eq "PASS"){"Green"}elseif($status -eq "FAIL"){"Red"}else{"Yellow"}
  Write-Host $line -ForegroundColor $col
  $script:LOG.Add($line)
  if($status -eq "PASS"){$script:PASS++}elseif($status -eq "FAIL"){$script:FAIL++}else{$script:WARN++}
}

function Invoke-API { param($method,$path,$body,$token)
  $h = @{"Content-Type"="application/json"}
  if($token){$h["Authorization"]="Bearer $token"}
  $uri = "$BASE$path"
  try {
    if($body){ return Invoke-RestMethod -Method $method -Uri $uri -Headers $h -Body ($body|ConvertTo-Json -Depth 5) -ErrorAction Stop }
    else     { return Invoke-RestMethod -Method $method -Uri $uri -Headers $h -ErrorAction Stop }
  } catch {
    $c = $_.Exception.Response.StatusCode.value__
    return [PSCustomObject]@{__error=$true;__code=$c;__msg=$_.Exception.Message}
  }
}
