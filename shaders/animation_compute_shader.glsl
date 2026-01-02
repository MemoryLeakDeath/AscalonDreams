#version 450

layout(std430, set=0, binding=0) readonly buffer sourceBuffer {
    float data[];
} sourceVector;

layout(std430, set=1, binding=0) readonly buffer weightsBuffer {
    float data[];
} weightsVector;

layout(std430, set=2, binding=0) buffer destinationBuffer {
    float data[];
} destinationVector;

layout(std430, set=3, binding=0) readonly buffer jointBuffer {
    mat4 data[];
} jointMatrices;

layout(local_size_x=32, local_size_y=1, local_size_z=1) in;

void main() {
    int baseIndexWeightsBuffer = int(gl_GlobalInvocationID.x) * 8;
    vec4 weights = vec4(weightsVector.data[baseIndexWeightsBuffer], weightsVector.data[baseIndexWeightsBuffer + 1], weightsVector.data[baseIndexWeightsBuffer + 2], weightsVector.data[baseIndexWeightsBuffer + 3]);
    ivec4 joints = ivec4(weightsVector.data[baseIndexWeightsBuffer + 4], weightsVector.data[baseIndexWeightsBuffer + 5], weightsVector.data[baseIndexWeightsBuffer + 6], weightsVector.data[baseIndexWeightsBuffer + 7]);

    int baseIndexSourceBuffer = int(gl_GlobalInvocationID.x) * 14;
    vec4 position = vec4(sourceVector.data[baseIndexSourceBuffer], sourceVector.data[baseIndexSourceBuffer + 1], sourceVector.data[baseIndexSourceBuffer + 2], 1);

    position =
                    weights.x * jointMatrices.data[joints.x] * position +
                    weights.y * jointMatrices.data[joints.y] * position +
                    weights.z * jointMatrices.data[joints.z] * position +
                    weights.w * jointMatrices.data[joints.w] * position;
    destinationVector.data[baseIndexSourceBuffer] = position.x / position.w;
    destinationVector.data[baseIndexSourceBuffer + 1] = position.y / position.w;
    destinationVector.data[baseIndexSourceBuffer + 2] = position.z / position.w;

    mat3 matJoint1 = mat3(transpose(inverse(jointMatrices.data[joints.x])));
    mat3 matJoint2 = mat3(transpose(inverse(jointMatrices.data[joints.y])));
    mat3 matJoint3 = mat3(transpose(inverse(jointMatrices.data[joints.z])));
    baseIndexSourceBuffer += 3;

    vec3 normal = vec3(sourceVector.data[baseIndexSourceBuffer], sourceVector.data[baseIndexSourceBuffer + 1], sourceVector.data[baseIndexSourceBuffer + 2]);
    normal =
            weights.x * matJoint1 * normal +
            weights.y * matJoint2 * normal +
            weights.z * matJoint3 * normal;
    normal = normalize(normal);
    destinationVector.data[baseIndexSourceBuffer] = normal.x;
    destinationVector.data[baseIndexSourceBuffer + 1] = normal.y;
    destinationVector.data[baseIndexSourceBuffer + 2] = normal.z;
    baseIndexSourceBuffer += 3;

    vec3 tangent = vec3(sourceVector.data[baseIndexSourceBuffer], sourceVector.data[baseIndexSourceBuffer + 1], sourceVector.data[baseIndexSourceBuffer + 2]);
    tangent =
            weights.x * matJoint1 * tangent +
            weights.y * matJoint2 * tangent +
            weights.z * matJoint3 * tangent;
    tangent = normalize(tangent);
    destinationVector.data[baseIndexSourceBuffer] = tangent.x;
    destinationVector.data[baseIndexSourceBuffer + 1] = tangent.y;
    destinationVector.data[baseIndexSourceBuffer + 2] = tangent.z;
    baseIndexSourceBuffer += 3;

    vec3 bitangent = vec3(sourceVector.data[baseIndexSourceBuffer], sourceVector.data[baseIndexSourceBuffer + 1], sourceVector.data[baseIndexSourceBuffer + 2]);
    bitangent =
    weights.x * matJoint1 * bitangent +
    weights.y * matJoint2 * bitangent +
    weights.z * matJoint3 * bitangent;
    bitangent = normalize(bitangent);
    destinationVector.data[baseIndexSourceBuffer] = bitangent.x;
    destinationVector.data[baseIndexSourceBuffer + 1] = bitangent.y;
    destinationVector.data[baseIndexSourceBuffer + 2] = bitangent.z;
    baseIndexSourceBuffer += 3;

    vec2 textureCoordinates = vec2(sourceVector.data[baseIndexSourceBuffer], sourceVector.data[baseIndexSourceBuffer + 1]);
    destinationVector.data[baseIndexSourceBuffer] = textureCoordinates.x;
    destinationVector.data[baseIndexSourceBuffer + 1] = textureCoordinates.y;
}