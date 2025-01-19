package tv.memoryleakdeath.ascalondreams.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class KeyboardCallback {
    private static final Logger logger = LoggerFactory.getLogger(KeyboardCallback.class);

    private List<UserInputCallback> inputCallbacks = new ArrayList<>();

    public KeyboardCallback addHandler(UserInputCallback callback) {
        inputCallbacks.add(callback);
        return this;
    }

    public void process() {
        inputCallbacks.forEach(UserInputCallback::performAction);
    }
}
