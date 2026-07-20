 = @(
  'CreateStudentRequest,com.spdms.dto,com.spdms.modules.student.dto.request',
  'UpdateStudentRequest,com.spdms.dto,com.spdms.modules.student.dto.request',
  'MyActivityStudentsResponse,com.spdms.dto,com.spdms.modules.student.dto.response',
  'StudentBadgeResponse,com.spdms.dto,com.spdms.modules.student.dto.response',
  'StudentResponse,com.spdms.dto,com.spdms.modules.student.dto.response',
  'StudentNotFoundException,com.spdms.exception,com.spdms.modules.student.exception',
  'StudentActivityXpRepository,com.spdms.repository,com.spdms.modules.student.repository',
  'StudentBadgeRepository,com.spdms.repository,com.spdms.modules.student.repository',
  'StudentGroupRepository,com.spdms.repository,com.spdms.modules.student.repository',
  'StudentRepository,com.spdms.repository,com.spdms.modules.student.repository',
  'StudentService,com.spdms.student,com.spdms.modules.student.service',
  'StudentController,com.spdms.student,com.spdms.modules.student.controller',
  'StudentXpController,com.spdms.student,com.spdms.modules.student.controller'
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
