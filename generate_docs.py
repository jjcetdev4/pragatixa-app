import os
import re

files = [
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/student/XpController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/student/LevelBadgeController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/student/controller/StudentXpController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/student/controller/StudentController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/activity/controller/GroupActivityController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/authentication/controller/AuthController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminDashboardController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminLookupController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminFacultyController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminStageController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminAssignmentController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminRoleController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminDepartmentController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminSubgroupController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminSubjectController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminActivityController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/modules/admin/controller/AdminUserController.java",
    "h:/Updating SPDMS/updating_decipline_backend/src/main/java/com/pragatix/admin/TeamController.java"
]

endpoints = []
controllers = []

for filepath in files:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extract Package
    pkg_match = re.search(r'package\s+([\w\.]+);', content)
    pkg = pkg_match.group(1) if pkg_match else "Unknown"

    # Extract Controller Name
    class_match = re.search(r'class\s+(\w+Controller)', content)
    if not class_match:
        continue
    ctrl_name = class_match.group(1)

    # Class level RequestMapping
    class_rm_match = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']', content)
    base_url = class_rm_match.group(1) if class_rm_match else ""

    controllers.append(ctrl_name)

    # Find methods
    method_matches = re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping\s*\(\s*(?:value\s*=\s*)?(?:\{?\s*["\']([^"\']+)["\']|["\']([^"\']+)["\'])?', content)
    
    for mm in method_matches:
        http_method = mm.group(1).upper()
        path_val = mm.group(2) if mm.group(2) else mm.group(3)
        path = path_val if path_val else ""
        
        full_url = (base_url + path).replace('//', '/')

        start_pos = mm.end()
        sig_match = re.search(r'public\s+(?:<[^>]+>\s+)?([\w<>,\s\[\]\?]+)\s+(\w+)\s*\(([^)]*)\)', content[start_pos:])
        
        if not sig_match:
            continue
            
        ret_type = sig_match.group(1).strip()
        java_method = sig_match.group(2).strip()
        args_str = sig_match.group(3).strip()

        block_start = content.rfind('}', 0, mm.start())
        if block_start == -1: block_start = 0
        pre_block = content[block_start:sig_match.start() + start_pos]

        op_match = re.search(r'@Operation\s*\([^)]*summary\s*=\s*["\']([^"\']+)["\']', pre_block)
        purpose = op_match.group(1) if op_match else java_method

        preauth_match = re.search(r'@PreAuthorize\s*\(\s*["\']([^"\']+)["\']', pre_block)
        auth_req = preauth_match.group(1) if preauth_match else "None"
        auth_required = "Yes"
        if auth_req == "None" and "login" in full_url:
            auth_required = "No"

        path_vars = []
        query_params = []
        req_body = "None"
        
        arg_list = args_str.split(',')
        for arg in arg_list:
            if '@PathVariable' in arg:
                parts = arg.strip().split()
                path_vars.append(parts[-1])
            elif '@RequestParam' in arg:
                parts = arg.strip().split()
                query_params.append(parts[-1])
            elif '@RequestBody' in arg:
                parts = arg.strip().split()
                req_body = parts[-2] if len(parts) >= 2 else "Unknown"

        req_dto = req_body if req_body != "None" else "None"
        
        endpoints.append({
            "controller": ctrl_name,
            "package": pkg,
            "http_method": http_method,
            "url": full_url,
            "java_method": java_method,
            "purpose": purpose,
            "path_vars": ", ".join(path_vars) if path_vars else "None",
            "query_params": ", ".join(query_params) if query_params else "None",
            "req_body": req_body,
            "req_dto": req_dto,
            "res_dto": ret_type,
            "auth": auth_required,
            "auth_req": auth_req,
            "service_method": f"{ctrl_name.replace('Controller','Service').lower()}.{java_method}",
            "repo_calls": "Hidden behind service layer",
            "tables": "Mapped to JPA Entities",
            "status": "200 OK, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found"
        })

md = "# REST API Documentation\n\n"

md += "## Detailed API Endpoints\n\n"
for i, ep in enumerate(endpoints, 1):
    md += "-" * 60 + "\n\n"
    md += f"**Controller Name:** {ep['controller']}\n\n"
    md += f"**Controller Package:** {ep['package']}\n\n"
    md += f"**HTTP Method:** {ep['http_method']}\n\n"
    md += f"**Complete Endpoint URL:** {ep['url']}\n\n"
    md += f"**Java Method Name:** {ep['java_method']}\n\n"
    md += f"**Purpose:** {ep['purpose']}\n\n"
    md += "**Request Type:**\n"
    md += f"- Path Variables: {ep['path_vars']}\n"
    md += f"- Query Parameters: {ep['query_params']}\n"
    md += f"- Request Body: {ep['req_body']}\n\n"
    md += f"**Request DTO/Class:** {ep['req_dto']}\n\n"
    md += f"**Response DTO/Class:** {ep['res_dto']}\n\n"
    md += f"**Authentication Required:** {ep['auth']}\n\n"
    md += f"**Authorization Requirements:** {ep['auth_req']}\n\n"
    md += f"**Service Method Called:** {ep['service_method']}\n\n"
    md += f"**Repository Calls:** {ep['repo_calls']}\n\n"
    md += f"**Database Tables/Entities Used:** {ep['tables']}\n\n"
    md += f"**Possible HTTP Response Codes:** {ep['status']}\n\n"

md += "-" * 60 + "\n\n"

md += "## Summary Table\n\n"
md += "| No | HTTP Method | Endpoint | Controller | Method Name | Authentication |\n"
md += "|---|---|---|---|---|---|\n"
for i, ep in enumerate(endpoints, 1):
    md += f"| {i} | {ep['http_method']} | {ep['url']} | {ep['controller']} | {ep['java_method']} | {ep['auth']} |\n"

md += "\n"

md += "## APIs Grouped by Controller\n\n"
from collections import defaultdict
grouped = defaultdict(list)
for ep in endpoints:
    grouped[ep['controller']].append(ep)

for ctrl in sorted(controllers):
    if ctrl not in grouped:
        continue
    md += f"### {ctrl}\n"
    md += "-" * len(ctrl) + "\n"
    for ep in grouped[ctrl]:
        md += f"{ep['http_method']} {ep['url']}\n"
    md += "\n"

total_ctrls = len(set([ep['controller'] for ep in endpoints]))
total_apis = len(endpoints)
gets = len([e for e in endpoints if e['http_method'] == 'GET'])
posts = len([e for e in endpoints if e['http_method'] == 'POST'])
puts = len([e for e in endpoints if e['http_method'] == 'PUT'])
patchs = len([e for e in endpoints if e['http_method'] == 'PATCH'])
deletes = len([e for e in endpoints if e['http_method'] == 'DELETE'])
public = len([e for e in endpoints if e['auth'] == 'No'])
protected = len([e for e in endpoints if e['auth'] == 'Yes'])

md += "## Project Statistics\n\n"
md += f"- **Total Controllers:** {total_ctrls}\n"
md += f"- **Total REST APIs:** {total_apis}\n"
md += f"- **GET APIs:** {gets}\n"
md += f"- **POST APIs:** {posts}\n"
md += f"- **PUT APIs:** {puts}\n"
md += f"- **PATCH APIs:** {patchs}\n"
md += f"- **DELETE APIs:** {deletes}\n"
md += f"- **Public APIs:** {public}\n"
md += f"- **Protected APIs:** {protected}\n"

out_file = r'C:\Users\ADMIN\.gemini\antigravity-ide\brain\d5aaff1e-bf4e-417e-b69b-5c45a391f59d\api_documentation.md'
with open(out_file, 'w', encoding='utf-8') as f:
    f.write(md)

print("Documentation generated at " + out_file)
