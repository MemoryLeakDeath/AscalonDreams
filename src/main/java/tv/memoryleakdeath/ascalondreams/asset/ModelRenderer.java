package tv.memoryleakdeath.ascalondreams.asset;

import java.util.List;

public class ModelRenderer {

    public void render(Model model) {
        model.getEntities().forEach(entity -> {

        });

        // draw the thing
        // GL46.glDrawElements(GL46.GL_TRIANGLES, model.getIndicies().capacity(),
        // GL46.GL_UNSIGNED_INT, 0);

        // cleanup
    }

    private void renderMaterials(List<Material> materialList) {

    }
}
