#version 450

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec2 inTextureCoords;

layout(location = 0) out vec2 outTextureCoords;

layout(set = 0, binding = 0) uniform ProjectionUniform {
    mat4 matrix;
} projectionUniform;

layout(push_constant) uniform pc {
    mat4 modelMatrix;
} push_constants;

void main() {
    gl_Position = projectionUniform.matrix * push_constants.modelMatrix * vec4(inPos, 1);
    outTextureCoords = inTextureCoords;
}