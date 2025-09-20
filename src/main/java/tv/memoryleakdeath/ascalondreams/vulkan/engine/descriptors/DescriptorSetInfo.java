package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors;

import java.util.List;

public record DescriptorSetInfo(List<DescriptorSet> setList, int poolPosition) {
}
