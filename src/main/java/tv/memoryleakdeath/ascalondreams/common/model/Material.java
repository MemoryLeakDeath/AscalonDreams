package tv.memoryleakdeath.ascalondreams.common.model;

import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import tv.memoryleakdeath.ascalondreams.common.asset.BaseTexture;

public class Material {
   public static final Vector4f DEFAULT_COLOR = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);

   private Vector4f ambientColor = DEFAULT_COLOR;
   private Vector4f diffuseColor = DEFAULT_COLOR;
   private int materialIdx = 0;
   private String normalMapPath;
   private float reflectance;
   private Vector4f specularColor = DEFAULT_COLOR;
   private String textureFilePath;

   public Vector4f getAmbientColor() {
      return ambientColor;
   }

   public void setAmbientColor(Vector4f ambientColor) {
      this.ambientColor = ambientColor;
   }

   public Vector4f getDiffuseColor() {
      return diffuseColor;
   }

   public void setDiffuseColor(Vector4f diffuseColor) {
      this.diffuseColor = diffuseColor;
   }

   public int getMaterialIdx() {
      return materialIdx;
   }

   public void setMaterialIdx(int materialIdx) {
      this.materialIdx = materialIdx;
   }

   public String getNormalMapPath() {
      return normalMapPath;
   }

   public void setNormalMapPath(String normalMapPath) {
      this.normalMapPath = normalMapPath;
   }

   public float getReflectance() {
      return reflectance;
   }

   public void setReflectance(float reflectance) {
      this.reflectance = reflectance;
   }

   public Vector4f getSpecularColor() {
      return specularColor;
   }

   public void setSpecularColor(Vector4f specularColor) {
      this.specularColor = specularColor;
   }

   public String getTextureFilePath() {
      return textureFilePath;
   }

   public void setTextureFilePath(String textureFilePath) {
      this.textureFilePath = textureFilePath;
   }

   public boolean hasTexture() {
      return StringUtils.isNotBlank(textureFilePath);
   }
}
