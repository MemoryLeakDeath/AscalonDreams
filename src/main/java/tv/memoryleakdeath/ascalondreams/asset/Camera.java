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

    // orbiting camera stuff (arcball/matrix math)
    private Matrix4f cameraRotationMatrix = new Matrix4f().identity();
    private float cameraHorizAngle = 0.0f;
    private float cameraVertAngle = 0.0f;
    private Vector3f cameraTarget = new Vector3f();
    private static final float ORBIT_DISTANCE = 4.0f;

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
        Vector3f basePosition = new Vector3f(0, 0, ORBIT_DISTANCE);
        Vector3f rotatedPosition = new Vector3f();
        cameraRotationMatrix.transformPosition(basePosition, rotatedPosition);
        position.set(cameraTarget).add(rotatedPosition);
        viewMatrix.identity().lookAt(position, cameraTarget, up);
    }

    private void updateOrbitRotationMatrix() {
        Matrix4f horizRotation = new Matrix4f().rotateY(cameraHorizAngle);
        Matrix4f vertRotation = new Matrix4f().rotateX(cameraVertAngle);
        Vector3f cameraRightVector = new Vector3f(1, 0, 0);
        horizRotation.transformDirection(cameraRightVector);

        cameraRotationMatrix.identity().mul(vertRotation).mul(horizRotation);
        updateOrbitUpVector();
    }

    private void updateOrbitUpVector() {
        Vector3f viewDirection = new Vector3f(0, 0, 1);
        cameraRotationMatrix.transformDirection(viewDirection);
        float dotProduct = viewDirection.dot(0, 1, 0);

        // if we're close to the poles (looking straight up or down) adjust the camera
        // up vector
        if (Math.abs(dotProduct) > 0.99999f) {
            Vector3f rightVector = new Vector3f(1, 0, 0);
            cameraRotationMatrix.transformDirection(rightVector);
            // recalc up vector
            up.set(rightVector).cross(viewDirection).normalize();
            if (dotProduct < 0) {
                up.negate();
            }
        } else {
            up.set(0, 1, 0);
        }
    }

    public void orbitRight(float inc) {
        cameraHorizAngle += inc;
        updateOrbitRotationMatrix();
        updateOrbitPosition();
    }

    public void orbitLeft(float inc) {
        cameraHorizAngle += -inc;
        updateOrbitRotationMatrix();
        updateOrbitPosition();
    }

    public void orbitUp(float inc) {
        cameraVertAngle += -inc;
        updateOrbitRotationMatrix();
        updateOrbitPosition();
    }

    public void orbitDown(float inc) {
        cameraVertAngle += inc;
        updateOrbitRotationMatrix();
        updateOrbitPosition();
    }

}
