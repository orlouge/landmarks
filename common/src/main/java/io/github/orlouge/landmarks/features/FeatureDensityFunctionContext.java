package io.github.orlouge.landmarks.features;

import io.github.orlouge.landmarks.density.*;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.HashMap;
import java.util.Map;

class FeatureDensityFunctionContext {
    private final Map<Long, double[][]> noise2DCache = new HashMap<>();
    private final Map<String, double[]> customCache = new HashMap<>();
    public final long seed;
    public final int minX, maxX, minY, maxY, minZ, maxZ, surfaceY;
    public final boolean[][] cantPlace;
    public final Map<String, Double> userParameters;

    public FeatureDensityFunctionContext(long seed, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int surfaceY, boolean[][] cantPlace, Map<String, Double> userParameters) {
        this.seed = seed;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.surfaceY = surfaceY;
        this.cantPlace = cantPlace;
        this.userParameters = userParameters;
    }

    public DensityFunction.DensityFunctionVisitor getVisitor() {
        int xExt = maxX - minX + 1, yExt = maxY - minY + 1, zExt = maxZ - minZ + 1;
        return function -> {
            if (function instanceof BorderChamferDistance edgeDist) {
                return edgeDist.create(cantPlace, minX - 1, maxX + 1, minZ - 1, maxZ + 1);
            } else if (function instanceof BorderBlurred blur) {
                return blur.create(cantPlace, minX - 1, maxX + 1, minZ - 1, maxZ + 1);
            } else if (function instanceof Noise3D noise) {
                return noise.create(seed);
            } else if (function instanceof Noise2D noise) {
                double[][] cache = noise2DCache.computeIfAbsent(noise.seed, k -> new double[xExt][zExt]);
                return noise.create(seed, cache, minX, maxX, minZ, maxZ);
            } else if (function instanceof FeatureCache cached) {
                double[] cache = customCache.computeIfAbsent(cached.key, k ->
                    cached.y2d ? new double[(xExt + 2) * (zExt + 2)] : new double[(xExt + 2) * (yExt + 2) * (zExt + 2)]);
                return cached.create(cache, minX - 1, maxX + 1, minY - 1, maxY + 1, minZ - 1, maxZ + 1);
            } else if (function instanceof FeatureUserParameter params && userParameters.containsKey(params.parameter)) {
                //System.out.println(params.parameter + " = " + userParameters.get(params.parameter));
                return params.create(userParameters.get(params.parameter));
            } else if (function instanceof FeatureMinY param) {
                return param.create(minY);
            } else if (function instanceof FeatureMaxY param) {
                return param.create(maxY);
            } else if (function instanceof FeatureSurfaceY param) {
                return param.create(surfaceY);
            } else if (function instanceof FeatureMinX param) {
                return param.create(minX);
            } else if (function instanceof FeatureMinZ param) {
                return param.create(minZ);
            } else if (function instanceof FeatureMaxX param) {
                return param.create(maxX);
            } else if (function instanceof FeatureMaxZ param) {
                return param.create(maxZ);
            } else {
                return function;
            }
        };
    }
}
