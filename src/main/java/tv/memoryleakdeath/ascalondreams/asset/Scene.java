package tv.memoryleakdeath.ascalondreams.asset;

import tv.memoryleakdeath.ascalondreams.shaders.Shader;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderUniformManager;

public class Scene {
    private Shader shader;
    private ShaderUniformManager uniformManager;
    private Entity entity;
    private Camera camera = new Camera();

    public Shader getShader() {
        return shader;
    }

    public void setShader(Shader shader) {
        this.shader = shader;
    }

    public ShaderUniformManager getUniformManager() {
        return uniformManager;
    }

    public void setUniformManager(ShaderUniformManager uniformManager) {
        this.uniformManager = uniformManager;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

}
