#version 450

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec3 inTangent;
layout(location = 3) in vec3 inBitangent;
layout(location = 4) in vec2 inTextureCoords;

layout(location = 0) out vec4 outPos;
layout(location = 1) out vec3 outNormal;
layout(location = 2) out vec3 outTangent;
layout(location = 3) out vec3 outBitangent;
layout(location = 4) out vec2 outTextureCoords;

layout(set = 0, binding = 0) uniform ProjectionUniform {
    mat4 matrix;
} projectionUniform;
layout(set = 1, binding = 0) uniform ViewUniform {
    mat4 matrix;
} viewUniform;

layout(push_constant) uniform pc {
    mat4 modelMatrix;
} push_constants;

void main() {
    vec4 worldPos = push_constants.modelMatrix * vec4(inPos, 1);
    gl_Position = projectionUniform.matrix * viewUniform.matrix * worldPos;
    mat3 mNormal = transpose(inverse(mat3(push_constants.modelMatrix)));
    outPos = worldPos;
    outNormal = mNormal * normalize(inNormal);
    outTangent = mNormal * normalize(inTangent);
    outBitangent = mNormal * normalize(inBitangent);
    outTextureCoords = inTextureCoords;
}