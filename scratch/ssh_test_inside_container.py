import paramiko

ip = "117.72.61.18"
user = "root"
pwd = "Nizouba12138."

java_code = """
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DietTest {
    public static void main(String[] args) {
        String url = "https://api.xiaomimimo.com/v1/chat/completions";
        String apiKey = "sk-cz4l6x44t4mrbwdlg7yj7k4n5j9cld7nf35r30hhgtdvr9if";
        String payload = "{\\"model\\":\\"mimo-v2-pro\\",\\"messages\\":[{\\"role\\":\\"user\\",\\"content\\":\\"你是一个专业的膳食营养估算模型。用户通过图片识别出吃了一餐：【韭菜】。请估算这餐分量并返回JSON数组。\\"}],\\"temperature\\":0.2}";

        try {
            System.out.println("=== Testing with HTTP/1.1 ===");
            testWithClient(HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build(), url, apiKey, payload);

            System.out.println("\\n=== Testing with default HTTP/2 ===");
            testWithClient(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build(), url, apiKey, payload);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testWithClient(HttpClient client, String url, String apiKey, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long elapsed = System.currentTimeMillis() - start;

            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Time taken: " + elapsed + " ms");
            System.out.println("Response: " + response.body().substring(0, Math.min(200, response.body().length())) + "...");
        } catch (Exception e) {
            System.out.println("Failed: " + e);
            e.printStackTrace();
        }
    }
}
"""

def main():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(ip, username=user, password=pwd, timeout=10)
        sftp = ssh.open_sftp()
        
        # Write to remote /tmp
        with sftp.file('/tmp/DietTest.java', 'w') as f:
            f.write(java_code)
        sftp.close()
        
        # Copy to container, compile and run
        commands = [
            "docker run --rm -v /tmp:/tmp eclipse-temurin:17-jdk-alpine javac /tmp/DietTest.java",
            "docker cp /tmp/DietTest.class shike-app:/tmp/DietTest.class",
            "docker exec shike-app java -cp /tmp DietTest"
        ]
        
        for cmd in commands:
            print(f"Running command: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            out = stdout.read().decode('utf-8', errors='replace')
            err = stderr.read().decode('utf-8', errors='replace')
            if out:
                print("[STDOUT]\n", out)
            if err:
                print("[STDERR]\n", err)
                
    except Exception as e:
        print(f"Error: {e}")
    finally:
        ssh.close()

if __name__ == "__main__":
    main()
