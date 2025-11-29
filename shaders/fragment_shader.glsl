#version 450

layout(location = 0) in vec2 inTextureCoords;
layout(location = 0) out vec4 outFragColor;

void main() {
    outFragColor = vec4(inTextureCoords.x, inTextureCoords.y, 0, 1);
}