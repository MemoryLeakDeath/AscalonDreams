#version 450
layout(location = 0) in vec2 inTextureCoordinates;
layout(location = 0) out vec4 outFragmentColor;
layout(set = 0, binding = 0) uniform sampler2D albedoSampler;

void main() {
    vec3 albedo = texture(albedoSampler, inTextureCoordinates).rgb;
    outFragmentColor = vec4(albedo, 1.0);
}