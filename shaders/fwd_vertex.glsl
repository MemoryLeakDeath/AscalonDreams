#version 450

layout(location=0) in vec3 entityPos;
layout(location=1) in vec2 entityTexCoords;

layout(location=0) out vec2 texCoords;

layout(push_constant) uniform matricies {
    mat4 projectionMatrix;
    mat4 modelMatrix;
} push_constants;

void main() {
    gl_Position = push_constants.projectionMatrix * push_constants.modelMatrix * vec4(entityPos, 1);
    texCoords = entityTexCoords;
}
