#version 120
uniform sampler2D Sampler0;
uniform vec3 Tint;
uniform bool Textured;
varying vec4 color;
varying vec2 texCoord;
void main() {
    vec4 base = Textured ? texture2D(Sampler0, texCoord) * color : color;
    if (base.a <= 0.0) discard;
    gl_FragColor = vec4(base.rgb * Tint, base.a);
}
