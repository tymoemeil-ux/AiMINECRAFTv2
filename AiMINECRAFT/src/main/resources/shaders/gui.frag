#version 330 core
// GUI: plaski kolor albo kolor * tekstura.
in vec2 vUV;
in vec4 vColor;

uniform sampler2D uTex;
uniform int uUseTex;

out vec4 FragColor;

void main() {
    vec4 col = vColor;
    if (uUseTex == 1) {
        vec4 tex = texture(uTex, vUV);
        if (tex.a < 0.05) {
            discard;
        }
        col *= tex;
    }
    FragColor = col;
}
