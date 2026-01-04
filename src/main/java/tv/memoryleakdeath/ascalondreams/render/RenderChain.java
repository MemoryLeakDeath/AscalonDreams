package tv.memoryleakdeath.ascalondreams.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.animations.AnimationRenderer;
import tv.memoryleakdeath.ascalondreams.gui.GuiRender;
import tv.memoryleakdeath.ascalondreams.lighting.LightingRenderer;
import tv.memoryleakdeath.ascalondreams.postprocess.PostProcessingRenderer;
import tv.memoryleakdeath.ascalondreams.shadow.ShadowRenderer;
import tv.memoryleakdeath.ascalondreams.swapchain.SwapChainRender;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class RenderChain {
   private static final Logger logger = LoggerFactory.getLogger(RenderChain.class);
   private static final Class[] INIT_CHAIN = new Class[] {SceneRenderer.class,
   ShadowRenderer.class, LightingRenderer.class, PostProcessingRenderer.class, GuiRender.class, SwapChainRender.class,
   AnimationRenderer.class};
   private static Map<Class<?>, Object> rendererInstances = new HashMap<>();

   public static void initChain() {
      for (Class<?> renderer : INIT_CHAIN) {
         try {
            Object rendererInstance = renderer.getDeclaredConstructor().newInstance();
            rendererInstances.put(renderer, renderer.getMethod("getInstance").invoke(rendererInstance));
         } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                  NoSuchMethodException e) {
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
   }
}
