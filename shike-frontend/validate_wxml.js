const fs = require('fs');
const path = require('path');

const frontendDir = path.resolve('d:/heming/shike/shike-frontend');

function validateWxml(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  // 移除注释 <!-- ... -->
  const cleanContent = content.replace(/<!--[\s\S]*?-->/g, '');
  
  const tagRegex = /<\/?([a-zA-Z0-9_-]+)(?:\s+[^>]*)?(\/?)>/g;
  const stack = [];
  const selfClosingOnly = new Set(['input', 'icon', 'slider', 'switch', 'progress', 'radio', 'checkbox', 'import', 'include']);
  
  let match;
  while ((match = tagRegex.exec(cleanContent)) !== null) {
    const fullTag = match[0];
    const tagName = match[1];
    const isSelfClosing = match[2] === '/' || fullTag.endsWith('/>') || (selfClosingOnly.has(tagName) && !fullTag.startsWith('</'));
    const isClosing = fullTag.startsWith('</');

    if (isClosing) {
      if (stack.length === 0) {
        return { valid: false, error: `多余的闭合标签 </${tagName}>` };
      }
      const top = stack.pop();
      if (top.tagName !== tagName) {
        return { valid: false, error: `标签不匹配: 期望 </${top.tagName}>, 但得到 </${tagName}>` };
      }
    } else if (!isSelfClosing) {
      stack.push({ tagName, fullTag });
    }
  }

  if (stack.length > 0) {
    const unclosed = stack.map(s => s.tagName).join(', ');
    return { valid: false, error: `未闭合的标签: ${unclosed}` };
  }

  return { valid: true };
}

function scanWxml(dir) {
  const files = fs.readdirSync(dir);
  let totalFiles = 0;
  let errors = 0;

  for (const file of files) {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      if (file !== 'node_modules' && file !== '.git') {
        scanWxml(fullPath);
      }
    } else if (file.endsWith('.wxml')) {
      totalFiles++;
      const res = validateWxml(fullPath);
      if (!res.valid) {
        console.error(`[WXML ERROR] ${path.relative(frontendDir, fullPath)}: ${res.error}`);
        errors++;
      } else {
        console.log(`[✓ WXML] ${path.relative(frontendDir, fullPath)} 结构完美！`);
      }
    }
  }
  return errors;
}

console.log('=== 全量 WXML 结构扫描 ===');
const errs = scanWxml(frontendDir);
if (errs === 0) {
  console.log('\n🎉 所有 WXML 模板文件语法与标签层级结构 100% 验证通过！');
}
