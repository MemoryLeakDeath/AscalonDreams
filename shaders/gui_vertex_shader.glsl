#version 450

layout (location = 0) in vec2 inPosition;
layout (location = 1) in vec2 inTextureCoordinates;
layout (location = 2) in vec4 inColor;

layout (push_constant) uniform PushConstants {
    vec2 scale;
} pushConstants;

layout (location = 0) out vec2 outTextureCoordinates;
layout (location = 1) out vec4 outColor;

out gl_PerVertex {
    vec4 gl_Position;
};

void main() {
    outTextureCoordinates = inTextureCoordinates;
    outColor = inColor;
    gl_Position = vec4(inPosition * pushConstants.scale + vec2(-1.0, 1.0), 0.0, 1.0);
}