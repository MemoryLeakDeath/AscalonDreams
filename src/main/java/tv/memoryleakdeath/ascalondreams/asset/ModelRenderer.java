package tv.memoryleakdeath.ascalondreams.asset;

import org.lwjgl.opengl.GL46;

public class ModelRenderer {

    public void render(Model model) {
        int vertexBufferObject = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vertexBufferObject);
        GL46.glBufferData(GL46.GL_ARRAY_BUFFER, model.getVerticies(), GL46.GL_STATIC_DRAW);

        GL46.glEnableVertexAttribArray(0);
        GL46.glVertexAttribPointer(0, 3, GL46.GL_FLOAT, false, 0, 0);

        int indexBufferObject = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);
        GL46.glBufferData(GL46.GL_ELEMENT_ARRAY_BUFFER, model.getIndicies(), GL46.GL_STATIC_DRAW);

        // draw the thing
        GL46.glDrawElements(GL46.GL_TRIANGLES, model.getIndicies().capacity(), GL46.GL_UNSIGNED_INT, 0);

        // cleanup
        GL46.glDisableVertexAttribArray(0);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, 0);
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL46.glDeleteBuffers(vertexBufferObject);
        GL46.glDeleteBuffers(indexBufferObject);
    }
}
