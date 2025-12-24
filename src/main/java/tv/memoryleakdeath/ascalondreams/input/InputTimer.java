package tv.memoryleakdeath.ascalondreams.input;

public class InputTimer {
   private static long checkpoint;
   private static InputTimer timer;

   private InputTimer() {
   }

   public static InputTimer getInstance() {
      if(timer == null) {
         timer = new InputTimer();
      }
      return timer;
   }

   public void tick() {
      checkpoint = System.nanoTime();
   }

   public long delta() {
      return System.nanoTime() - checkpoint;
   }
}
