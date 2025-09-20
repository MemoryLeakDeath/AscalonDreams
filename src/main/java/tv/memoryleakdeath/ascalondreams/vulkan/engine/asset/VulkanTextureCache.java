package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.apache.commons.collections4.map.ListOrderedMap;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;

import java.util.LinkedHashMap;
import java.util.Map;

public class VulkanTextureCache {
    private ListOrderedMap<String, VulkanTexture> cache = new ListOrderedMap<>();

    public VulkanTexture createTexture(LogicalDevice device, String fullPathFile, int format) {
        VulkanTexture texture = cache.get(fullPathFile);
        if(texture == null) {
            texture = new VulkanTexture(device, fullPathFile, format);
            cache.put(fullPathFile, texture);
        }
        return texture;
    }

    public int getTextureCacheIndex(String texturePath) {
        return cache.indexOf(texturePath);
    }

    public void cleanup() {
        cache.forEach((k,v) -> v.cleanup());
        cache.clear();
    }
}
