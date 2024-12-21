package tv.memoryleakdeath.ascalondreams.shaders.opengl;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.shaders.ShaderUniformManager;

public class GLShaderUniformManager implements ShaderUniformManager {
    private static final Logger logger = LoggerFactory.getLogger(GLShaderUniformManager.class);
    private int shaderProgramId;
    private Map<String, Integer> uniformMap = new HashMap<>();

    public GLShaderUniformManager(int shaderProgramId) {
        this.shaderProgramId = shaderProgramId;
    }

    @Override
    public int getShaderProgramId() {
        return shaderProgramId;
    }

    @Override
    public Map<String, Integer> getUniformMap() {
        return uniformMap;
    }

    @Override
    public void create(String... uniformNames) {
        for (String uniformName : uniformNames) {
            int uniformLocation = GL46.glGetUniformLocation(shaderProgramId, uniformName);
            if (uniformLocation < 0) {
                throw new RuntimeException("Unable to find shader uniform location for name: %s in shader program: %d"
                        .formatted(uniformName, shaderProgramId));
            }
            uniformMap.put(uniformName, uniformLocation);
        }
    }

    private int getUniformLocation(String uniformName) {
        if (!uniformMap.containsKey(uniformName)) {
            throw new RuntimeException("Could not find uniform with name: %s".formatted(uniformName));
        }
        return uniformMap.get(uniformName);
    }

    @Override
    public ShaderUniformManager setUniform(String name, int value) {
        GL46.glUniform1i(getUniformLocation(name), value);
        return this;
    }

    @Override
    public ShaderUniformManager setUniform(String name, Matrix4f value) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(16);
        GL46.glUniformMatrix4fv(getUniformLocation(name), false, value.get(buffer));
        MemoryUtil.memFree(buffer);
        return this;
    }

    @Override
    public ShaderUniformManager setUniform(String name, Vector4f value) {
        GL46.glUniform4f(getUniformLocation(name), value.x, value.y, value.z, value.w);
        return this;
    }
}
