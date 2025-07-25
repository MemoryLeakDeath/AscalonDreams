package tv.memoryleakdeath.ascalondreams.common.asset;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.common.model.Material;
import tv.memoryleakdeath.ascalondreams.common.model.Mesh;
import tv.memoryleakdeath.ascalondreams.common.model.Model;
import tv.memoryleakdeath.ascalondreams.util.FileUtils;

public class ModelLoader {
    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    public Model load(String modelId, String modelFile, String textureDir) {
        return load(modelId, modelFile, textureDir, null);
    }

    public Model load(String modelId, String modelFile, String textureDir, List<String> textureFiles) {
        logger.debug("Loading model file: {} with texture directory: {}", modelFile, textureDir);
        if(!FileUtils.exists(modelFile)) {
            logger.error("Model file: {} does not exist!", modelFile);
            throw new RuntimeException("Model file: %s does not exist!".formatted(modelFile));
        }
        if(StringUtils.isNotBlank(textureDir) && !FileUtils.dirExists(textureDir)) {
            logger.error("Texture directory: {} does not exist!", textureDir);
            throw new RuntimeException("Texture directory: %s does not exist!".formatted(textureDir));
        }
        if(textureFiles != null && !textureFiles.isEmpty() && !FileUtils.allExists(textureFiles)) {
            logger.error("One or more texture files: {} do not exist!", textureFiles);
            throw new RuntimeException("One or more texture files: %s do not exist".formatted(textureFiles));
        }

        AIScene scene = Assimp.aiImportFile(modelFile, Assimp.aiProcess_Triangulate
                | Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_FixInfacingNormals
                | Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_CalcTangentSpace
                | Assimp.aiProcess_LimitBoneWeights | Assimp.aiProcess_PreTransformVertices | Assimp.aiProcess_FlipUVs);
        if (scene == null) {
            logger.error("Unable to load model from file: {}", modelFile);
            throw new RuntimeException("Unable to load model!");
        }

        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < scene.mNumMaterials(); i++) {
            AIMaterial material = AIMaterial.create(scene.mMaterials().get(i));
            materials.add(processMaterial(material, textureDir, null));
        }

        List<Mesh> meshList = new ArrayList<>();
        for (int i = 0; i < scene.mNumMeshes(); i++) {
            if(logger.isDebugEnabled()) {
                debugLogSceneData(scene);
            }
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(i));
            meshList.add(processMesh(aiMesh));
        }
        return new Model(modelId, meshList, materials);
    }

    private void debugLogSceneData(AIScene scene) {
        logger.debug("Number of meshes: {}", scene.mNumMeshes());
        logger.debug("Number of textures: {}", scene.mNumTextures());
        logger.debug("Number of materials: {}", scene.mNumMaterials());
        logger.debug("Number of lights: {}", scene.mNumLights());
        logger.debug("Number of animations: {}", scene.mNumAnimations());
        logger.debug("Number of skeletons: {}", scene.mNumSkeletons());
        logger.debug("Number of cameras: {}", scene.mNumCameras());
    }

    private Mesh processMesh(AIMesh mesh) {
        float[] verticies = processVerticies(mesh);
        List<float[]> texCoords = processTextureCoords(mesh);
        int[] indicies = processIndicies(mesh);
        float[] colors = processColors(mesh);

        if (texCoords.isEmpty()) {
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

    private Material processMaterial(AIMaterial aiMaterial, String texturePath, String textureFile) {
        Material material = new Material();
        AIColor4D color = AIColor4D.create();
        if(logger.isDebugEnabled()) {
            debugLogMaterialInfo(aiMaterial);
        }
        int result = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_DIFFUSE,
                0,
                color);
        if (result == Assimp.aiReturn_SUCCESS) {
            material.setDiffuseColor(new Vector4f(color.r(), color.g(), color.b(), color.a()));
        }
        if(StringUtils.isNotBlank(textureFile) && FileUtils.exists(textureFile)) {
            material.setTexturePath(textureFile);
            material.setDiffuseColor(new Vector4f(0f, 0f, 0f, 0f));
        } else if(StringUtils.isNotBlank(texturePath)) {
            material.setTexturePath(getTexturePath(aiMaterial, texturePath));
            material.setDiffuseColor(new Vector4f(0f, 0f, 0f, 0f));
        }
        return material;
    }

    private String getTexturePath(AIMaterial aiMaterial, String textureDir) {
        String fullTextureFile = "";
        try(MemoryStack stack = MemoryStack.stackPush()) {
            AIString texturePath = AIString.calloc(stack);
            Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, texturePath, (IntBuffer) null, null, null, null, null, null);
            String textureFile = texturePath.dataString();
            fullTextureFile = FileUtils.appendPathAndCheckExists(textureDir, textureFile);
        }
        return fullTextureFile;
    }

    private void debugLogMaterialInfo(AIMaterial aiMaterial) {
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
    }

    private float[] processColors(AIMesh mesh) {
        if (mesh.mColors(0) == null) {
            return new float[mesh.mNumVertices() * 4];
        }
        return ArrayUtils.toPrimitive(mesh.mColors(0).stream()
                .flatMap(color -> Stream.of(color.r(), color.g(), color.b(), color.a())).toArray(Float[]::new));
    }
}
