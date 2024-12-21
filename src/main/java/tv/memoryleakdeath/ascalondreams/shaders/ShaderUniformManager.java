package tv.memoryleakdeath.ascalondreams.shaders;

import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public interface ShaderUniformManager {

    int getShaderProgramId();
    Map<String, Integer> getUniformMap();
    void create(String... uniformNames);
    ShaderUniformManager setUniform(String name, int value);
    ShaderUniformManager setUniform(String name, Matrix4f value);
    ShaderUniformManager setUniform(String name, Vector4f value);

}