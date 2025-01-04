package tv.memoryleakdeath.ascalondreams.asset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46;

import tv.memoryleakdeath.ascalondreams.shaders.opengl.GLShader;
import tv.memoryleakdeath.ascalondreams.shaders.opengl.GLShaderUniformManager;

public class ModelRenderer {
    public static final String TEXTURE_PATH = "/home/mem/development/models/scifi-ship/Textures/Sci-Fi Ship_Textures_01.png";
    public static final String EMISSIVE_TEXTURE_PATH = "/home/mem/development/models/scifi-ship/Textures/Sci-Fi Ship_Textures_Emissive.png";
    private static final List<GLShader.ShaderModuleData> shaderFiles = List.of(
            new GLShader.ShaderModuleData("shaders/model.vert", GL46.GL_VERTEX_SHADER),
            new GLShader.ShaderModuleData("shaders/model.frag", GL46.GL_FRAGMENT_SHADER));
    private static final float FOV = (float) Math.toRadians(60.0f);
    private static final float Z_FAR = 1000.0f;
    private static final float Z_NEAR = 0.01f;


    private Scene scene;
    private Matrix4f projectionMatrix = new Matrix4f();
    private Map<String, Texture> textureMap = new HashMap<>();

    public ModelRenderer() {
        scene = new Scene();
        scene.setShader(new GLShader(shaderFiles));
        createShaderUniforms();
        projectionMatrix.setPerspective(FOV, (float) 600 / 600, Z_NEAR, Z_FAR);
    }

    private void createShaderUniforms() {
        scene.setUniformManager(new GLShaderUniformManager(scene.getShader().getId()));
        scene.getUniformManager().create("projectionMatrix", "modelMatrix", "viewMatrix", "txtSampler",
                "material.diffuse");
    }

    private void initEntity(Model model) {
        scene.setEntity(new Entity("ship", model.getId()));
        scene.getEntity()
                .setPosition(0f, 0f, -4f)
                .setScale(1.0f)
                .setRotation(1f, 1f, 1f, (float) Math.toRadians(0f))
                .updateModelMatrix();
        getCamera().setCameraTarget(new Vector3f(0f, 0f, -4f));
    }

    public void render(Model model) {
        if (scene.getEntity() == null) {
            initEntity(model);
        }
        scene.getShader().bind();
        scene.getUniformManager().setUniform("projectionMatrix", projectionMatrix)
                .setUniform("viewMatrix", getCamera().getViewMatrix())
                .setUniform("txtSampler", 0);

        // List<Entity> entities = model.getEntities();

        scene.getUniformManager().setUniform("modelMatrix", scene.getEntity().getModelMatrix());
        Texture texture = textureMap.computeIfAbsent(TEXTURE_PATH, Texture::new);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        texture.bind();
        Texture emissiveTexture = textureMap.computeIfAbsent(EMISSIVE_TEXTURE_PATH, Texture::new);
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        emissiveTexture.bind();

        model.getMaterialList().forEach(material -> {
            scene.getUniformManager().setUniform("material.diffuse", material.getDiffuseColor());
            material.getMeshList().forEach(mesh -> {
                GL46.glBindVertexArray(mesh.getVaoId());
                GL46.glDrawElements(GL46.GL_TRIANGLES, mesh.getNumVertices(), GL46.GL_UNSIGNED_INT, 0);
            });
        });
        GL46.glBindVertexArray(0);
        scene.getShader().unbind();
    }

    public Camera getCamera() {
        return scene.getCamera();
    }

    public Scene getScene() {
        return scene;
    }
}
