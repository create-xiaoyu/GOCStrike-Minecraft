#version 120

uniform sampler2D Sampler0;
uniform float Strength;
varying vec2 texCoord;

void main() {
    gl_FragColor = vec4(clamp(texture2D(Sampler0, texCoord).rgb * Strength, 0.0, 1.0), 1.0);
}
