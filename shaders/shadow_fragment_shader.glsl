#version 450

// keep in sync manually with java code
const int MAX_TEXTURES = 100;

layout(location = 0) in vec2 inTextureCoordinates;
layout(location = 1) in flat uint inMaterialIndex;

layout(location = 0) out vec2 outFragmentColor;

struct Material {
    vec4 diffuseColor;
    uint hasTexture;
    uint textureIndex;
    uint hasNormalMap;
    uint normalMapIndex;
    uint hasRoughMap;
    uint roughMapIndex;
    float roughnessFactor;
    float metallicFactor;
};

layout(set = 1, binding = 0) uniform sampler2D textureSampler[MAX_TEXTURES];
layout(set = 2, binding = 0) readonly buffer MaterialUniform {
    Material materials[];
} materialUniform;

void main() {
    Material material = materialUniform.materials[inMaterialIndex];
    vec4 albedo;
    if(material.hasTexture == 1) {
        albedo = texture(textureSampler[material.textureIndex], inTextureCoordinates);
    } else {
        albedo = material.diffuseColor;
    }
    if(albedo.a < 0.5) {
        discard;
    }

    float depth = gl_FragCoord.z;
    float moment1 = depth;
    float moment2 = depth * depth;

    // adjust moments to avoid light bleeding
    float dx = dFdx(depth);
    float dy = dFdy(depth);
    moment2 += 0.25 * (dx * dx + dy * dy);

    outFragmentColor = vec2(moment1, moment2);
}