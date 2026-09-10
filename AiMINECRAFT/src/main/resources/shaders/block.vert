#version 330 core
// Wierzcholki blokow: pozycja (swiat) + UV + cien sciany.
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aUV;
layout (location = 2) in float aShade;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 vUV;
out float vShade;
out float vFogDepth;

void main() {
    vec4 viewPos = uView * vec4(aPos, 1.0);
    vFogDepth = -viewPos.z;
    vUV = aUV;
    vShade = aShade;
    gl_Position = uProjection * viewPos;
}
