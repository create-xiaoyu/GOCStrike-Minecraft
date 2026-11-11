#version 150

uniform sampler2D Sampler0;
uniform vec2 Step;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 light = texture(Sampler0, texCoord).rgb * 0.227027;
    light += texture(Sampler0, texCoord + Step * 1.384615).rgb * 0.316216;
    light += texture(Sampler0, texCoord - Step * 1.384615).rgb * 0.316216;
    light += texture(Sampler0, texCoord + Step * 3.230769).rgb * 0.070270;
    light += texture(Sampler0, texCoord - Step * 3.230769).rgb * 0.070270;
    fragColor = vec4(light, 1.0);
}
