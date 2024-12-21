package tv.memoryleakdeath.ascalondreams.shaders;

import java.util.List;

public interface Shader {

    void load(List<ShaderModuleData> data);
    void bind();
    void unbind();
    void validate();
    void cleanup();
    int getId();
    record ShaderModuleData(String file, int type) {
    }

}