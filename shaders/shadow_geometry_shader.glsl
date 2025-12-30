#version 450

// change this manually if SHADOW_MAP_CASCADE_COUNT changes
#define SHADOW_MAP_CASCADE_COUNT 3

layout(triangles, invocations = SHADOW_MAP_CASCADE_COUNT) in;
layout(triangle_strip, max_vertices = 3) out;

layout(location = 0) in vec2 inTextureCoordinates[];
layout(location = 1) in flat uint inMaterialIndex[];

layout(location = 0) out vec2 outTextureCoordinates;
layout(location = 1) out flat uint outMaterialIndex;

layout(set = 0, binding = 0) uniform ProjectionUniforms {
    mat4 projectionViewMatrices[SHADOW_MAP_CASCADE_COUNT];
} projectionUniforms;

void main() {
    for(int i = 0; i < 3; i++) {
        outTextureCoordinates = inTextureCoordinates[i];
        outMaterialIndex = inMaterialIndex[i];
        gl_Layer = gl_InvocationID;
        gl_Position = projectionUniforms.projectionViewMatrices[gl_InvocationID] * gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}