# Data Stwardship Exercise 2

curl -X POST -H "Content-Type: application/json" \
 -d '{"name":"maDMP workflow","steps":[{"provider":":internal","stepType":"http/sr","parameters":{"url":"http://localhost:8081/madmp/${invocationId}/${dataset.id}","method":"POST","contentType":"text/plain","body":"","expectedResponse":"OK.*"}}]}' \
 http://localhost:8080/api/admin/workflows/
