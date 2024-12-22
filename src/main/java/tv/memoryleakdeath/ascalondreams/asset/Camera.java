package tv.memoryleakdeath.ascalondreams.asset;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {
    private Vector3f direction = new Vector3f();
    private Vector3f position = new Vector3f();
    private Vector2f rotation = new Vector2f();
    private Vector3f right = new Vector3f();
    private Vector3f up = new Vector3f();
    private Matrix4f viewMatrix = new Matrix4f();

    // orbiting camera stuff
    private Vector3f cameraTarget = new Vector3f();
    private static final float ORBIT_DISTANCE = 4.0f;
    private static final float PI_OVER_TWO = (float) Math.PI / 2;
    private static final float THREE_PI_OVER_TWO = 3 * (float) Math.PI / 2;

    public Vector3f getDirection() {
        return direction;
    }

    public void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        recalculate();
    }

    public void setRotation(float x, float y) {
        rotation.set(x, y);
        recalculate();
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    private void recalculate() {
        viewMatrix.rotationX(rotation.x).rotationY(rotation.y).translation(-position.x, -position.y, -position.z);
    }

    public void addRotation(float x, float y) {
        rotation.add(x, y);
        recalculate();
    }

    public void moveBackwards(float inc) {
        viewMatrix.positiveZ(direction).negate().mul(inc);
        position.sub(direction);
        recalculate();
    }

    public void moveDown(float inc) {
        viewMatrix.positiveY(up).mul(inc);
        position.sub(up);
        recalculate();
    }

    public void moveLeft(float inc) {
        viewMatrix.positiveX(right).mul(inc);
        position.sub(right);
        recalculate();
    }

    public void moveRight(float inc) {
        viewMatrix.positiveX(right).mul(inc);
        position.add(right);
        recalculate();
    }

    public void moveUp(float inc) {
        viewMatrix.positiveY(up).mul(inc);
        position.add(up);
        recalculate();
    }

    public Vector3f getCameraTarget() {
        return cameraTarget;
    }

    public void setCameraTarget(Vector3f cameraTarget) {
        this.cameraTarget = cameraTarget;
    }

    private void updateOrbitPosition() {
        float yaw = (float) Math.toRadians(rotation.y);
        float pitch = (float) Math.toRadians(rotation.x);

        float distance = (float) (ORBIT_DISTANCE * Math.cos(pitch));
        float x = (float) (distance * Math.sin(yaw));
        float y = (float) (ORBIT_DISTANCE * Math.sin(pitch));
        float z = (float) (distance * Math.cos(yaw));

        position.set(cameraTarget.x + x, cameraTarget.y + y, cameraTarget.z + z);

        Vector3f cameraUpVector = calculateCameraUpVector(pitch);
        viewMatrix.setLookAt(position, cameraTarget, cameraUpVector);
    }

    private Vector3f calculateCameraUpVector(float pitch) {
        // are we looking from below or above?
        float normalizedPitch = pitch % (2 * (float) Math.PI);
        if (normalizedPitch < 0) {
            normalizedPitch += 2 * (float) Math.PI;
        }

        Vector3f cameraUpVector = new Vector3f(0f, 1f, 0f);
        // interpolation if we're below the model
        if (normalizedPitch > PI_OVER_TWO && normalizedPitch < THREE_PI_OVER_TWO) {
            cameraUpVector.set(0f, -1f, 0f);
        }

        // smooth out the interpolation
        float transitionRange = 0.1f;
        boolean normalizedPiOverTwo = Math.abs(normalizedPitch - PI_OVER_TWO) < transitionRange;
        boolean doSmoothInterpolation = (normalizedPiOverTwo
                || Math.abs(normalizedPitch - THREE_PI_OVER_TWO) < transitionRange);
        if (doSmoothInterpolation) {
            float interpolationFactor = 0;
            if (normalizedPiOverTwo) {
                interpolationFactor = (normalizedPitch - (PI_OVER_TWO - transitionRange)) / (2 * transitionRange);
            } else {
                interpolationFactor = (normalizedPitch - (THREE_PI_OVER_TWO - transitionRange)) / (2 * transitionRange);
            }
            // smooth it out
            interpolationFactor = 0.5f + 0.5f * (float) Math.sin((interpolationFactor - 0.5f) * Math.PI);
            cameraUpVector.set(0, Math.cos(interpolationFactor * Math.PI), 0);
        }
        return cameraUpVector;
    }

    public void orbitRight(float inc) {
        addRotation(0, inc);
        updateOrbitPosition();
    }

    public void orbitLeft(float inc) {
        addRotation(0, -inc);
        updateOrbitPosition();
    }

    public void orbitUp(float inc) {
        addRotation(-inc, 0);
        updateOrbitPosition();
    }

    public void orbitDown(float inc) {
        addRotation(inc, 0);
        updateOrbitPosition();
    }

}
