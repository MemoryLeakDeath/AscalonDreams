package tv.memoryleakdeath.ascalondreams.animations;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.input.KeyboardInputCallback;
import tv.memoryleakdeath.ascalondreams.state.GameState;
import tv.memoryleakdeath.ascalondreams.state.StateMachine;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.Entity;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.EntityAnimation;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AnimationInputCallback implements KeyboardInputCallback {
   private static final Logger logger = LoggerFactory.getLogger(AnimationInputCallback.class);
   private static final Set<Integer> KEYS = Set.of(GLFW.GLFW_KEY_SPACE);
   private Set<Integer> pressedKeys;
   private StateMachine stateMachine = StateMachine.getInstance();
   private List<EntityAnimation> entityAnimationList;

   public AnimationInputCallback(VulkanScene scene) {
      this.entityAnimationList = scene.getEntities().values().stream()
              .flatMap(Collection::stream)
              .map(Entity::getEntityAnimation)
              .filter(Objects::nonNull)
              .toList();
   }

   @Override
   public boolean handles(Set<Integer> pressedKeys, int pressedModifiers, GameState state) {
      if(stateMachine.getCurrentGameState() == GameState.GUI) {
         return false;
      }

      // keyboard input handler
      boolean isHandled = KEYS.stream().anyMatch(pressedKeys::contains);
      if(isHandled) {
         this.pressedKeys = pressedKeys;
      }
      return isHandled;
   }

   @Override
   public void performAction(long deltaTimeNano) {
      if(pressedKeys.contains(GLFW.GLFW_KEY_SPACE)) {
         entityAnimationList.forEach(e -> e.setStarted(!e.isStarted()));
      }
   }

}
