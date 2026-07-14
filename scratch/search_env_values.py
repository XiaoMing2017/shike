import os

for k, v in os.environ.items():
    if 's0un' in v or 'xcRF' in v or '7544108' in v:
        print(f"Found in ENV: {k} = {v}")
