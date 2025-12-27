package tv.memoryleakdeath.ascalondreams.sound;

import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class SoundBuffer {
   private final int id;
   private final ShortBuffer pcmBuffer;

   public SoundBuffer(String filePath) {
      this.id = AL10.alGenBuffers();
      try(STBVorbisInfo info = STBVorbisInfo.malloc()) {
         this.pcmBuffer = readVorbis(filePath, info);

         // copy to buffer
         AL10.alBufferData(id, info.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, pcmBuffer, info.sample_rate());
      }
   }

   public void cleanup() {
      AL10.alDeleteBuffers(id);
      if(pcmBuffer != null) {
         MemoryUtil.memFree(pcmBuffer);
      }
   }

   private ShortBuffer readVorbis(String filePath, STBVorbisInfo info) {
      try(var stack = MemoryStack.stackPush()) {
         IntBuffer error = stack.mallocInt(1);
         long decoder = STBVorbis.stb_vorbis_open_filename(filePath, error, null);
         if(decoder == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to open ogg vorbis file: %s, error: %s".formatted(filePath, error.get(0)));
         }

         STBVorbis.stb_vorbis_get_info(decoder, info);
         int channels = info.channels();
         int sampleLength = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
         ShortBuffer result = MemoryUtil.memAllocShort(sampleLength * channels);
         result.limit(STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, result) * channels);
         STBVorbis.stb_vorbis_close(decoder);
         return result;
      }
   }

   public int getId() {
      return id;
   }
}
