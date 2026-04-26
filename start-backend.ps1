$env:SPRING_DATASOURCE_URL="jdbc:mysql://gateway01.ap-southeast-1.prod.alicloud.tidbcloud.com:4000/tms_db?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="21c5u9UWuopDpJo.root"
$env:SPRING_DATASOURCE_PASSWORD="A2L0cZKKVrV5SZ3d"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="update"
$env:SPRING_JPA_SHOW_SQL="true"
$env:SPRING_SERVLET_MULTIPART_ENABLED="true"
$env:SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE="20MB"
$env:SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE="25MB"
$env:LMS_UPLOAD_DIR="/app/uploads/lms"
$env:SPRING_CACHE_TYPE="simple"

mvn spring-boot:run
