package tv.memoryleakdeath.ascalondreams.util;

public class MathUtils {
   private MathUtils() {
   }

   public static int calculateMipLevels(int width, int height) {
      return (int) Math.floor(log2(Math.min(width, height))) + 1;
   }

   public static double log2(int n) {
      return Math.log(n) / Math.log(2);
   }
}
