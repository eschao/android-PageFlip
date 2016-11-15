uniform sampler2D u_texture;
uniform sampler2D u_shadow;
uniform vec4 u_maskColor;

varying vec2 v_texCoord;
varying float v_shadowX;

void main() {
    vec4 texture = texture2D(u_texture, v_texCoord);
    vec2 shadowCoord = vec2(v_shadowX, 0);
    vec4 shadow = texture2D(u_shadow, shadowCoord);
    vec4 maskedTexture = vec4(mix(texture.rgb, u_maskColor.rgb, u_maskColor.a), 1.0);
    gl_FragColor = vec4(maskedTexture.rgb * (1.0 - shadow.a) + shadow.rgb, maskedTexture.a);
    //gl_FragColor = vec4(texture.rgb + shadow.rgb * 0, 1.0);//texture.a);
}
