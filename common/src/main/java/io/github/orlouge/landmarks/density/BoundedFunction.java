package io.github.orlouge.landmarks.density;

public interface BoundedFunction {
    // Functions that are guaranteed to be 0 outside the bounds.

    int minX();
    int maxX();
    int minY();
    int maxY();
    int minZ();
    int maxZ();
}
