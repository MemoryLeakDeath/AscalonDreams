#version 450
#extension GL_EXT_buffer_reference: require
#extension GL_EXT_buffer_reference2: enable
#extension GL_EXT_scalar_block_layout: require

struct Vertex {
    vec3 inPosition;
    vec3 inNormal;
    vec3 inTangent;
    vec3 inBitangent;
    vec2 inTextureCoordinates;
};

layout(scalar, buffer_reference) buffer VertexBuffer {
    Vertex[] vertices;
};

layout(std430, buffer_reference) buffer IndexBuffer {
    uint[] indices;
};

struct InstanceData {
    mat4 modelMatrix;
    uint materialIndex;
    uint padding[3];
};

layout(push_constant) uniform pc {
    mat4 modelMatrix;
    VertexBuffer vertexBuffer;
    IndexBuffer indexBuffer;
    uint materialIndex;
} push_constants;

layout(location = 0) out vec2 outTextureCoordinates;
layout(location = 1) out flat uint outMaterialIndex;

void main() {
    uint index = push_constants.indexBuffer.indices[gl_VertexIndex];
    VertexBuffer vertexData = push_constants.vertexBuffer;

    Vertex vertex = vertexData.vertices[index];

    outTextureCoordinates = vertex.inTextureCoordinates;
    outMaterialIndex = push_constants.materialIndex;

    gl_Position = push_constants.modelMatrix * vec4(vertex.inPosition, 1.0f);
}