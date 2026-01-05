package tv.memoryleakdeath.ascalondreams.sound;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.scene.Camera;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
   private static final Logger logger = LoggerFactory.getLogger(SoundManager.class);
   private final long context;
   private final long device;
   private final Map<String, SoundBuffer> soundBufferMap = new HashMap<>();
   private final Map<String, SoundSource> soundSourceMap = new HashMap<>();
   private SoundListener listener;
   private static SoundManager soundManager;

   private SoundManager() {
      this.device = ALC10.alcOpenDevice((ByteBuffer) null);
      if(device == MemoryUtil.NULL) {
         throw new IllegalStateException("Failed to open the default OpenAL device!");
      }
      ALCCapabilities deviceCapabilities = ALC.createCapabilities(device);
      this.context = ALC10.alcCreateContext(device, (IntBuffer) null);
      if(context == MemoryUtil.NULL) {
         throw new IllegalStateException("Failed to create OpenAL context!");
      }
      ALC10.alcMakeContextCurrent(context);
      AL.createCapabilities(deviceCapabilities);
   }

   public static SoundManager getInstance() {
      if(soundManager == null) {
         soundManager = new SoundManager();
      }
      return soundManager;
   }

   public void addSoundBuffer(String name, SoundBuffer buf) {
      soundBufferMap.put(name, buf);
   }

   public void addSoundSource(String name, SoundSource source) {
      soundSourceMap.put(name, source);
   }

   public void cleanup() {
      soundSourceMap.values().forEach(SoundSource::cleanup);
      soundSourceMap.clear();
      soundBufferMap.values().forEach(SoundBuffer::cleanup);
      soundBufferMap.clear();
      if(context != MemoryUtil.NULL) {
         ALC10.alcDestroyContext(context);
      }
      if(device != MemoryUtil.NULL) {
         ALC10.alcCloseDevice(device);
      }
   }

   public SoundSource getSoundSource(String name) {
      return soundSourceMap.get(name);
   }

   public void pause(String sourceId) {
      SoundSource source = getSoundSource(sourceId);
      if(source == null) {
         logger.warn("Unknown source: {}", sourceId);
         return;
      }
      source.pause();
   }

   public void play(String sourceId, String bufferId) {
      SoundSource source = getSoundSource(sourceId);
      if(source == null) {
         logger.warn("Unknown source: {}", sourceId);
         return;
      }
      SoundBuffer buffer = soundBufferMap.get(bufferId);
      if(buffer == null) {
         logger.warn("Unknown buffer: {}", bufferId);
         return;
      }
      source.setBuffer(buffer.getId());
      source.play();
   }

   public void setAttenuationModel(int model) {
      AL10.alDistanceModel(model);
   }

   public void setListener(SoundListener listener) {
      this.listener = listener;
   }

   public void stop(String sourceId) {
      SoundSource soundSource = soundSourceMap.get(sourceId);
      if(soundSource == null) {
         logger.warn("Unknown Source: {}", sourceId);
         return;
      }
      soundSource.stop();
   }

   public void updateListenerPosition(Camera camera) {
      Matrix4f viewMatrix = camera.getViewMatrix();
      listener.setPosition(camera.getPosition());
      Vector3f at = new Vector3f();
      viewMatrix.positiveZ(at).negate();
      var up = new Vector3f();
      viewMatrix.positiveY(up);
      listener.setOrientation(at, up);
   }
}
