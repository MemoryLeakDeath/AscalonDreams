#version 450

layout(location = 0) out vec2 outTextureCoordinates;

void main() {
    outTextureCoordinates = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(outTextureCoordinates.x * 2.0f - 1.0f, outTextureCoordinates.y * -2.0f + 1.0f, 0.0f, 1.0f);
}