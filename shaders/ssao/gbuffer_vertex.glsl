#version 450

layout(location = 0) in vec4 inPosition;
layout(location = 1) in vec4 inUV;
layout(location = 2) in vec3 inColor;
layout(location = 3) in vec3 inNormal;

layout(binding = 0) uniform UBO {
    mat4 projection;
    mat4 model;
    mat4 view;
} ubo;

layout(location = 0) out vec3 outNormal;
layout(location = 1) out vec3 outUV;
layout(location = 2) out vec3 outColor;
layout(location = 3) out vec3 outPosition;

void main() {
    gl_Position = ubo.projection * ubo.view * ubo.model * inPosition;
    outUV = inUV;

    // vertex position in view space
    outPosition = vec3(ubo.view * ubo.model * inPosition);

    // normal in view space
    mat3 normalMatrix = transpose(inverse(mat3(ubo.view * ubo.model)));
    outNormal = normalMatrix * inNormal;

    outColor = inColor;
}