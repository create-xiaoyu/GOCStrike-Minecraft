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
    vec3 light = texture(Sampler0, texCoord).rgb * 0.227027;
    light += texture(Sampler0, texCoord + Step * 1.384615).rgb * 0.316216;
    light += texture(Sampler0, texCoord - Step * 1.384615).rgb * 0.316216;
    light += texture(Sampler0, texCoord + Step * 3.230769).rgb * 0.070270;
    light += texture(Sampler0, texCoord - Step * 3.230769).rgb * 0.070270;
    fragColor = vec4(light, 1.0);
}
