package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModelCache {
   private Map<String, VulkanModel> modelMap = new HashMap<>();
   private static ModelCache modelCache;

   private ModelCache() {
   }

   public static ModelCache getInstance() {
      if(modelCache == null) {
         modelCache = new ModelCache();
      }
      return modelCache;
   }

   public void cleanup(LogicalDevice device) {
      modelMap.forEach((k, t) -> t.cleanup(device));
      modelMap.clear();
   }

   public VulkanModel getModel(String name) {
      return modelMap.get(name);
   }

   public Map<String, VulkanModel> getModelMap() {
      return modelMap;
   }

   public void loadModels(LogicalDevice device, List<VulkanModel> modelList, CommandPool pool, BaseDeviceQueue queue) {
      modelMap = modelList.stream().collect(Collectors.toMap(VulkanModel::getId, Function.identity()));
      var command = new CommandBuffer(device, pool, true, true);
      List<TransferBuffer> transferBuffers = modelList.stream().flatMap(model -> model.getTransferBuffers().stream()).toList();
      command.beginRecording();
      transferBuffers.forEach(b -> b.recordTransferCommand(command));
      command.endRecording();
      command.submitAndWait(device, queue);
      command.cleanup(device, pool);
      transferBuffers.forEach(b -> b.cleanupSourceBuffer(device));
   }
}
