package tv.memoryleakdeath.ascalondreams.input;

import org.joml.Vector2f;

public interface MouseInputCallback {
   boolean handles(Vector2f currentCursorPosition, Vector2f deltaCursorPosition, boolean[] buttonsPressed, int modifiersPressed);
   void performAction(long deltaTimeMillis);
}
