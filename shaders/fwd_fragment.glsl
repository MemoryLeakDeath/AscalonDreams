#version 450

layout(location=0) in  vec2 texCoords;
layout(location=0) out vec4 uFragColor;

void main() {
    uFragColor = vec4(texCoords.x, texCoords.y, 0, 1);
}
