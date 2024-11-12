package tv.memoryleakdeath.ascalondreams.asset;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.util.FileUtils;

public class ModelLoader {
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    public Model load(String modelId, String filename) {
        AIScene scene = Assimp.aiImportFile(filename, Assimp.aiProcess_Triangulate
                | Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_FixInfacingNormals
                | Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_CalcTangentSpace
                | Assimp.aiProcess_LimitBoneWeights | Assimp.aiProcess_PreTransformVertices);
        if (scene == null) {
            logger.error("Unable to load model from file: {}", filename);
            throw new RuntimeException("Unable to load model!");
        }

        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < scene.mNumMaterials(); i++) {
            AIMaterial material = AIMaterial.create(scene.mMaterials().get(i));
            materials.add(processMaterial(material, FileUtils.getParentDirectory(filename)));
        }

        Material defaultMaterial = new Material();
        for (int i = 0; i < scene.mNumMeshes(); i++) {
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(i));
            Mesh mesh = processMesh(aiMesh);
            int materialIndex = aiMesh.mMaterialIndex();
            Material material = defaultMaterial;
            if (materialIndex >= 0 && materialIndex < materials.size()) {
                material = materials.get(materialIndex);
            }
            material.getMeshList().add(mesh);
        }

        if (!defaultMaterial.getMeshList().isEmpty()) {
            materials.add(defaultMaterial);
        }
        return new Model(modelId, materials);
    }

    private Mesh processMesh(AIMesh mesh) {
        float[] verticies = processVerticies(mesh);
        float[] texCoords = processTextureCoords(mesh);
        int[] indicies = processIndicies(mesh);

        if (texCoords.length == 0) {
            int numElements = (verticies.length / 3) * 2;
            texCoords = new float[numElements];
        }

        return new Mesh(verticies, texCoords, indicies);
    }

    private float[] processVerticies(AIMesh mesh) {
        return ArrayUtils.toPrimitive(
                mesh.mVertices().stream().flatMap(vec -> Stream.of(vec.x(), vec.y(), vec.z())).toArray(Float[]::new));
    }

    private float[] processTextureCoords(AIMesh mesh) {
        return ArrayUtils.toPrimitive(
                mesh.mTextureCoords(0).stream().flatMap(vec -> Stream.of(vec.x(), vec.y())).toArray(Float[]::new));
    }

    private int[] processIndicies(AIMesh mesh) {
        List<Integer> indicies = new ArrayList<>();
        mesh.mFaces().forEach(face -> {
            IntBuffer buffer = face.mIndices();
            while (buffer.hasRemaining()) {
                indicies.add(buffer.get());
            }
        });
        return indicies.stream().mapToInt(Integer::intValue).toArray();
    }

    private Material processMaterial(AIMaterial aiMaterial, String modelPath) {
        Material material = new Material();
        AIColor4D color = AIColor4D.create();
        int result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0,
                color);
        if (result == Assimp.aiReturn_SUCCESS) {
            material.setDiffuseColor(new Vector4f(color.r(), color.g(), color.b(), color.a()));
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            AIString aiTexturePath = AIString.calloc(memoryStack);
            Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, aiTexturePath, (IntBuffer) null,
                    null, null, null, null, null);
            String texturePath = aiTexturePath.dataString();
            if (StringUtils.isNotEmpty(texturePath)) {
                material.setTexturePath(modelPath + File.separator + new File(texturePath).getName());
            }
        }
        return material;
    }
}
