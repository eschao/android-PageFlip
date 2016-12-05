precision mediump float;
uniform mat4 u_MVPMatrix;
uniform float u_vexZ;
attribute vec4 a_vexPosition;
varying vec4 v_texColor;

void main() {
    vec4 vexPos = vec4(a_vexPosition.xy, u_vexZ, 1.0);
    v_texColor = vec4(a_vexPosition.z, a_vexPosition.z, a_vexPosition.z, a_vexPosition.w);
    gl_Position = u_MVPMatrix * vexPos;
}