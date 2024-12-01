package tv.memoryleakdeath.ascalondreams.asset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL46;

import tv.memoryleakdeath.ascalondreams.shaders.Shader;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderUniformManager;

public class ModelRenderer {
    public static final String TEXTURE_PATH = "/home/mem/development/models/scifi-ship/Textures/Sci-Fi Ship_Textures_01.png";
    private static final List<Shader.ShaderModuleData> shaderFiles = List.of(
            new Shader.ShaderModuleData("shaders/model.vert", GL46.GL_VERTEX_SHADER),
            new Shader.ShaderModuleData("shaders/model.frag", GL46.GL_FRAGMENT_SHADER));
    private static final float FOV = (float) Math.toRadians(60.0f);
    private static final float Z_FAR = 1000.0f;
    private static final float Z_NEAR = 0.01f;

    private Shader shader;
    private ShaderUniformManager uniformManager;
    private Entity entity;

    public ModelRenderer() {
        shader = new Shader(shaderFiles);
        createShaderUniforms();
    }

    private void createShaderUniforms() {
        uniformManager = new ShaderUniformManager(shader.getId());
        uniformManager.create("projectionMatrix", "modelMatrix", "viewMatrix", "txtSampler", "material.diffuse");
    }

    private void initEntity(Model model) {
        entity = new Entity("ship", model.getId());
        entity.setPosition(0f, 0f, -4f);
        entity.setScale(1.0f);
        entity.setRotation(1f, 1f, 1f, (float) Math.toRadians(0f));
        entity.updateModelMatrix();
    }

    public void render(Model model) {
        if (entity == null) {
            initEntity(model);
        }
        shader.bind();
        Matrix4f projectionMatrix = new Matrix4f();
        projectionMatrix.setPerspective(FOV, (float)600/600, Z_NEAR, Z_FAR);
        uniformManager.setUniform("projectionMatrix", projectionMatrix);
        uniformManager.setUniform("viewMatrix", new Matrix4f());
        uniformManager.setUniform("txtSampler", 0);

        // List<Entity> entities = model.getEntities();
        Map<String, Texture> textureMap = new HashMap<>();

        uniformManager.setUniform("modelMatrix", entity.getModelMatrix());
        model.getMaterialList().forEach(material -> {
            // String texturePath = material.getTexturePath();
            uniformManager.setUniform("material.diffuse", material.getDiffuseColor());
            Texture texture = textureMap.computeIfAbsent(TEXTURE_PATH, Texture::new);
            GL46.glActiveTexture(GL46.GL_TEXTURE0);
            texture.bind();

            material.getMeshList().forEach(mesh -> {
                GL46.glBindVertexArray(mesh.getVaoId());
                GL46.glDrawElements(GL46.GL_TRIANGLES, mesh.getNumVertices(), GL46.GL_UNSIGNED_INT, 0);
            });
        });
        GL46.glBindVertexArray(0);
        shader.unbind();
    }
}
