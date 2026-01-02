package tv.memoryleakdeath.ascalondreams.vulkan.engine.scene;

public class EntityAnimation {
   private int animationIndex;
   private int currentFrame;
   private long frameStartTime;
   private boolean started;

   public EntityAnimation(boolean started, int animationIndex, int currentFrame) {
      this.started = started;
      this.animationIndex = animationIndex;
      this.currentFrame = currentFrame;
      if(started) {
         this.frameStartTime = System.currentTimeMillis();
      }
   }

   public int getAnimationIndex() {
      return animationIndex;
   }

   public void setAnimationIndex(int animationIndex) {
      this.animationIndex = animationIndex;
   }

   public int getCurrentFrame() {
      return currentFrame;
   }

   public void setCurrentFrame(int currentFrame) {
      this.currentFrame = currentFrame;
   }

   public boolean isStarted() {
      return started;
   }

   public void setStarted(boolean started) {
      this.started = started;
      if(started) {
         this.frameStartTime = System.currentTimeMillis();
      }
   }

   public long getFrameStartTime() {
      return frameStartTime;
   }
}
