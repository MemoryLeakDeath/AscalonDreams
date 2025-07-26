package tv.memoryleakdeath.ascalondreams.common.model;

import java.util.List;

public class Mesh {
   private float[] vertices;
   private List<float[]> texCoords;
   private int[] indexes;
   private float[] colors;
   private int numVertices;

   public Mesh(float[] vertices, List<float[]> texCoords, int[] indexes, float[] colors) {
      this.vertices = vertices;
      this.texCoords = texCoords;
      this.indexes = indexes;
      this.colors = colors;
   }

   public float[] getVertices() {
      return vertices;
   }

   public void setVertices(float[] vertices) {
      this.vertices = vertices;
   }

   public List<float[]> getTexCoords() {
      return texCoords;
   }

   public void setTexCoords(List<float[]> texCoords) {
      this.texCoords = texCoords;
   }

   public int[] getIndexes() {
      return indexes;
   }

   public void setIndexes(int[] indexes) {
      this.indexes = indexes;
   }

   public float[] getColors() {
      return colors;
   }

   public void setColors(float[] colors) {
      this.colors = colors;
   }

   public int getNumVertices() {
      return numVertices;
   }

   public void setNumVertices(int numVertices) {
      this.numVertices = numVertices;
   }
}
