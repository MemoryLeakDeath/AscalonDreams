package tv.memoryleakdeath.ascalondreams.shaders;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL46;

import tv.memoryleakdeath.ascalondreams.util.FileUtils;

public class Shader {
    private int id;

    public Shader(List<ShaderModuleData> data) {
        id = GL46.glCreateProgram();
        if (id == 0) {
            throw new RuntimeException("Unable to create glsl shader program!");
        }

        List<Integer> shaderIds = new ArrayList<>();
        data.forEach(shader -> shaderIds.add(compile(FileUtils.readEntireFile(shader.file()), shader.type())));
        link(shaderIds);
    }

    private int compile(String shaderCode, int type) {
        int shaderId = GL46.glCreateShader(type);
        if (shaderId == 0) {
            throw new RuntimeException("Unable to create shader of type: %d".formatted(type));
        }

        GL46.glShaderSource(shaderId, shaderCode);
        GL46.glCompileShader(shaderId);

        if (GL46.glGetShaderi(shaderId, GL46.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Unable to compile shader id: %d Error: ".formatted(shaderId)
                    + GL46.glGetShaderInfoLog(shaderId, 1024));
        }

        GL46.glAttachShader(id, shaderId);
        return shaderId;
    }

    private void link(List<Integer> shaderIds) {
        GL46.glLinkProgram(id);
        if (GL46.glGetProgrami(id, GL46.GL_LINK_STATUS) == 0) {
            throw new RuntimeException(
                    "Unable to link shaders to program: %d Error: ".formatted(id) + GL46.glGetProgramInfoLog(id, 1024));
        }
        shaderIds.forEach(i -> {
            GL46.glDetachShader(id, i);
            GL46.glDeleteShader(i);
        });
    }

    public void bind() {
        GL46.glUseProgram(id);
    }

    public void unbind() {
        GL46.glUseProgram(0);
    }

    public void validate() {
        GL46.glValidateProgram(id);
        if (GL46.glGetProgrami(id, GL46.GL_VALIDATE_STATUS) == 0) {
            throw new RuntimeException(
                    "Unable to validate shader program: %d Error: ".formatted(id) + GL46.glGetProgramInfoLog(id, 1024));
        }
    }

    public void cleanup() {
        unbind();
        if (id != 0) {
            GL46.glDeleteProgram(id);
        }
    }

    public int getId() {
        return id;
    }

    public record ShaderModuleData(String file, int type) {
    }
}
