#version 450
#extension GL_EXT_buffer_reference: require
#extension GL_EXT_shader_explicit_arithmetic_types_int64 : enable

layout(std430, buffer_reference) buffer FloatBuffer {
    float data[];
};

layout(std430, buffer_reference) buffer MatricesBuffer {
    mat4[] data;
};

layout(local_size_x=32, local_size_y=1, local_size_z=1) in;

layout(push_constant) uniform Pc {
    FloatBuffer sourceBuffer;
    FloatBuffer weightsBuffer;
    MatricesBuffer jointsBuffer;
    FloatBuffer destinationBuffer;
    uint64_t sourceBufferFloatSize;
} pc;

void main() {
    int baseIndexSourceBuffer = int(gl_GlobalInvocationID.x) * 14;
    if(baseIndexSourceBuffer >= pc.sourceBufferFloatSize) {
        return;
    }
    int baseIndexWeightsBuffer = int(gl_GlobalInvocationID.x) * 8;
    vec4 weights = vec4(pc.weightsBuffer.data[baseIndexWeightsBuffer], pc.weightsBuffer.data[baseIndexWeightsBuffer + 1], pc.weightsBuffer.data[baseIndexWeightsBuffer + 2], pc.weightsBuffer.data[baseIndexWeightsBuffer + 3]);
    ivec4 joints = ivec4(pc.weightsBuffer.data[baseIndexWeightsBuffer + 4], pc.weightsBuffer.data[baseIndexWeightsBuffer + 5], pc.weightsBuffer.data[baseIndexWeightsBuffer + 6], pc.weightsBuffer.data[baseIndexWeightsBuffer + 7]);

    vec4 position = vec4(pc.sourceBuffer.data[baseIndexSourceBuffer], pc.sourceBuffer.data[baseIndexSourceBuffer + 1], pc.sourceBuffer.data[baseIndexSourceBuffer + 2], 1);

    position =
                    weights.x * pc.jointsBuffer.data[joints.x] * position +
                    weights.y * pc.jointsBuffer.data[joints.y] * position +
                    weights.z * pc.jointsBuffer.data[joints.z] * position +
                    weights.w * pc.jointsBuffer.data[joints.w] * position;
    pc.destinationBuffer.data[baseIndexSourceBuffer] = position.x / position.w;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 1] = position.y / position.w;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 2] = position.z / position.w;

    mat3 matJoint1 = mat3(transpose(inverse(pc.jointsBuffer.data[joints.x])));
    mat3 matJoint2 = mat3(transpose(inverse(pc.jointsBuffer.data[joints.y])));
    mat3 matJoint3 = mat3(transpose(inverse(pc.jointsBuffer.data[joints.z])));
    baseIndexSourceBuffer += 3;

    vec3 normal = vec3(pc.sourceBuffer.data[baseIndexSourceBuffer], pc.sourceBuffer.data[baseIndexSourceBuffer + 1], pc.sourceBuffer.data[baseIndexSourceBuffer + 2]);
    normal =
            weights.x * matJoint1 * normal +
            weights.y * matJoint2 * normal +
            weights.z * matJoint3 * normal;
    normal = normalize(normal);
    pc.destinationBuffer.data[baseIndexSourceBuffer] = normal.x;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 1] = normal.y;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 2] = normal.z;
    baseIndexSourceBuffer += 3;

    vec3 tangent = vec3(pc.sourceBuffer.data[baseIndexSourceBuffer], pc.sourceBuffer.data[baseIndexSourceBuffer + 1], pc.sourceBuffer.data[baseIndexSourceBuffer + 2]);
    tangent =
            weights.x * matJoint1 * tangent +
            weights.y * matJoint2 * tangent +
            weights.z * matJoint3 * tangent;
    tangent = normalize(tangent);
    pc.destinationBuffer.data[baseIndexSourceBuffer] = tangent.x;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 1] = tangent.y;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 2] = tangent.z;
    baseIndexSourceBuffer += 3;

    vec3 bitangent = vec3(pc.sourceBuffer.data[baseIndexSourceBuffer], pc.sourceBuffer.data[baseIndexSourceBuffer + 1], pc.sourceBuffer.data[baseIndexSourceBuffer + 2]);
    bitangent =
    weights.x * matJoint1 * bitangent +
    weights.y * matJoint2 * bitangent +
    weights.z * matJoint3 * bitangent;
    bitangent = normalize(bitangent);
    pc.destinationBuffer.data[baseIndexSourceBuffer] = bitangent.x;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 1] = bitangent.y;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 2] = bitangent.z;
    baseIndexSourceBuffer += 3;

    vec2 textureCoordinates = vec2(pc.sourceBuffer.data[baseIndexSourceBuffer], pc.sourceBuffer.data[baseIndexSourceBuffer + 1]);
    pc.destinationBuffer.data[baseIndexSourceBuffer] = textureCoordinates.x;
    pc.destinationBuffer.data[baseIndexSourceBuffer + 1] = textureCoordinates.y;
}