package tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMaterial;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMeshData;

import java.io.File;
import java.io.IO;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelConverter {
   private static final Logger logger = LoggerFactory.getLogger(ModelConverter.class);
   private static final Pattern GET_TEXTURE_ID = Pattern.compile("\\*([0-9]+)");
   private static final int FLAGS = Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_JoinIdenticalVertices |
           Assimp.aiProcess_Triangulate | Assimp.aiProcess_FixInfacingNormals | Assimp.aiProcess_CalcTangentSpace |
           Assimp.aiProcess_PreTransformVertices;

   private ModelConverter() {
   }

   public static void processFile(String modelFile) throws IOException {
      logger.debug("Loading model file: {}", modelFile);
      File file = new File(modelFile);
      if(!file.exists()) {
         throw new RuntimeException("Model file: %s does not exist!".formatted(modelFile));
      }

      AIScene scene = Assimp.aiImportFile(modelFile, FLAGS);
      if(scene == null) {
         throw new RuntimeException("Error loading model: %s".formatted(modelFile));
      }

      String modelId = FilenameUtils.getBaseName(modelFile);
      ConvertedModel convertedModel = new ConvertedModel();

      int numMaterials = scene.mNumMaterials();
      logger.debug("Number of materials: {}", numMaterials);
      List<VulkanMaterial> parsedMaterials = new ArrayList<>();
      File parentDirectory = file.getParentFile();
      for(int i = 0; i < numMaterials; i++) {
         var aiMaterial = AIMaterial.create(scene.mMaterials().get(i));
         parsedMaterials.add(processMaterial(scene, aiMaterial, modelId, parentDirectory.getPath(), i));
      }

      int numMeshes = scene.mNumMeshes();
      PointerBuffer aiMeshes = scene.mMeshes();
      List<VulkanMeshData> meshDataList = new ArrayList<>();
      for(int i = 0; i < numMeshes; i++) {
         AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
         meshDataList.add(processMesh(aiMesh, parsedMaterials, i));
      }

      // todo: write the file
   }

   private static VulkanMaterial processMaterial(AIScene scene, AIMaterial material, String modelId, String textureDir, int index)
           throws IOException {
      Vector4f diffuseVec = new Vector4f();
      AIColor4D color = AIColor4D.create();
      int result = Assimp.aiGetMaterialColor(material, Assimp.AI_MATKEY_COLOR_DIFFUSE, Assimp.aiTextureType_NONE, 0, color);
      if(result == Assimp.aiReturn_SUCCESS) {
         diffuseVec.set(color.r(), color.g(), color.b(), color.a());
      }
      String diffuseTexturePath = processTexture(scene, material, textureDir, Assimp.aiTextureType_DIFFUSE);
      return new VulkanMaterial("%s-mat-%d".formatted(modelId, index), diffuseTexturePath, diffuseVec);
   }

   private static String processTexture(AIScene scene, AIMaterial material, String textureDir, int textureType)
      throws IOException {
      String texturePath;
      try(var stack = MemoryStack.stackPush()) {
         int numEmbeddedTextures = scene.mNumTextures();
         AIString aiTexturePath = AIString.calloc(stack);
         Assimp.aiGetMaterialTexture(material, textureType, 0, aiTexturePath, (IntBuffer)null,
                 null, null, null, null, null);
         texturePath = aiTexturePath.dataString();
         if(StringUtils.isNotBlank(textureDir)) {
            Matcher matcher = GET_TEXTURE_ID.matcher(texturePath);
            int embeddedTextureIndex = matcher.matches() && matcher.groupCount() > 0 ? Integer.parseInt(matcher.group(1)) : -1;
            if(embeddedTextureIndex >= 0 && embeddedTextureIndex < numEmbeddedTextures) {
               var aiTexture = AITexture.create(scene.mTextures().get(embeddedTextureIndex));
               String baseFileName = aiTexture.mFilename().dataString() + ".png";
               texturePath = "%s/%s".formatted(textureDir, baseFileName);
               logger.info("Writing texture file to {}", texturePath);

               var channel = FileChannel.open(Path.of(texturePath), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
               channel.write(aiTexture.pcDataCompressed());
            } else {
               texturePath = "%s/%s".formatted(textureDir, FilenameUtils.getName(texturePath));
            }
         }
      }
      return texturePath;
   }

   private static VulkanMeshData processMesh(AIMesh mesh, List<VulkanMaterial> materialList, int position) {
      VulkanMeshData returnData = new VulkanMeshData();
      returnData.setVerticies(processVerticies(mesh));
      returnData.setTextureCoords(processTextureCoords(mesh));
      returnData.setIndicies(processIndices(mesh));

      int vertexListSize = returnData.getVerticies().length;
      if(ArrayUtils.isEmpty(returnData.getTextureCoords())) {
         returnData.setTextureCoords(ArrayUtils.toPrimitive(Collections.nCopies((vertexListSize / 3) * 2, 0f).toArray(Float[]::new)));
      }

      int materialIndex = mesh.mMaterialIndex();
      String materialId = "";
      if(materialIndex >= 0 && materialIndex < materialList.size()) {
         materialId = materialList.get(materialIndex).getId();
      }
      returnData.setMaterialId(materialId);
      return returnData;
   }

   private static float[] processVerticies(AIMesh mesh) {
      List<Float> verticies = new ArrayList<>();
      var aiVerticies = mesh.mVertices();
      while(aiVerticies.hasRemaining()) {
         var vertex = aiVerticies.get();
         verticies.add(vertex.x());
         verticies.add(vertex.y());
         verticies.add(vertex.z());
      }
      return ArrayUtils.toPrimitive(verticies.toArray(Float[]::new));
   }

   private static float[] processTextureCoords(AIMesh mesh) {
      List<Float> textureCoordinates = new ArrayList<>();
      var aiTextureCoords = mesh.mTextureCoords(0);
      int numTextureCoords = aiTextureCoords != null ? aiTextureCoords.remaining() : 0;
      for(int i = 0; i < numTextureCoords; i++) {
         var coord = aiTextureCoords.get();
         textureCoordinates.add(coord.x());
         textureCoordinates.add(1 - coord.y());
      }
      return ArrayUtils.toPrimitive(textureCoordinates.toArray(Float[]::new));
   }

   private static int[] processIndices(AIMesh mesh) {
      List<Integer> indices = new ArrayList<>();
      int numFaces = mesh.mNumFaces();
      var aiFaces = mesh.mFaces();
      for(int i = 0; i < numFaces; i++) {
         var face = aiFaces.get(i);
         IntBuffer buf = face.mIndices();
         while(buf.hasRemaining()) {
            indices.add(buf.get());
         }
      }
      return ArrayUtils.toPrimitive(indices.toArray(Integer[]::new));
   }
}
