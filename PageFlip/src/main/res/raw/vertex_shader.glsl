/**
    Uniform variable is passed from outer application to shader
    Function glUniform** () can assign value to uniform variable
    shader can only read it but can't modify it
    uniform variable noramlly is used to represent:
    Tranform Matrix, Texture, Lighting Params and colors
    Examples:
        uniform mat4 viewProjectMatrix; // Project+View Matrix
        uniform mat4 viewMatrix;        // View Matrix
        uniform vec3 lightPosition;     // Light position

    Attribute variable is only used in vertex shader.
    It can't be used in fragment shader
    Noramlly, using glBindAttribLocation() to bind position with
    every attribute variable in application first, and then use
    glVertexAttribPointer() to assign value
    Examples:
        uniform mat4 u_matViewProjection;
        attribute vec4 a_position;
        attribute vec2 a_texCoord0;
        variying vec2 v_texCoord;
        void main() {
            gl_Position = u_matViewProjection * a_position;
            v_texCoord = a_texCoord0;

    Varying variable is used to pass data between vertex and fragment shader
    Generally, vertex shader modify value of varying variable, and
    fragment shader use its value. So we should keep the same declaration of it
    between vertex shader and frgament shader.
    Application can't use this variable.
    Examples:
        // Vertex shader
        uniform mat4 u_matViewProjection;
        attribute vec4 a_position;
        attribute vec2 a_texCoord0;
        varying vec2 v_texCoord;
        void main() {
            gl_Position = u_matViewProjection * a_position;
            v_texCoord = a_texCoord0;
        }

        // Framgment shader
        precision mediump float;
        varying vec2 v_texCoord;
        uniform sampler2D s_baseMap;
        uniform sampler2D s_lightMap;
        void main() {
            vec4 baseColor;
            vec4 lightColor;
            baseColor = texture2D(s_baseMap, v_texCoord);
            lightColor = texture2D(s_lightMap, v_texCoord);
            gl_FragColor = baseColor * (lightColor + 0.25);
        }
*/
uniform mat4 u_MVPMatrix;
attribute vec4 a_vexPosition;
attribute vec2 a_texCoord;
varying vec2 v_texCoord;

void main() {
    gl_Position = u_MVPMatrix * a_vexPosition;
    v_texCoord = a_texCoord;
}