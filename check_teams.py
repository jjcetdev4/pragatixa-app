import urllib.request
import json

data = json.dumps({"username": "hod_test", "password": "password"}).encode('utf-8')
req = urllib.request.Request('http://localhost:8080/api/v1/auth/authenticate', data=data, headers={'Content-Type': 'application/json'})
res = urllib.request.urlopen(req)
res_data = json.loads(res.read())
token = res_data['data']['token']

req2 = urllib.request.Request('http://localhost:8080/api/v1/teams', headers={'Authorization': 'Bearer ' + token})
res2 = urllib.request.urlopen(req2)
print(res2.read().decode())
