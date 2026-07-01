package io.github.orlouge.landmarks.utils;

import net.minecraft.util.RandomSource;

import java.util.function.Function;

public class MaxDensitySquare {

    public record Result(int x, int y, int size, double density) {}

    public static Result findDenseSquare(double[][] density, int iterations, RandomSource rnd, Function<RandomSource, Integer> side, Function<Result, Double> scoreFun) {
        int n = density.length;
        int m = density[0].length;

        double[][] prefixSum = new double[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                prefixSum[i][j] = (density[i - 1][j - 1])
                        + prefixSum[i - 1][j]
                        + prefixSum[i][j - 1]
                        - prefixSum[i - 1][j - 1];
            }
        }

        Result bestResult = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int t = 0; t < iterations; t++) {
            int k = Math.min(Math.min(n, m), Math.max(1, side.apply(rnd)));

            int x = rnd.nextInt(n - k + 1);
            int y = rnd.nextInt(m - k + 1);

            double sum = squareSum(prefixSum, x, y, k);
            Result result = new Result(x, y, k, sum);
            double score = scoreFun.apply(result);

            if (score > bestScore) {
                bestScore = score;
                bestResult = result;
            }
        }

        return bestResult;
    }

    private static double squareSum(double[][] ps, int x, int y, int k) {
        int x2 = x + k;
        int y2 = y + k;
        return ps[x2][y2] - ps[x][y2] - ps[x2][y] + ps[x][y];
    }
}
