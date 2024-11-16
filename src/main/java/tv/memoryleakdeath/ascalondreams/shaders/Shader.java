package tv.memoryleakdeath.ascalondreams.shaders;

import java.util.List;

public class Shader {
    private int id;

    public Shader(List<ShaderModuleData> data) {

    }

    public record ShaderModuleData(String file, int type) {
    }
}
