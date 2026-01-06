#version 450

layout(binding = 0) uniform sampler2D samplerPosition;
layout(binding = 1) uniform sampler2D samplerNormal;
layout(binding = 2) uniform sampler2D samplerAlbedo;
layout(binding = 3) uniform sampler2D samplerSSAO;
layout(binding = 4) uniform sampler2D samplerSSAOBlur;
layout(binding = 5) uniform UBO {
    mat4 _dummy;
    int ssao;
    int ssaoOnly;
    int ssaoBlur;
} uboParams;

layout(location = 0) in vec2 inUV;
layout(location = 0) out vec4 outFragmentColor;

void main() {
    vec3 fragmentPosition = texture(samplerPosition, inUV).rgb;
    vec3 normal = normalize(texture(samplerNormal, inUV).rgb * 2.0 - 1.0);
    vec4 albedo = texture(samplerAlbedo, inUV);

    float ssao = (uboParams.ssaoBlur == 1) ? texture(samplerSSAOBlur, inUV).r : texture(samplerSSAO, inUV).r;

    vec3 lightPosition = vec3(0);
    vec3 L = normalize(lightPosition - fragmentPosition);
    float NdotL = max(0.5, dot(normal, L));

    if(uboParams.ssaoOnly == 1) {
        outFragmentColor.rgb = ssao.rrr;
    } else {
        vec3 baseColor = albedo.rgb * NdotL;
        if(uboParams.ssao == 1) {
            outFragmentColor.rgb = ssao.rrr;
            if(uboParams.ssaoOnly != 1) {
                outFragmentColor.rgb *= baseColor;
            }
        } else {
            outFragmentColor.rgb = baseColor;
        }
    }
}