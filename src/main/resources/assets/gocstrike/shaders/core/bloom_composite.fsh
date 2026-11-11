#version 150

uniform sampler2D Sampler0;
uniform float Strength;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = vec4(clamp(texture(Sampler0, texCoord).rgb * Strength, 0.0, 1.0), 1.0);
}
