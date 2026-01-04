package tv.memoryleakdeath.ascalondreams.util;

public class VulkanConstants {
   public static final int FLOAT_SIZE = 4;
   public static final int INT_SIZE = 4;
   public static final int PTR_SIZE = 8;
   public static final int SHORT_SIZE = 2;
   public static final int MAT4X4_SIZE = 16 * FLOAT_SIZE;
   public static final int VEC2_SIZE = 2 * FLOAT_SIZE;
   public static final int VEC3_SIZE = 3 * FLOAT_SIZE;
   public static final int VEC4_SIZE = 4 * FLOAT_SIZE;
   public static final int MAX_IN_FLIGHT = 2;

   private VulkanConstants() {}
}
