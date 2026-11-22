#version 120

uniform sampler2D Sampler0;
uniform vec2 Step;
varying vec2 texCoord;

void main() {
    vec3 light = texture2D(Sampler0, texCoord).rgb * 0.227027;
    light += texture2D(Sampler0, texCoord + Step * 1.384615).rgb * 0.316216;
    light += texture2D(Sampler0, texCoord - Step * 1.384615).rgb * 0.316216;
    light += texture2D(Sampler0, texCoord + Step * 3.230769).rgb * 0.070270;
    light += texture2D(Sampler0, texCoord - Step * 3.230769).rgb * 0.070270;
    gl_FragColor = vec4(light, 1.0);
}
