import glob
import subprocess

repo_jars = glob.glob('D:/maven/local/repo/**/*.jar', recursive=True)
cp = ';'.join(repo_jars) + ';d:/heming/shike/shike-backend/target/classes'

with open('d:/heming/shike/scratch/javac_options.txt', 'w', encoding='utf-8') as f:
    f.write('-encoding\nUTF-8\n')
    f.write('-cp\n')
    f.write(cp + '\n')
    f.write('-d\n')
    f.write('d:/heming/shike/shike-backend/target/classes\n')
    f.write('d:/heming/shike/shike-backend/src/main/java/com/shike/service/impl/TeamServiceImpl.java\n')

cmd = ['D:/Program Files/Java/jdk17/bin/javac.exe', '@d:/heming/shike/scratch/javac_options.txt']
res = subprocess.run(cmd, capture_output=True, text=True)
print('Returncode:', res.returncode)
if res.stderr:
    print('Stderr:', res.stderr[:500])
else:
    print('Compilation successful!')
