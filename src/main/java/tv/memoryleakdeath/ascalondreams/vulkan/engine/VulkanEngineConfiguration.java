package tv.memoryleakdeath.ascalondreams.vulkan.engine;

public class VulkanEngineConfiguration {
   private int logicUpdatesPerSecond = 30;
   private int framesPerSecond = 60;
   private long logicFrameTime = 1_000_000_000L / logicUpdatesPerSecond;
   private long fpsFrameTime = 1_000_000_000L / framesPerSecond;
   private float movementIncrement = 0.02f;
   private static VulkanEngineConfiguration instance;
   private String applicationName = "AscalonDreams";
   private int applicationVersion = 1;
   private int engineVersion = 0;
   private int maxInFlightCommandBuffers = 2;

   private VulkanEngineConfiguration() {
   }

   public static VulkanEngineConfiguration getInstance() {
      if(instance == null) {
         instance = new VulkanEngineConfiguration();
      }
      return instance;
   }

   public int getLogicUpdatesPerSecond() {
      return logicUpdatesPerSecond;
   }

   public int getFramesPerSecond() {
      return framesPerSecond;
   }

   public long getLogicFrameTime() {
      return logicFrameTime;
   }

   public long getFpsFrameTime() {
      return fpsFrameTime;
   }

   public float getMovementIncrement() {
      return movementIncrement;
   }

   public String getApplicationName() {
      return applicationName;
   }

   public int getApplicationVersion() {
      return applicationVersion;
   }

   public int getEngineVersion() {
      return engineVersion;
   }

   public int getMaxInFlightCommandBuffers() {
      return maxInFlightCommandBuffers;
   }
}
