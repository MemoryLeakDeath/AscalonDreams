package tv.memoryleakdeath.ascalondreams.asset;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelLoader {
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    public Model load(String filename) {
        AIScene scene = Assimp.aiImportFile(filename, Assimp.aiProcess_Triangulate);
        if (scene == null) {
            logger.error("Unable to load model from file: {}", filename);
            throw new RuntimeException("Unable to load model!");
        }
        AIMesh mesh = AIMesh.create(scene.mMeshes().get(0));
        return processMesh(mesh);
    }

    private Model processMesh(AIMesh mesh) {
        FloatBuffer verticies = MemoryUtil.memAllocFloat(mesh.mNumVertices() * 3);
        mesh.mVertices().forEach(vec -> {
            verticies.put(vec.x()).put(vec.y()).put(vec.z());
        });
        verticies.flip();

        IntBuffer indicies = MemoryUtil.memAllocInt(mesh.mNumFaces() * 3);
        mesh.mFaces().forEach(face -> {
            indicies.put(face.mIndices());
        });
        indicies.flip();

        return new Model(verticies, indicies);
    }
}
