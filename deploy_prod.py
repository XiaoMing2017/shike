import os
import sys
import time
import subprocess
import requests
import paramiko

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

# ==================== 配置区 ====================
SERVER_IP = "117.72.61.18"
SERVER_PORT = 22
SERVER_USER = "root"
SERVER_PASS = "Nizouba12138."
REMOTE_APP_DIR = "/root/shike"
REMOTE_JAR_PATH = f"{REMOTE_APP_DIR}/app.jar"

PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.join(PROJECT_ROOT, "shike-backend")
LOCAL_JAR_PATH = os.path.join(BACKEND_DIR, "target", "shike-backend-1.0.0.jar")
JDK17_PATH = r"D:\Program Files\Java\jdk17"

def build_local_jar():
    print("=" * 60)
    print(" [Step 1/5] 本地 Maven 打包编译中 (使用 JDK 17)...")
    print("=" * 60)
    
    env = os.environ.copy()
    if os.path.exists(JDK17_PATH):
        env["JAVA_HOME"] = JDK17_PATH
        env["PATH"] = f"{JDK17_PATH}\\bin;" + env["PATH"]
    
    cmd = "mvn clean package -DskipTests --settings settings.xml"
    process = subprocess.Popen(cmd, cwd=BACKEND_DIR, shell=True, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace')
    
    for line in process.stdout:
        if any(keyword in line for keyword in ["[INFO] Building", "[INFO] Compiling", "[INFO] Replacing", "BUILD SUCCESS", "BUILD FAILURE"]):
            print("  -> " + line.strip())
            
    process.wait()
    if process.returncode != 0:
        print("❌ 本地 Maven 编译失败！请检查代码错误。")
        sys.exit(1)
        
    if not os.path.exists(LOCAL_JAR_PATH):
        print(f"❌ 找不到生成的 JAR 文件: {LOCAL_JAR_PATH}")
        sys.exit(1)
        
    jar_size_mb = os.path.getsize(LOCAL_JAR_PATH) / (1024 * 1024)
    print(f"✅ 本地打包成功！产物大小: {jar_size_mb:.2f} MB")

def upload_jar_and_config():
    print("=" * 60)
    print(f" [Step 2/5] 传输制品 app.jar 到生产服务器 ({SERVER_IP})...")
    print("=" * 60)
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(SERVER_IP, port=SERVER_PORT, username=SERVER_USER, password=SERVER_PASS)
    
    sftp = ssh.open_sftp()
    
    # 确保远程目标目录存在
    try:
        sftp.stat(REMOTE_APP_DIR)
    except IOError:
        sftp.mkdir(REMOTE_APP_DIR)
        
    def progress_callback(transferred, total):
        pct = (transferred / total) * 100
        sys.stdout.write(f"\r  -> 上传进度: {pct:.1f}% ({transferred/(1024*1024):.2f}/{total/(1024*1024):.2f} MB)")
        sys.stdout.flush()
        
    sftp.put(LOCAL_JAR_PATH, REMOTE_JAR_PATH, callback=progress_callback)
    print("\n✅ app.jar 制品上传成功！")
    
    # 写入规范的纯运行时 docker-compose.yml
    docker_compose_content = """version: '3.8'

services:
  # ==================== MySQL ====================
  shike-mysql:
    image: mysql:8.0
    container_name: shike-mysql
    restart: always
    ports:
      - "3306:3306"
    command: --default-authentication-plugin=mysql_native_password --innodb-buffer-pool-size=512M --max-connections=100 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    environment:
      - MYSQL_DATABASE=db_shike
      - MYSQL_ROOT_PASSWORD=Nizouba12138.
      - TZ=Asia/Shanghai
      - LANG=C.UTF-8
    volumes:
      - shike-mysql-data:/var/lib/mysql
    networks:
      - shike-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-u", "root", "-pNizouba12138."]
      interval: 10s
      timeout: 5s
      retries: 5

  # ==================== Redis ====================
  shike-redis:
    image: redis:7.2-alpine
    container_name: shike-redis
    restart: always
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - shike-redis-data:/data
    networks:
      - shike-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  # ==================== Backend App (挂载制品运行，无源码) ====================
  shike-app:
    image: eclipse-temurin:17-jre-alpine
    container_name: shike-app
    restart: always
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:mysql://shike-mysql:3306/db_shike?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_PASSWORD=Nizouba12138.
      - SPRING_DATA_REDIS_HOST=shike-redis
      - TZ=Asia/Shanghai
      - LANG=C.UTF-8
      - LC_ALL=C.UTF-8
      - JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.country=CN
      - JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=zh -Duser.country=CN -Djdk.httpclient.allowRestrictedHeaders=connection
    volumes:
      - ./app.jar:/app/app.jar
      - ./uploads:/app/uploads
    entrypoint: ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
    depends_on:
      shike-mysql:
        condition: service_healthy
      shike-redis:
        condition: service_healthy
    networks:
      - shike-network

volumes:
  shike-mysql-data:
    driver: local
  shike-redis-data:
    driver: local

networks:
  shike-network:
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: "1450"
"""
    with sftp.file(f"{REMOTE_APP_DIR}/docker-compose.yml", 'w') as f:
        f.write(docker_compose_content)
        
    sftp.close()
    ssh.close()
    print("✅ 远程 docker-compose.yml 配置同步成功！")

def restart_and_verify():
    print("=" * 60)
    print(" [Step 3/5] 远程极速热重启 shike-app 容器...")
    print("=" * 60)
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(SERVER_IP, port=SERVER_PORT, username=SERVER_USER, password=SERVER_PASS)
    
    # 强制重新创建 shike-app 容器以应用最新 docker-compose 配置
    cmd = f"cd {REMOTE_APP_DIR} && docker compose up -d --force-recreate shike-app"
    stdin, stdout, stderr = ssh.exec_command(cmd)
    print(stdout.read().decode('utf-8', errors='replace'))
    
    print("=" * 60)
    print(" [Step 4/5] 生产服务健康检查与接口探活...")
    print("=" * 60)
    
    is_healthy = False
    for i in range(15):
        time.sleep(2)
        try:
            r = requests.get("https://shike.store/api/v1/config/features?env=prod", timeout=5)
            if r.status_code == 200 and r.json().get('code') == 200:
                print(f"  -> 健康检查通过！(耗时 {(i+1)*2} 秒) 响应正常: {r.json().get('data')}")
                is_healthy = True
                break
        except Exception as e:
            print(f"  -> 等待服务启动中... ({i+1}/15)")
            
    if not is_healthy:
        print("❌ 健康检查超时或异常，正在抓取容器日志...")
        stdin, stdout, stderr = ssh.exec_command("docker logs --tail 40 shike-app")
        print(stdout.read().decode('utf-8', errors='replace'))
        ssh.close()
        sys.exit(1)
        
    print("✅ 生产服务基于 JRE 容器挂载 app.jar 运行 100% 成功！")
    
    print("=" * 60)
    print(" [Step 5/5] 安全清退生产服务器源码目录...")
    print("=" * 60)
    cleanup_cmd = f"rm -rf {REMOTE_APP_DIR}/shike-backend"
    stdin, stdout, stderr = ssh.exec_command(cleanup_cmd)
    stdout.read()
    
    # 查看当前目录文件
    stdin, stdout, stderr = ssh.exec_command(f"ls -lh {REMOTE_APP_DIR}")
    print("当前服务器生产根目录文件清单:")
    print(stdout.read().decode('utf-8', errors='replace'))
    
    ssh.close()
    print("=" * 60)
    print("🎉 恭喜！企业级轻量化制品部署已全部顺利完成！")
    print("=" * 60)

if __name__ == "__main__":
    build_local_jar()
    upload_jar_and_config()
    restart_and_verify()
