#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;
layout(std140) uniform GocBloom {
    vec2 Step;
    float Strength;
};
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(clamp(texture(Sampler0, texCoord).rgb * Strength, 0.0, 1.0), 1.0);
}
