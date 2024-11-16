package tv.memoryleakdeath.ascalondreams.asset;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL46;

public class ModelRenderer {
    public static final String TEXTURE_PATH = "/home/mem/development/models/scifi-ship/Textures/Sci-Fi Ship_Textures_01.png";

    public void render(Model model) {
        // List<Entity> entities = model.getEntities();
        Map<String, Texture> textureMap = new HashMap<>();

        model.getMaterialList().forEach(material -> {
            // String texturePath = material.getTexturePath();
            Texture texture = textureMap.computeIfAbsent(TEXTURE_PATH, Texture::new);
            GL46.glActiveTexture(GL46.GL_TEXTURE0);
            texture.bind();

            material.getMeshList().forEach(mesh -> {
                GL46.glBindVertexArray(mesh.getVaoId());
                GL46.glDrawElements(GL46.GL_TRIANGLES, mesh.getNumVertices(), GL46.GL_UNSIGNED_INT, 0);
            });
        });
        GL46.glBindVertexArray(0);
    }
}
