package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor;

import java.util.List;

public record DescriptorSetInfo(List<DescriptorSet> descriptorSets, int poolPosition) {
}
