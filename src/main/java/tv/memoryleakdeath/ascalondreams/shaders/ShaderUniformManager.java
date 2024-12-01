package tv.memoryleakdeath.ascalondreams.shaders;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShaderUniformManager {
    private static final Logger logger = LoggerFactory.getLogger(ShaderUniformManager.class);
    private int shaderProgramId;
    private Map<String, Integer> uniformMap = new HashMap<>();

    public ShaderUniformManager(int shaderProgramId) {
        this.shaderProgramId = shaderProgramId;
    }

    public int getShaderProgramId() {
        return shaderProgramId;
    }

    public Map<String, Integer> getUniformMap() {
        return uniformMap;
    }

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

    public void setUniform(String name, int value) {
        GL46.glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform(String name, Matrix4f value) {
        try (MemoryStack m = MemoryStack.stackPush()) {
            GL46.glUniformMatrix4fv(getUniformLocation(name), false, value.get(m.mallocFloat(16)));
        }
    }

    public void setUniform(String name, Vector4f value) {
        GL46.glUniform4f(getUniformLocation(name), value.x, value.y, value.z, value.w);
    }
}
