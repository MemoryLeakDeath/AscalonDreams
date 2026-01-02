package tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AINodeAnim;
import org.lwjgl.assimp.AIQuatKey;
import org.lwjgl.assimp.AIQuaternion;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVectorKey;
import org.lwjgl.assimp.AIVertexWeight;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tv.memoryleakdeath.ascalondreams.util.ObjectMapperInstance;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.AnimatedFrame;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.Animation;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.Bone;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.Node;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMaterial;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMeshData;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelConverter {
   private static final Logger logger = LoggerFactory.getLogger(ModelConverter.class);
   private static final Pattern GET_TEXTURE_ID = Pattern.compile("\\*([0-9]+)");
   private static final int FLAGS = Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_JoinIdenticalVertices |
           Assimp.aiProcess_Triangulate | Assimp.aiProcess_FixInfacingNormals | Assimp.aiProcess_CalcTangentSpace;
   private static final int NON_ANIMATED_FLAGS = Assimp.aiProcess_PreTransformVertices;
   private static final int ANIMATED_FLAGS = Assimp.aiProcess_LimitBoneWeights;
   private static final int MAX_JOINTS_MATRICES_LIST = 150;
   private static final int MAX_WEIGHTS = 4;

   private ModelConverter() {
   }

   public static void convertModelFiles(String modelFile, String animationFile) throws IOException {
      logger.debug("Checking model file: {}", modelFile);
      File file = new File(modelFile);
      if (!file.exists()) {
         throw new RuntimeException("Model file: %s does not exist!".formatted(modelFile));
      }
      if(StringUtils.isNotBlank(animationFile) && !new File(animationFile).exists()) {
         throw new RuntimeException("Animation file: %s does not exist!".formatted(animationFile));
      }

      int loadFlags = FLAGS;
      if (StringUtils.isNotBlank(animationFile)) {
         loadFlags |= ANIMATED_FLAGS;
      } else {
         loadFlags |= NON_ANIMATED_FLAGS;
      }
      File parentDirectory = file.getParentFile();
      convertModelFile(modelFile, loadFlags, parentDirectory.getPath(), animationFile);
   }

   public static void convertModelFile(String modelFile, int loadFlags, String parentDirPath, String animationFile) throws IOException {
      logger.debug("Processing convertedModel file: {}", modelFile);
      AIScene scene = Assimp.aiImportFile(modelFile, loadFlags);
      if(scene == null) {
         throw new RuntimeException("Error loading convertedModel: %s".formatted(modelFile));
      }

      AIScene animatedScene = null;
      if(StringUtils.isNotBlank(animationFile)) {
         animatedScene = Assimp.aiImportFile(animationFile, loadFlags);
      }

      String modelId = FilenameUtils.getBaseName(modelFile);
      ConvertedModel convertedModel = new ConvertedModel();
      convertedModel.setId(modelId);

      int numMaterials = scene.mNumMaterials();
      logger.debug("Number of materials: {}", numMaterials);
      List<VulkanMaterial> parsedMaterials = new ArrayList<>();
      for (int i = 0; i < numMaterials; i++) {
         var aiMaterial = AIMaterial.create(scene.mMaterials().get(i));
         parsedMaterials.add(processMaterial(scene, aiMaterial, modelId, parentDirPath, i));
      }

      int numMeshes = scene.mNumMeshes();
      logger.debug("Number of meshes: {}", numMeshes);
      PointerBuffer aiMeshes = scene.mMeshes();
      List<VulkanMeshData> meshDataList = new ArrayList<>();
      for (int i = 0; i < numMeshes; i++) {
         AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
         meshDataList.add(processMesh(aiMesh, parsedMaterials, i));
      }

      List<AnimationMeshData> animationMeshData = null;
      List<Animation> animations = null;
      if(animatedScene != null) {
         int numAnimations = animatedScene.mNumAnimations();
         if (numAnimations > 0) {
            logger.debug("Processing animations....");
            List<Bone> bones = new ArrayList<>();
            animationMeshData = new ArrayList<>();
            for (int i = 0; i < numMeshes; i++) {
               AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
               animationMeshData.add(processBones(aiMesh, bones));
            }

            Node rootNode = buildNodesTree(animatedScene.mRootNode(), null);
            Matrix4f globalInverseTransform = toMatrix(animatedScene.mRootNode().mTransformation()).invert();
            animations = processAnimations(animatedScene, bones, rootNode, globalInverseTransform);
         }
      }

      convertedModel.setMaterials(parsedMaterials);
      convertedModel.setMeshData(meshDataList);
      convertedModel.setTexturePath(parentDirPath);
      convertedModel.setAnimationMeshData(animationMeshData);
      convertedModel.setAnimations(animations);

      File outputFile = new File("%s/%s.json".formatted(convertedModel.getTexturePath(), modelId));
      ObjectMapper mapper = ObjectMapperInstance.getInstance();
      mapper.writeValue(outputFile, convertedModel);
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
      String normalTexturePath = processTexture(scene, material, textureDir, Assimp.aiTextureType_NORMALS);
      String roughTexturePath = processTexture(scene, material, textureDir, Assimp.AI_MATKEY_GLTF_PBRMETALLICROUGHNESS_METALLICROUGHNESS_TEXTURE);

      float[] metallicArray = new float[]{0f};
      int[] pMax = new int[]{1};
      result = Assimp.aiGetMaterialFloatArray(material, Assimp.AI_MATKEY_METALLIC_FACTOR, Assimp.aiTextureType_NONE, 0, metallicArray, pMax);
      if(result != Assimp.aiReturn_SUCCESS) {
         metallicArray[0] = 0f;
      }

      float[] roughnessArray = new float[]{0f};
      result = Assimp.aiGetMaterialFloatArray(material, Assimp.AI_MATKEY_ROUGHNESS_FACTOR, Assimp.aiTextureType_NONE, 0, roughnessArray, pMax);
      if(result != Assimp.aiReturn_SUCCESS) {
         roughnessArray[0] = 1.0f;
      }

      return new VulkanMaterial("%s-mat-%d".formatted(modelId, index), diffuseTexturePath, diffuseVec, normalTexturePath,
              roughTexturePath, roughnessArray[0], metallicArray[0]);
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
         if(StringUtils.isNotBlank(texturePath)) {
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
      float[] normals = processNormals(mesh);
      returnData.setNormals(normals);
      returnData.setTangents(processTangents(mesh, normals));
      returnData.setBiTangents(processBitangents(mesh, normals));
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

      String meshId = "%s_%d".formatted(mesh.mName().dataString(), position);
      returnData.setId(meshId);

      return returnData;
   }

   private static float[] processVerticies(AIMesh mesh) {
      List<Float> verticies = new ArrayList<>();
      var aiVerticies = mesh.mVertices();
      logger.debug("Number of Verticies: {}", mesh.mNumVertices());
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
      logger.debug("Number of faces: {}", numFaces);
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

   private static float[] processBitangents(AIMesh aiMesh, float[] normals) {
      List<Float> biTangents = new ArrayList<>();
      var aiBitangents = aiMesh.mBitangents();
      while(aiBitangents != null && aiBitangents.hasRemaining()) {
         var aiBitangent = aiBitangents.get();
         biTangents.add(aiBitangent.x());
         biTangents.add(aiBitangent.y());
         biTangents.add(aiBitangent.z());
      }

      // assimp may not calculate tangents with models that do not have texture coords.  Just create empty values
      if(biTangents.isEmpty()) {
         biTangents = new ArrayList<>(Collections.nCopies(normals.length, 0f));
      }
      return ArrayUtils.toPrimitive(biTangents.toArray(Float[]::new));
   }

   private static float[] processNormals(AIMesh aiMesh) {
      List<Float> normals = new ArrayList<>();
      var aiNormals = aiMesh.mNormals();
      while(aiNormals != null && aiNormals.hasRemaining()) {
         var aiNormal = aiNormals.get();
         normals.add(aiNormal.x());
         normals.add(aiNormal.y());
         normals.add(aiNormal.z());
      }
      return ArrayUtils.toPrimitive(normals.toArray(Float[]::new));
   }

   private static float[] processTangents(AIMesh aiMesh, float[] normals) {
      List<Float> tangents = new ArrayList<>();
      var aiTangents = aiMesh.mTangents();
      while(aiTangents != null && aiTangents.hasRemaining()) {
         var aiTangent = aiTangents.get();
         tangents.add(aiTangent.x());
         tangents.add(aiTangent.y());
         tangents.add(aiTangent.z());
      }

      // assimp may not calc tangents with models that do not have texture coords.  Just create empty values
      if(tangents.isEmpty()) {
         tangents = new ArrayList<>(Collections.nCopies(normals.length, 0f));
      }
      return ArrayUtils.toPrimitive(tangents.toArray(Float[]::new));
   }

   private static void buildFrameMatrices(AIAnimation aiAnimation, List<Bone> bones, AnimatedFrame animatedFrame,
                                          int frame, Node node, Matrix4f parentTransform, Matrix4f globalInverseTransform) {
      String nodeName = node.getName();
      AINodeAnim aiNodeAnim = findAIAnimationNode(aiAnimation, nodeName);
      Matrix4f nodeTransform = node.getNodeTransform();
      if(aiNodeAnim != null) {
         nodeTransform = buildNodeTransformationMatrix(aiNodeAnim, frame);
      }
      Matrix4f nodeGlobalTransform = new Matrix4f(parentTransform).mul(nodeTransform);

      List<Bone> affectedBones = bones.stream().filter(b -> b.name().equals(nodeName)).toList();
      affectedBones.forEach(ab -> {
         var boneTransform = new Matrix4f(globalInverseTransform).mul(nodeGlobalTransform)
                 .mul(ab.offsetMatrix());
         animatedFrame.jointMatrices().set(ab.id(), boneTransform);
      });

      node.getChildren().forEach(child -> buildFrameMatrices(aiAnimation, bones, animatedFrame, frame, child, nodeGlobalTransform,
              globalInverseTransform));
   }

   private static Matrix4f buildNodeTransformationMatrix(AINodeAnim aiNodeAnim, int frame) {
      AIVectorKey.Buffer positionKeys = aiNodeAnim.mPositionKeys();
      AIVectorKey.Buffer scalingKeys = aiNodeAnim.mScalingKeys();
      AIQuatKey.Buffer rotationKeys = aiNodeAnim.mRotationKeys();

      AIVectorKey aiVectorKey;
      AIVector3D vector3D;
      Matrix4f nodeTransform = new Matrix4f();
      int numPositions = aiNodeAnim.mNumPositionKeys();
      if(numPositions > 0) {
         aiVectorKey = positionKeys.get(Math.min(numPositions - 1, frame));
         vector3D = aiVectorKey.mValue();
         nodeTransform.translate(vector3D.x(), vector3D.y(), vector3D.z());
      }

      int numRotations = aiNodeAnim.mNumRotationKeys();
      if(numRotations > 0) {
         AIQuatKey quatKey = rotationKeys.get(Math.min(numRotations - 1, frame));
         AIQuaternion aiQuaternion = quatKey.mValue();
         Quaternionf quaternion = new Quaternionf(aiQuaternion.x(), aiQuaternion.y(), aiQuaternion.z(), aiQuaternion.w());
         nodeTransform.rotate(quaternion);
      }

      int numScalingKeys = aiNodeAnim.mNumScalingKeys();
      if(numScalingKeys > 0) {
         aiVectorKey = scalingKeys.get(Math.min(numScalingKeys - 1, frame));
         vector3D = aiVectorKey.mValue();
         nodeTransform.scale(vector3D.x(), vector3D.y(), vector3D.z());
      }
      return nodeTransform;
   }

   private static Node buildNodesTree(AINode aiNode, Node parentNode) {
      String nodeName = aiNode.mName().dataString();
      Node node = new Node(nodeName, parentNode, toMatrix(aiNode.mTransformation()));

      int numChildren = aiNode.mNumChildren();
      PointerBuffer aiChildren = aiNode.mChildren();
      for(int i = 0; i < numChildren; i++) {
         AINode aiChildNode = AINode.create(aiChildren.get(i));
         Node childNode = buildNodesTree(aiChildNode, node);
         node.addChild(childNode);
      }
      return node;
   }

   private static int calculateAnimationMaxFrames(AIAnimation aiAnimation) {
      int maxFrames = 0;
      int numNodeAnimations = aiAnimation.mNumChannels();
      PointerBuffer aiChannels = aiAnimation.mChannels();
      for(int i = 0; i < numNodeAnimations; i++) {
         AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
         int numFrames = Math.max(Math.max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys()),
                 aiNodeAnim.mNumRotationKeys());
         maxFrames = Math.max(maxFrames, numFrames);
      }
      return maxFrames;
   }

   private static AINodeAnim findAIAnimationNode(AIAnimation aiAnimation, String nodeName) {
      AINodeAnim result = null;
      int numAnimationNodes = aiAnimation.mNumChannels();
      PointerBuffer aiChannels = aiAnimation.mChannels();
      for(int i = 0; i < numAnimationNodes; i++) {
         AINodeAnim aiNodeAnim = AINodeAnim.create(aiChannels.get(i));
         if(nodeName.equals(aiNodeAnim.mNodeName().dataString())) {
            result = aiNodeAnim;
            break;
         }
      }
      return result;
   }

   private static void padJointList(List<Matrix4f> list, int maxSize) {
      for(int i = list.size(); i < maxSize; i++) {
         list.add(new Matrix4f());
      }
   }
   private static List<Animation> processAnimations(AIScene aiScene, List<Bone> bones, Node rootNode,
                                                    Matrix4f globalInverseTransform) {
      List<Animation> animations = new ArrayList<>();
      int numAnimations = aiScene.mNumAnimations();
      PointerBuffer aiAnimations = aiScene.mAnimations();
      for(int i = 0; i < numAnimations; i++) {
         AIAnimation aiAnimation = AIAnimation.create(aiAnimations.get(i));
         int maxFrames = calculateAnimationMaxFrames(aiAnimation);
         float frameMillis = (float) (aiAnimation.mDuration() / aiAnimation.mTicksPerSecond());
         List<AnimatedFrame> frames = new ArrayList<>();
         animations.add(new Animation(aiAnimation.mName().dataString(), frameMillis, frames));

         for(int j = 0; j < maxFrames; j++) {
            List<Matrix4f> jointMatrices = new ArrayList<>();
            AnimatedFrame animatedFrame = new AnimatedFrame(jointMatrices);
            padJointList(jointMatrices, MAX_JOINTS_MATRICES_LIST);
            buildFrameMatrices(aiAnimation, bones, animatedFrame, j, rootNode, rootNode.getNodeTransform(), globalInverseTransform);
            frames.add(animatedFrame);
         }
      }
      return animations;
   }

   private static AnimationMeshData processBones(AIMesh aiMesh, List<Bone> bones) {
      List<Integer> boneIds = new ArrayList<>();
      List<Float> weights = new ArrayList<>();

      Map<Integer, List<VertexWeight>> weightMap = new HashMap<>();
      int numBones = aiMesh.mNumBones();
      PointerBuffer aiBones = aiMesh.mBones();
      for(int i = 0; i < numBones; i++) {
         AIBone aiBone = AIBone.create(aiBones.get(i));
         int id = bones.size();
         Bone bone = new Bone(id, aiBone.mName().dataString(), toMatrix(aiBone.mOffsetMatrix()));
         bones.add(bone);

         int numWeights = aiBone.mNumWeights();
         AIVertexWeight.Buffer aiWeights = aiBone.mWeights();
         for(int j = 0; j < numWeights; j++) {
            AIVertexWeight aiVertexWeight = aiWeights.get(j);
            VertexWeight vw = new VertexWeight(bone.id(), aiVertexWeight.mVertexId(), aiVertexWeight.mWeight());
            List<VertexWeight> vertexWeights = weightMap.get(vw.vertexId());
            if(vertexWeights == null) {
               vertexWeights = new ArrayList<>();
               weightMap.put(vw.vertexId(), vertexWeights);
            }
            vertexWeights.add(vw);
         }
      }

      int numVertices = aiMesh.mNumVertices();
      for(int i = 0; i < numVertices; i++) {
         List<VertexWeight> vertexWeights = weightMap.get(i);
         int size = vertexWeights != null ? vertexWeights.size() : 0;
         for(int j = 0; j < MAX_WEIGHTS; j++) {
            if(j < size) {
               VertexWeight vw = vertexWeights.get(j);
               weights.add(vw.weight());
               boneIds.add(vw.boneId());
            } else {
               weights.add(0f);
               boneIds.add(0);
            }
         }
      }

      return new AnimationMeshData(weights, boneIds);
   }

   private static Matrix4f toMatrix(AIMatrix4x4 aiMatrix4x4) {
      Matrix4f result = new Matrix4f();
      result.m00(aiMatrix4x4.a1());
      result.m10(aiMatrix4x4.a2());
      result.m20(aiMatrix4x4.a3());
      result.m30(aiMatrix4x4.a4());
      result.m01(aiMatrix4x4.b1());
      result.m11(aiMatrix4x4.b2());
      result.m21(aiMatrix4x4.b3());
      result.m31(aiMatrix4x4.b4());
      result.m02(aiMatrix4x4.c1());
      result.m12(aiMatrix4x4.c2());
      result.m22(aiMatrix4x4.c3());
      result.m32(aiMatrix4x4.c4());
      result.m03(aiMatrix4x4.d1());
      result.m13(aiMatrix4x4.d2());
      result.m23(aiMatrix4x4.d3());
      result.m33(aiMatrix4x4.d4());
      return result;
   }
}
