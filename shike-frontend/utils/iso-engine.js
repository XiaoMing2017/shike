/**
 * iso-engine.js — 轻量手写 3D 等轴测渲染引擎
 * 专为微信小程序 Canvas WebGL 设计，零外部依赖
 */

// ======================= Vec3 =======================
class Vec3 {
  constructor(x, y, z) { this.x = x || 0; this.y = y || 0; this.z = z || 0; }
  set(x, y, z) { this.x = x; this.y = y; this.z = z; return this; }
  clone() { return new Vec3(this.x, this.y, this.z); }
  add(v) { return new Vec3(this.x + v.x, this.y + v.y, this.z + v.z); }
  sub(v) { return new Vec3(this.x - v.x, this.y - v.y, this.z - v.z); }
  scale(s) { return new Vec3(this.x * s, this.y * s, this.z * s); }
  len() { return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z); }
  normalize() { var l = this.len(); return l > 1e-6 ? this.scale(1 / l) : new Vec3(); }
  dot(v) { return this.x * v.x + this.y * v.y + this.z * v.z; }
  cross(v) {
    return new Vec3(
      this.y * v.z - this.z * v.y,
      this.z * v.x - this.x * v.z,
      this.x * v.y - this.y * v.x
    );
  }
  lerp(v, t) {
    return new Vec3(
      this.x + (v.x - this.x) * t,
      this.y + (v.y - this.y) * t,
      this.z + (v.z - this.z) * t
    );
  }
}

// ======================= Mat4 (column-major) =======================
class Mat4 {
  constructor() { this.m = new Float32Array(16); this.identity(); }

  identity() {
    var d = this.m; d.fill(0);
    d[0] = d[5] = d[10] = d[15] = 1;
    return this;
  }

  clone() { var r = new Mat4(); r.m.set(this.m); return r; }

  // C = A * B (column-major)
  static mul(a, b) {
    var r = new Mat4(), o = r.m, A = a.m, B = b.m;
    for (var c = 0; c < 4; c++) {
      for (var row = 0; row < 4; row++) {
        o[c * 4 + row] =
          A[row] * B[c * 4] +
          A[4 + row] * B[c * 4 + 1] +
          A[8 + row] * B[c * 4 + 2] +
          A[12 + row] * B[c * 4 + 3];
      }
    }
    return r;
  }

  translate(x, y, z) {
    var t = new Mat4();
    t.m[12] = x; t.m[13] = y; t.m[14] = z;
    return Mat4.mul(this, t);
  }

  scale(x, y, z) {
    var s = new Mat4();
    s.m[0] = x; s.m[5] = y; s.m[10] = z;
    return Mat4.mul(this, s);
  }

  rotateX(rad) {
    var c = Math.cos(rad), s = Math.sin(rad), r = new Mat4();
    r.m[5] = c; r.m[6] = s; r.m[9] = -s; r.m[10] = c;
    return Mat4.mul(this, r);
  }

  rotateY(rad) {
    var c = Math.cos(rad), s = Math.sin(rad), r = new Mat4();
    r.m[0] = c; r.m[2] = -s; r.m[8] = s; r.m[10] = c;
    return Mat4.mul(this, r);
  }

  rotateZ(rad) {
    var c = Math.cos(rad), s = Math.sin(rad), r = new Mat4();
    r.m[0] = c; r.m[1] = s; r.m[4] = -s; r.m[5] = c;
    return Mat4.mul(this, r);
  }

  static ortho(l, r, b, t, n, f) {
    var m = new Mat4(); var d = m.m;
    d[0] = 2 / (r - l);
    d[5] = 2 / (t - b);
    d[10] = -2 / (f - n);
    d[12] = -(r + l) / (r - l);
    d[13] = -(t + b) / (t - b);
    d[14] = -(f + n) / (f - n);
    return m;
  }

  static lookAt(eye, center, up) {
    var f = center.sub(eye).normalize();
    var s = f.cross(up).normalize();
    var u = s.cross(f);
    var m = new Mat4(); var d = m.m;
    d[0] = s.x; d[1] = u.x; d[2] = -f.x;
    d[4] = s.y; d[5] = u.y; d[6] = -f.y;
    d[8] = s.z; d[9] = u.z; d[10] = -f.z;
    d[12] = -s.dot(eye);
    d[13] = -u.dot(eye);
    d[14] = f.dot(eye);
    return m;
  }
}

// ======================= Geometry Generators =======================
var Geometry = {
  /**
   * createBox(w, h, d) — 6 面盒体，每面 4 顶点 + 法线
   * 返回 { positions: Float32Array, normals: Float32Array, indices: Uint16Array }
   */
  createBox: function (w, h, d) {
    var hw = w / 2, hh = h / 2, hd = d / 2;
    // 6 faces: +X, -X, +Y, -Y, +Z, -Z
    var faceData = [
      // positions (4 verts)                    normal
      [[hw,-hh,hd],[hw,hh,hd],[hw,hh,-hd],[hw,-hh,-hd],   [1,0,0]],   // +X
      [[-hw,-hh,-hd],[-hw,hh,-hd],[-hw,hh,hd],[-hw,-hh,hd], [-1,0,0]], // -X
      [[-hw,hh,hd],[hw,hh,hd],[hw,hh,-hd],[-hw,hh,-hd],     [0,1,0]],  // +Y (top, wrong winding fixed below)
      [[-hw,-hh,-hd],[hw,-hh,-hd],[hw,-hh,hd],[-hw,-hh,hd],  [0,-1,0]],// -Y
      [[-hw,-hh,hd],[hw,-hh,hd],[hw,hh,hd],[-hw,hh,hd],     [0,0,1]],  // +Z
      [[hw,-hh,-hd],[-hw,-hh,-hd],[-hw,hh,-hd],[hw,hh,-hd],  [0,0,-1]] // -Z
    ];
    // Fix +Y winding
    faceData[2] = [[-hw,hh,-hd],[hw,hh,-hd],[hw,hh,hd],[-hw,hh,hd], [0,1,0]];
    
    var pos = [], nrm = [], idx = [];
    for (var fi = 0; fi < 6; fi++) {
      var fd = faceData[fi], n = fd[4], base = fi * 4;
      for (var vi = 0; vi < 4; vi++) {
        pos.push(fd[vi][0], fd[vi][1], fd[vi][2]);
        nrm.push(n[0], n[1], n[2]);
      }
      idx.push(base, base + 1, base + 2, base, base + 2, base + 3);
    }
    return {
      positions: new Float32Array(pos),
      normals: new Float32Array(nrm),
      indices: new Uint16Array(idx)
    };
  },

  /**
   * createSphere(radius, wSeg, hSeg)
   */
  createSphere: function (radius, wSeg, hSeg) {
    wSeg = wSeg || 12; hSeg = hSeg || 8;
    var pos = [], nrm = [], idx = [];
    for (var y = 0; y <= hSeg; y++) {
      var phi = Math.PI * y / hSeg;
      var sp = Math.sin(phi), cp = Math.cos(phi);
      for (var x = 0; x <= wSeg; x++) {
        var theta = 2 * Math.PI * x / wSeg;
        var st = Math.sin(theta), ct = Math.cos(theta);
        var nx = st * sp, ny = cp, nz = ct * sp;
        pos.push(radius * nx, radius * ny, radius * nz);
        nrm.push(nx, ny, nz);
      }
    }
    for (var y = 0; y < hSeg; y++) {
      for (var x = 0; x < wSeg; x++) {
        var a = y * (wSeg + 1) + x;
        var b = a + wSeg + 1;
        idx.push(a, b, a + 1, b, b + 1, a + 1);
      }
    }
    return {
      positions: new Float32Array(pos),
      normals: new Float32Array(nrm),
      indices: new Uint16Array(idx)
    };
  },

  /**
   * createCylinder(rTop, rBot, height, seg)
   */
  createCylinder: function (rTop, rBot, height, seg) {
    seg = seg || 10;
    var hh = height / 2;
    var pos = [], nrm = [], idx = [];
    // Side
    for (var i = 0; i <= seg; i++) {
      var a = 2 * Math.PI * i / seg;
      var ca = Math.cos(a), sa = Math.sin(a);
      var slope = (rBot - rTop) / height;
      var ny = slope, nLen = Math.sqrt(1 + slope * slope);
      pos.push(rTop * ca, hh, rTop * sa);
      nrm.push(ca / nLen, ny / nLen, sa / nLen);
      pos.push(rBot * ca, -hh, rBot * sa);
      nrm.push(ca / nLen, ny / nLen, sa / nLen);
    }
    for (var i = 0; i < seg; i++) {
      var a = i * 2, b = a + 1, c = a + 2, d = a + 3;
      idx.push(a, b, c, b, d, c);
    }
    // Top cap
    var base = pos.length / 3;
    pos.push(0, hh, 0); nrm.push(0, 1, 0);
    for (var i = 0; i <= seg; i++) {
      var a = 2 * Math.PI * i / seg;
      pos.push(rTop * Math.cos(a), hh, rTop * Math.sin(a));
      nrm.push(0, 1, 0);
    }
    for (var i = 0; i < seg; i++) {
      idx.push(base, base + 1 + i, base + 2 + i);
    }
    // Bottom cap
    base = pos.length / 3;
    pos.push(0, -hh, 0); nrm.push(0, -1, 0);
    for (var i = 0; i <= seg; i++) {
      var a = 2 * Math.PI * i / seg;
      pos.push(rBot * Math.cos(a), -hh, rBot * Math.sin(a));
      nrm.push(0, -1, 0);
    }
    for (var i = 0; i < seg; i++) {
      idx.push(base, base + 2 + i, base + 1 + i);
    }
    return {
      positions: new Float32Array(pos),
      normals: new Float32Array(nrm),
      indices: new Uint16Array(idx)
    };
  }
};

// ======================= SceneNode =======================
class SceneNode {
  constructor(name) {
    this.name = name || '';
    this.position = new Vec3();
    this.rotation = new Vec3(); // euler radians (Y, X, Z order)
    this.scaleVec = new Vec3(1, 1, 1);
    this.children = [];
    this.mesh = null;   // { glBuffers, indexCount, color }
    this.visible = true;
    this.alpha = 1.0;
  }

  add(child) { this.children.push(child); return this; }

  getLocalMatrix() {
    var m = new Mat4();
    m = m.translate(this.position.x, this.position.y, this.position.z);
    m = m.rotateY(this.rotation.y);
    m = m.rotateX(this.rotation.x);
    m = m.rotateZ(this.rotation.z);
    m = m.scale(this.scaleVec.x, this.scaleVec.y, this.scaleVec.z);
    return m;
  }

  traverse(callback, parentMatrix) {
    var world = parentMatrix ? Mat4.mul(parentMatrix, this.getLocalMatrix()) : this.getLocalMatrix();
    if (this.visible) {
      callback(this, world);
    }
    for (var i = 0; i < this.children.length; i++) {
      this.children[i].traverse(callback, world);
    }
  }
}

// ======================= IsoCamera =======================
class IsoCamera {
  constructor() {
    this.size = 3.8;
    this.aspect = 1;
    this.near = 0.1;
    this.far = 100;
    this.pitch = 35.264 * Math.PI / 180;
    this.yaw = Math.PI / 4;
    this.panX = 0;
    this.panZ = 0;
    this.targetPanX = 0;
    this.targetPanZ = 0;
    this.damping = 0.08;
    this.maxPan = 1.2;
  }

  getProjectionMatrix() {
    var s = this.size, a = this.aspect;
    return Mat4.ortho(-s * a, s * a, -s, s, this.near, this.far);
  }

  getViewMatrix() {
    var dist = 30;
    var cp = Math.cos(this.pitch), sp = Math.sin(this.pitch);
    var cy = Math.cos(this.yaw), sy = Math.sin(this.yaw);
    var eye = new Vec3(
      sy * cp * dist + this.panX,
      sp * dist + 1.0,
      cy * cp * dist + this.panZ
    );
    var center = new Vec3(this.panX, 1.0, this.panZ);
    return Mat4.lookAt(eye, center, new Vec3(0, 1, 0));
  }

  update() {
    this.panX += (this.targetPanX - this.panX) * this.damping;
    this.panZ += (this.targetPanZ - this.panZ) * this.damping;
  }

  pan(dx, dz) {
    this.targetPanX = Math.max(-this.maxPan, Math.min(this.maxPan, this.targetPanX + dx));
    this.targetPanZ = Math.max(-this.maxPan, Math.min(this.maxPan, this.targetPanZ + dz));
  }

  release() {
    this.targetPanX = 0;
    this.targetPanZ = 0;
  }
}

// ======================= WebGLRenderer =======================
var VERT_SRC = [
  'attribute vec3 a_position;',
  'attribute vec3 a_normal;',
  'uniform mat4 u_proj;',
  'uniform mat4 u_view;',
  'uniform mat4 u_model;',
  'varying vec3 v_normal;',
  'void main(){',
  '  v_normal = mat3(u_model) * a_normal;',
  '  gl_Position = u_proj * u_view * u_model * vec4(a_position, 1.0);',
  '}'
].join('\n');

var FRAG_SRC = [
  'precision mediump float;',
  'varying vec3 v_normal;',
  'uniform vec3 u_lightDir;',
  'uniform vec3 u_lightColor;',
  'uniform vec3 u_ambient;',
  'uniform vec3 u_color;',
  'uniform float u_alpha;',
  'void main(){',
  '  vec3 n = normalize(v_normal);',
  '  float NdL = max(dot(n, normalize(u_lightDir)), 0.0);',
  '  float toon;',
  '  if(NdL > 0.55) toon = 1.0;',
  '  else if(NdL > 0.25) toon = 0.72;',
  '  else if(NdL > 0.05) toon = 0.52;',
  '  else toon = 0.38;',
  '  vec3 diff = u_color * u_lightColor * toon;',
  '  vec3 amb  = u_color * u_ambient;',
  '  gl_FragColor = vec4(amb + diff, u_alpha);',
  '}'
].join('\n');

class WebGLRenderer {
  constructor() {
    this.gl = null;
    this.program = null;
    this.locs = {};
    this.meshCache = [];
    this.lightDir = [0.5, 0.8, 0.3];
    this.lightColor = [1.0, 0.95, 0.9];
    this.ambientColor = [0.35, 0.35, 0.4];
  }

  init(canvas) {
    var gl = canvas.getContext('webgl');
    if (!gl) { console.error('WebGL not supported'); return false; }
    this.gl = gl;
    this.canvas = canvas;

    // Compile shaders
    var vs = this._compileShader(gl.VERTEX_SHADER, VERT_SRC);
    var fs = this._compileShader(gl.FRAGMENT_SHADER, FRAG_SRC);
    var prog = gl.createProgram();
    gl.attachShader(prog, vs);
    gl.attachShader(prog, fs);
    gl.linkProgram(prog);
    if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
      console.error('Program link error:', gl.getProgramInfoLog(prog));
      return false;
    }
    this.program = prog;

    // Cache uniform/attribute locations
    this.locs = {
      aPos: gl.getAttribLocation(prog, 'a_position'),
      aNorm: gl.getAttribLocation(prog, 'a_normal'),
      uProj: gl.getUniformLocation(prog, 'u_proj'),
      uView: gl.getUniformLocation(prog, 'u_view'),
      uModel: gl.getUniformLocation(prog, 'u_model'),
      uLightDir: gl.getUniformLocation(prog, 'u_lightDir'),
      uLightColor: gl.getUniformLocation(prog, 'u_lightColor'),
      uAmbient: gl.getUniformLocation(prog, 'u_ambient'),
      uColor: gl.getUniformLocation(prog, 'u_color'),
      uAlpha: gl.getUniformLocation(prog, 'u_alpha')
    };

    // GL state
    gl.enable(gl.DEPTH_TEST);
    gl.depthFunc(gl.LEQUAL);
    gl.enable(gl.CULL_FACE);
    gl.cullFace(gl.BACK);
    gl.clearColor(0.96, 0.98, 0.97, 1.0);

    return true;
  }

  _compileShader(type, src) {
    var gl = this.gl;
    var s = gl.createShader(type);
    gl.shaderSource(s, src);
    gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
      console.error('Shader compile error:', gl.getShaderInfoLog(s));
    }
    return s;
  }

  /**
   * Upload geometry to GPU, return mesh handle
   */
  createMesh(geom, color, alpha) {
    var gl = this.gl;
    var posBuf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, posBuf);
    gl.bufferData(gl.ARRAY_BUFFER, geom.positions, gl.STATIC_DRAW);

    var normBuf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, normBuf);
    gl.bufferData(gl.ARRAY_BUFFER, geom.normals, gl.STATIC_DRAW);

    var idxBuf = gl.createBuffer();
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, idxBuf);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, geom.indices, gl.STATIC_DRAW);

    var mesh = {
      posBuf: posBuf,
      normBuf: normBuf,
      idxBuf: idxBuf,
      indexCount: geom.indices.length,
      color: color || [0.8, 0.8, 0.8],
      alpha: alpha != null ? alpha : 1.0
    };
    this.meshCache.push(mesh);
    return mesh;
  }

  /**
   * Render entire scene
   */
  render(scene, camera) {
    var gl = this.gl;
    gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    gl.useProgram(this.program);

    // Set camera matrices
    var proj = camera.getProjectionMatrix();
    var view = camera.getViewMatrix();
    gl.uniformMatrix4fv(this.locs.uProj, false, proj.m);
    gl.uniformMatrix4fv(this.locs.uView, false, view.m);

    // Set lighting
    gl.uniform3fv(this.locs.uLightDir, this.lightDir);
    gl.uniform3fv(this.locs.uLightColor, this.lightColor);
    gl.uniform3fv(this.locs.uAmbient, this.ambientColor);

    // Collect opaque & transparent nodes
    var opaqueList = [];
    var translucentList = [];

    scene.traverse(function (node, worldMatrix) {
      if (!node.mesh) return;
      var item = { node: node, matrix: worldMatrix };
      if (node.mesh.alpha < 1.0 || node.alpha < 1.0) {
        translucentList.push(item);
      } else {
        opaqueList.push(item);
      }
    }, null);

    // Draw opaque first
    gl.disable(gl.BLEND);
    gl.depthMask(true);
    for (var i = 0; i < opaqueList.length; i++) {
      this._drawNode(opaqueList[i].node, opaqueList[i].matrix);
    }

    // Draw translucent (shadows etc.)
    if (translucentList.length > 0) {
      gl.enable(gl.BLEND);
      gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
      gl.depthMask(false);
      for (var i = 0; i < translucentList.length; i++) {
        this._drawNode(translucentList[i].node, translucentList[i].matrix);
      }
      gl.depthMask(true);
      gl.disable(gl.BLEND);
    }
  }

  _drawNode(node, worldMatrix) {
    var gl = this.gl, mesh = node.mesh, locs = this.locs;
    gl.uniformMatrix4fv(locs.uModel, false, worldMatrix.m);
    gl.uniform3fv(locs.uColor, mesh.color);
    gl.uniform1f(locs.uAlpha, mesh.alpha * node.alpha);

    gl.bindBuffer(gl.ARRAY_BUFFER, mesh.posBuf);
    gl.enableVertexAttribArray(locs.aPos);
    gl.vertexAttribPointer(locs.aPos, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, mesh.normBuf);
    gl.enableVertexAttribArray(locs.aNorm);
    gl.vertexAttribPointer(locs.aNorm, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, mesh.idxBuf);
    gl.drawElements(gl.TRIANGLES, mesh.indexCount, gl.UNSIGNED_SHORT, 0);
  }

  setLighting(mode) {
    if (mode === 'SUNSET') {
      this.lightDir = [0.3, 0.5, 0.2];
      this.lightColor = [1.0, 0.7, 0.5];
      this.ambientColor = [0.4, 0.3, 0.3];
      var gl = this.gl;
      gl.clearColor(0.98, 0.92, 0.88, 1.0);
    } else if (mode === 'NIGHT') {
      this.lightDir = [0.2, 0.6, 0.3];
      this.lightColor = [0.35, 0.38, 0.55];
      this.ambientColor = [0.12, 0.12, 0.22];
      var gl = this.gl;
      gl.clearColor(0.12, 0.14, 0.22, 1.0);
    } else {
      this.lightDir = [0.5, 0.8, 0.3];
      this.lightColor = [1.0, 0.95, 0.9];
      this.ambientColor = [0.35, 0.35, 0.4];
      var gl = this.gl;
      gl.clearColor(0.96, 0.98, 0.97, 1.0);
    }
  }

  destroy() {
    var gl = this.gl;
    if (!gl) return;
    for (var i = 0; i < this.meshCache.length; i++) {
      var m = this.meshCache[i];
      gl.deleteBuffer(m.posBuf);
      gl.deleteBuffer(m.normBuf);
      gl.deleteBuffer(m.idxBuf);
    }
    if (this.program) gl.deleteProgram(this.program);
    this.meshCache = [];
    this.gl = null;
  }
}

module.exports = { Vec3: Vec3, Mat4: Mat4, Geometry: Geometry, SceneNode: SceneNode, IsoCamera: IsoCamera, WebGLRenderer: WebGLRenderer };
