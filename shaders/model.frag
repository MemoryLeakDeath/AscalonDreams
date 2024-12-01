#version 460

in vec2 outTextureCoord;

out vec4 fragmentColor;

struct Material
{
	vec4 diffuse;
};

uniform sampler2D txtSampler;
uniform Material material;

void main() 
{
	fragmentColor = texture(txtSampler, outTextureCoord) + material.diffuse;
}