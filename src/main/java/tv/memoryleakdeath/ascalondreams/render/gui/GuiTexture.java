package tv.memoryleakdeath.ascalondreams.render.gui;

import java.security.SecureRandom;

public record GuiTexture(long id, String texturePath) {
   public GuiTexture(String texturePath) {
      this(generateId(), texturePath);
   }

   private static long generateId() {
      SecureRandom rand = new SecureRandom();
      long id = Math.abs(rand.nextLong());
      if(id == 0) {
         id += 1;
      }
      return id;
   }
}
