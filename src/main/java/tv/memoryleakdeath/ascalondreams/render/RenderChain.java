package tv.memoryleakdeath.ascalondreams.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.animations.AnimationRenderer;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.gui.GuiRender;
import tv.memoryleakdeath.ascalondreams.lighting.LightingRenderer;
import tv.memoryleakdeath.ascalondreams.postprocess.PostProcessingRenderer;
import tv.memoryleakdeath.ascalondreams.shadow.ShadowRenderer;
import tv.memoryleakdeath.ascalondreams.swapchain.SwapChainRender;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderChain {
   private static final Logger logger = LoggerFactory.getLogger(RenderChain.class);
   private static final List<Class<? extends Renderer>> INIT_CHAIN = List.of(
           SceneRenderer.class, ShadowRenderer.class, LightingRenderer.class, PostProcessingRenderer.class,
           GuiRender.class, SwapChainRender.class, AnimationRenderer.class );
   private static final List<Class<? extends Renderer>> LOAD_CHAIN = List.of(
           SceneRenderer.class, ShadowRenderer.class, GuiRender.class, AnimationRenderer.class );
   private static final List<Class<? extends Renderer>> RENDER_CHAIN = List.of(AnimationRenderer.class,
           SceneRenderer.class, ShadowRenderer.class, LightingRenderer.class, PostProcessingRenderer.class,
           GuiRender.class, SwapChainRender.class);
   private static final List<Class<? extends Renderer>> RESIZE_CHAIN = List.of(SceneRenderer.class,
           LightingRenderer.class, PostProcessingRenderer.class, GuiRender.class, SwapChainRender.class);
   private static Map<Class<?>, Renderer> rendererInstances = new HashMap<>();

   public static void initChain() {
      for (Class<?> renderer : INIT_CHAIN) {
         try {
            Renderer rendererInstance = (Renderer) renderer.getMethod("getInstance").invoke(null);
            rendererInstances.put(renderer, rendererInstance);
         } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
         }
      }
   }

   public static <T> T getRendererInstance(Class<T> rendererClass) {
      return (T) rendererInstances.get(rendererClass);
   }

   public static void cleanup() {
      for (Object instance : rendererInstances.values()) {
         try {
            instance.getClass().getMethod("cleanup").invoke(instance);
         } catch (IllegalAccessException | InvocationTargetException |
                  NoSuchMethodException e) {
            throw new RuntimeException(e);
         }
      }
      rendererInstances.clear();
   }

   public static void loadChain() {
      for(Class<?> renderer : LOAD_CHAIN) {
         Renderer instance = rendererInstances.get(renderer);
         instance.load();
      }
   }

   public static void renderChain(CommandBuffer commandBuffer, int currentFrame, int imageIndex) {
      for(Class<?> renderer : RENDER_CHAIN) {
         Renderer instance = rendererInstances.get(renderer);
         instance.render(commandBuffer, currentFrame, imageIndex);
      }
   }

   public static void resizeChain() {
      for(Class<?> renderer : RESIZE_CHAIN) {
         Renderer instance = rendererInstances.get(renderer);
         instance.resize();
      }
   }
}
