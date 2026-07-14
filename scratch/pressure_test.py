import urllib.request
import json
import time
import threading
import sys
import uuid

# Default values
REQUEST_TYPE = 'GET'
CONCURRENCY = 10
REQUESTS_PER_THREAD = 20
BASE_URL = "http://localhost:8081/api/v1"

if len(sys.argv) > 1:
    REQUEST_TYPE = sys.argv[1].upper()
if len(sys.argv) > 2:
    CONCURRENCY = int(sys.argv[2])
if len(sys.argv) > 3:
    REQUESTS_PER_THREAD = int(sys.argv[3])
if len(sys.argv) > 4:
    BASE_URL = sys.argv[4]

def test_get_daily_records(user_id=2):
    url = f"{BASE_URL}/diet/daily?userId={user_id}&date=2026-06-24"
    req = urllib.request.Request(url)
    req.add_header('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36')
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.getcode() == 200
    except Exception:
        return False

def test_recognize_mock():
    boundary = uuid.uuid4().hex.encode('utf-8')
    dummy_img = b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15c4\x00\x00\x00\rIDATx\x9cc`\x00\x00\x00\x02\x00\x01H\xaf\xa4q\x00\x00\x00\x00IEND\xaeB`\x82'
    
    body = bytearray()
    
    # Add hint field
    body.extend(b'--' + boundary + b'\r\n')
    body.extend(b'Content-Disposition: form-data; name="hint"\r\n\r\n')
    body.extend(b'egg\r\n')
    
    # Add userId field
    body.extend(b'--' + boundary + b'\r\n')
    body.extend(b'Content-Disposition: form-data; name="userId"\r\n\r\n')
    body.extend(b'2\r\n')
    
    # Add file field
    body.extend(b'--' + boundary + b'\r\n')
    body.extend(b'Content-Disposition: form-data; name="file"; filename="test.png"\r\n')
    body.extend(b'Content-Type: image/png\r\n\r\n')
    body.extend(dummy_img)
    body.extend(b'\r\n')
    
    body.extend(b'--' + boundary + b'--\r\n')
    
    url = f"{BASE_URL}/diet/recognize"
    req = urllib.request.Request(url, data=body, method='POST')
    req.add_header('Content-Type', f'multipart/form-data; boundary={boundary.decode("utf-8")}')
    req.add_header('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36')
    
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.getcode() == 200
    except Exception:
        return False

class BenchmarkThread(threading.Thread):
    def __init__(self, request_fn, total_requests, thread_id, results):
        super().__init__()
        self.request_fn = request_fn
        self.total_requests = total_requests
        self.thread_id = thread_id
        self.results = results
        
    def run(self):
        success_count = 0
        fail_count = 0
        latencies = []
        
        for _ in range(self.total_requests):
            start_time = time.time()
            success = self.request_fn()
            latency = time.time() - start_time
            
            if success:
                success_count += 1
                latencies.append(latency)
            else:
                fail_count += 1
                
        self.results[self.thread_id] = {
            'success': success_count,
            'fail': fail_count,
            'latencies': latencies
        }

def run_benchmark(request_type, concurrency, requests_per_thread):
    if request_type == 'GET':
        request_fn = test_get_daily_records
        desc = "GET (Query Daily Records - Database & Cache Read)"
    elif request_type == 'POST_AI':
        request_fn = test_recognize_mock
        desc = "POST (AI Recognition - File Upload & Image Process)"
    else:
        print("Invalid request type")
        return
        
    print("="*60)
    print(f" Starting Stress Test for: {desc}")
    print(f" Concurrency (Threads): {concurrency}")
    print(f" Requests per Thread:   {requests_per_thread}")
    print(f" Total Requests:        {concurrency * requests_per_thread}")
    print(f" Target Server Base:    {BASE_URL}")
    print("="*60)
    
    results = {}
    threads = []
    
    start_wall = time.time()
    
    for i in range(concurrency):
        t = BenchmarkThread(request_fn, requests_per_thread, i, results)
        threads.append(t)
        t.start()
        
    for t in threads:
        t.join()
        
    end_wall = time.time()
    total_time = end_wall - start_wall
    
    # Aggregate results
    total_success = sum(r['success'] for r in results.values())
    total_fail = sum(r['fail'] for r in results.values())
    all_latencies = []
    for r in results.values():
        all_latencies.extend(r['latencies'])
        
    avg_latency = sum(all_latencies) / len(all_latencies) if all_latencies else 0
    qps = (total_success + total_fail) / total_time if total_time > 0 else 0
    
    print("\n" + "="*40)
    print(" Benchmark Report ")
    print("="*40)
    print(f"Total Time Taken:     {total_time:.2f} seconds")
    print(f"Successful Requests:  {total_success}")
    print(f"Failed Requests:      {total_fail}")
    print(f"Success Rate:         {total_success / (total_success + total_fail) * 100:.2f}%" if (total_success + total_fail) > 0 else "0.00%")
    print(f"Average Latency:      {avg_latency:.4f} seconds")
    print(f"Throughput (QPS):     {qps:.2f} req/sec")
    print("="*40)

if __name__ == "__main__":
    run_benchmark(request_type=REQUEST_TYPE, concurrency=CONCURRENCY, requests_per_thread=REQUESTS_PER_THREAD)
