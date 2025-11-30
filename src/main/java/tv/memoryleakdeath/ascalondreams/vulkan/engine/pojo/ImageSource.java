package tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo;

import java.nio.ByteBuffer;

public record ImageSource(ByteBuffer data, int width, int height, int channels) {
}
