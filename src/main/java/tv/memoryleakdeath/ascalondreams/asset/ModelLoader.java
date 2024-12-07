package tv.memoryleakdeath.ascalondreams.asset;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.joml.Vector4f;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.util.FileUtils;

public class ModelLoader {
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    public Model load(String modelId, String filename) {
        AIScene scene = Assimp.aiImportFile(filename, Assimp.aiProcess_Triangulate
                | Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_FixInfacingNormals
                | Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_CalcTangentSpace
                | Assimp.aiProcess_LimitBoneWeights | Assimp.aiProcess_PreTransformVertices | Assimp.aiProcess_FlipUVs);
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
            logger.debug("Number of meshes: {}", scene.mNumMeshes());
            logger.debug("Number of textures: {}", scene.mNumTextures());
            logger.debug("Number of materials: {}", scene.mNumMaterials());
            logger.debug("Number of lights: {}", scene.mNumLights());
            logger.debug("Number of animations: {}", scene.mNumAnimations());
            logger.debug("Number of skeletons: {}", scene.mNumSkeletons());
            logger.debug("Number of cameras: {}", scene.mNumCameras());

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
        List<float[]> texCoords = processTextureCoords(mesh);
        int[] indicies = processIndicies(mesh);
        float[] colors = processColors(mesh);

        if (texCoords.size() == 0) {
            int numElements = (verticies.length / 3) * 2;
            texCoords = List.of(new float[numElements]);
        }
        logger.debug("Num verticies: {}", verticies.length);
        logger.debug("Num texture coordinate sets: {}", texCoords.size());
        for (int i = 0; i < texCoords.size(); i++) {
            logger.debug("Num texture coordinates in set {}: {}", i, texCoords.get(i).length);
        }
        logger.debug("Num indicies: {}", indicies.length);
        logger.debug("Num colors: {}", colors.length);
        return new Mesh(verticies, texCoords, indicies, colors);
    }

    private float[] processVerticies(AIMesh mesh) {
        return ArrayUtils.toPrimitive(
                mesh.mVertices().stream().flatMap(vec -> Stream.of(vec.x(), vec.y(), vec.z())).toArray(Float[]::new));
    }

    private List<float[]> processTextureCoords(AIMesh mesh) {
        List<float[]> textureCoordList = new ArrayList<>();
        for (int i = 0; i < mesh.mTextureCoords().limit(); i++) {
            if (mesh.mTextureCoords(i) != null) {
                textureCoordList.add(ArrayUtils.toPrimitive(mesh.mTextureCoords(i).stream()
                        .flatMap(vec -> Stream.of(vec.x(), vec.y())).toArray(Float[]::new)));
            } else {
                logger.debug("Texture Coordinate Set: {} is null", i);
            }
        }
        return textureCoordList;
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
        logger.debug("Material - diffuse - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_DIFFUSE));
        logger.debug("Material - specular - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_SPECULAR));
        logger.debug("Material - height - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_HEIGHT));
        logger.debug("Material - base color - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_BASE_COLOR));
        logger.debug("Material - emissive - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_EMISSIVE));
        logger.debug("Material - emission color - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_EMISSION_COLOR));
        logger.debug("Material - normals - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_NORMALS));
        logger.debug("Material - opacity - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_OPACITY));
        logger.debug("Material - metalness - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_METALNESS));
        logger.debug("Material - NONE - texture count: {}",
                Assimp.aiGetMaterialTextureCount(aiMaterial, Assimp.aiTextureType_NONE));
        int result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_DIFFUSE,
                0,
                color);
        if (result == Assimp.aiReturn_SUCCESS) {
            material.setDiffuseColor(new Vector4f(color.r(), color.g(), color.b(), color.a()));
        }
        return material;
    }

    private float[] processColors(AIMesh mesh) {
        if (mesh.mColors(0) == null) {
            return new float[mesh.mNumVertices() * 4];
        }
        return ArrayUtils.toPrimitive(mesh.mColors(0).stream()
                .flatMap(color -> Stream.of(color.r(), color.g(), color.b(), color.a())).toArray(Float[]::new));
    }
}
