package tv.memoryleakdeath.ascalondreams.input;

import tv.memoryleakdeath.ascalondreams.state.GameState;

import java.util.Set;

public interface KeyboardInputCallback {
   boolean handles(Set<Integer> pressedKeys, int pressedModifiers, GameState state);

   void performAction(long deltaTimeMillis);
}
