package tv.memoryleakdeath.ascalondreams.asset;

import java.io.Serializable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

public class Mesh implements Serializable {
    private static final long serialVersionUID = 1L;

    private int numVertices;
    private int vaoId;
    private List<Integer> vboIdList = new ArrayList<>();

    public int getNumVertices() {
        return numVertices;
    }

    public void setNumVertices(int numVertices) {
        this.numVertices = numVertices;
    }

    public int getVaoId() {
        return vaoId;
    }

    public void setVaoId(int vaoId) {
        this.vaoId = vaoId;
    }

    public List<Integer> getVboIdList() {
        return vboIdList;
    }

    public void setVboIdList(List<Integer> vboIdList) {
        this.vboIdList = vboIdList;
    }

    public Mesh(float[] verticies, float[] texCoords, int[] indicies) {
        init(verticies, texCoords, indicies);
    }

    private void init(float[] verticies, float[] texCoords, int[] indicies) {
        setNumVertices(indicies.length);
        setVaoId(GL46.glGenVertexArrays());
        GL46.glBindVertexArray(getVaoId());

        // verticies vbo
        int vertexVboId = GL46.glGenBuffers();
        vboIdList.add(vertexVboId);
        FloatBuffer vertexBuffer = MemoryUtil.memCallocFloat(verticies.length);
        // FloatBuffer vertexBuffer = memoryStack.callocFloat();
        vertexBuffer.put(0, verticies);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, vertexVboId);
        GL46.glBufferData(GL46.GL_ARRAY_BUFFER, vertexBuffer, GL46.GL_STATIC_DRAW);
        GL46.glEnableVertexAttribArray(0);
        GL46.glVertexAttribPointer(0, 3, GL46.GL_FLOAT, false, 0, 0);

        // textures vbo
        int texVboId = GL46.glGenBuffers();
        vboIdList.add(texVboId);
        FloatBuffer texBuffer = MemoryUtil.memCallocFloat(texCoords.length);
        // FloatBuffer texBuffer = memoryStack.callocFloat(texCoords.length);
        texBuffer.put(0, texCoords);
        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, texVboId);
        GL46.glBufferData(GL46.GL_ARRAY_BUFFER, texBuffer, GL46.GL_STATIC_DRAW);
        GL46.glEnableVertexAttribArray(1);
        GL46.glVertexAttribPointer(1, 2, GL46.GL_FLOAT, false, 0, 0);

        // index vbo
        int indexVboId = GL46.glGenBuffers();
        vboIdList.add(indexVboId);
        IntBuffer indexBuffer = MemoryUtil.memCallocInt(indicies.length);
        // IntBuffer indexBuffer = memoryStack.callocInt(indicies.length);
        indexBuffer.put(0, indicies);
        GL46.glBindBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER, indexVboId);
        GL46.glBufferData(GL46.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL46.GL_STATIC_DRAW);

        GL46.glBindBuffer(GL46.GL_ARRAY_BUFFER, 0);
        GL46.glBindVertexArray(0);
    }

    public void cleanup() {
        vboIdList.forEach(GL46::glDeleteBuffers);
        GL46.glDeleteVertexArrays(vaoId);
    }
}
