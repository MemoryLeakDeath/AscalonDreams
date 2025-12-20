#version 450
#extension GL_EXT_debug_printf : enable

const int MAX_TEXTURES = 100;
layout(location = 0) in vec2 inTextureCoords;
layout(location = 0) out vec4 outFragColor;

struct Material {
    vec4 diffuseColor;
    uint hasTexture;
    uint textureIndex;
    uint padding[2];
};

layout(set = 1, binding = 0) readonly buffer MaterialUniform {
    Material materials[];
} materialUniform;

layout(set = 2, binding = 0) uniform sampler2D textSampler[MAX_TEXTURES];

layout(push_constant) uniform pc {
    layout(offset = 64) uint materialIndex;
} push_constants;

void main() {
    Material material = materialUniform.materials[push_constants.materialIndex];
    if(material.hasTexture == 1) {
        outFragColor = texture(textSampler[material.textureIndex], inTextureCoords);
        debugPrintfEXT("Texture index: %d", material.textureIndex);
    } else {
        outFragColor = material.diffuseColor;
        debugPrintfEXT("Using diffuseColor");
    }
}