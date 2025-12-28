#version 450

layout(location = 0) in vec2 inTextureCoordinates;

layout(location = 0) out vec4 outFragmentColor;

layout(set = 0, binding = 0) uniform sampler2D albedoSampler;
layout(set = 0, binding = 1) uniform sampler2D depthSampler;

void main() {
    outFragmentColor = vec4(texture(albedoSampler, inTextureCoordinates).rgb, 1.0);
}