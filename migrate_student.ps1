 = @(
  'CreateStudentRequest,com.pragatix.dto,com.pragatix.modules.student.dto.request',
  'UpdateStudentRequest,com.pragatix.dto,com.pragatix.modules.student.dto.request',
  'MyActivityStudentsResponse,com.pragatix.dto,com.pragatix.modules.student.dto.response',
  'StudentBadgeResponse,com.pragatix.dto,com.pragatix.modules.student.dto.response',
  'StudentResponse,com.pragatix.dto,com.pragatix.modules.student.dto.response',
  'StudentNotFoundException,com.pragatix.exception,com.pragatix.modules.student.exception',
  'StudentActivityXpRepository,com.pragatix.repository,com.pragatix.modules.student.repository',
  'StudentBadgeRepository,com.pragatix.repository,com.pragatix.modules.student.repository',
  'StudentGroupRepository,com.pragatix.repository,com.pragatix.modules.student.repository',
  'StudentRepository,com.pragatix.repository,com.pragatix.modules.student.repository',
  'StudentService,com.pragatix.student,com.pragatix.modules.student.service',
  'StudentController,com.pragatix.student,com.pragatix.modules.student.controller',
  'StudentXpController,com.pragatix.student,com.pragatix.modules.student.controller'
)

foreach ($move in $moves) {
  $parts = $move.Split(',')
  $className = $parts[0]
  $oldPkg = $parts[1]
  $newPkg = $parts[2]
  
  Write-Host ""
  Write-Host "=========================================="
  Write-Host "Processing $className..."
  Write-Host "=========================================="
  
  python 'C:\Users\ADMIN\.gemini\antigravity-ide\brain\076c9dcc-b359-4d70-903b-677f12ffccf2\scratch\refactor_simple.py' $className $oldPkg $newPkg
  
  mvn clean compile -q
  if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed on $className. Stopping."
    exit 1
  }
  
  git add .
  git commit -m "Migrate $className to Student module"
}
