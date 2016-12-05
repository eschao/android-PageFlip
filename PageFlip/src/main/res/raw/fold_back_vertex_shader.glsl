precision mediump float;
uniform mat4 u_MVPMatrix;
uniform float u_texXOffset;
attribute vec4 a_vexPosition;
attribute vec2 a_texCoord;
varying vec2 v_texCoord;
varying float v_shadowX;

void main() {
    v_texCoord = vec2(abs(a_texCoord.x - u_texXOffset), a_texCoord.y);
    v_shadowX = clamp(abs(a_vexPosition.w), 0.01, 1.0);
    vec4 vertex = vec4(a_vexPosition.xyz, 1.0);
    gl_Position = u_MVPMatrix * vertex;
}