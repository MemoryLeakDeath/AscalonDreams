package tv.memoryleakdeath.ascalondreams.asset;

import java.io.Serializable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Model implements Serializable {
    private static final long serialVersionUID = 1L;

    private FloatBuffer verticies;
    private IntBuffer indicies;
    private float currentRotation = 0f;

    public float getCurrentRotation() {
        return currentRotation;
    }

    public void setCurrentRotation(float currentRotation) {
        this.currentRotation = currentRotation;
    }

    public FloatBuffer getVerticies() {
        return verticies;
    }

    public IntBuffer getIndicies() {
        return indicies;
    }

    public Model(FloatBuffer verticies, IntBuffer indicies) {
        super();
        this.verticies = verticies;
        this.indicies = indicies;
    }

}
