#version 330 core
// Woda: polprzezroczysta, lekko przyciemniona.
in vec2 vUV;
in float vShade;
in float vFogDepth;

uniform sampler2D uAtlas;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;
uniform float uBrightness;

out vec4 FragColor;

void main() {
    vec4 tex = texture(uAtlas, vUV);
    vec3 col = tex.rgb * vShade * uBrightness;
    float f = clamp((vFogDepth - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
    f = f * f * (3.0 - 2.0 * f);
    FragColor = vec4(mix(col, uFogColor, f), 0.72);
}
