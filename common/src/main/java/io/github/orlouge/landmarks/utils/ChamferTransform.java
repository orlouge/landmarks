package io.github.orlouge.landmarks.utils;

public class ChamferTransform {
    public static double[][] distanceTransform(boolean[][] binary) {
        int rows = binary.length;
        int cols = binary[0].length;
        double[][] dist = new double[rows][cols];

        double INF = rows * cols;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                dist[r][c] = binary[r][c] ? 0.0 : INF;
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!binary[r][c]) {
                    if (r > 0) {
                        dist[r][c] = Math.min(dist[r][c], dist[r - 1][c] + 2);
                        if (c > 0) dist[r][c] = Math.min(dist[r][c], dist[r - 1][c - 1] + 3);
                        if (c < cols - 1) dist[r][c] = Math.min(dist[r][c], dist[r - 1][c + 1] + 3);
                    }
                    if (c > 0) {
                        dist[r][c] = Math.min(dist[r][c], dist[r][c - 1] + 2);
                    }
                }
            }
        }

        for (int r = rows - 1; r >= 0; r--) {
            for (int c = cols - 1; c >= 0; c--) {
                if (!binary[r][c]) {
                    if (r < rows - 1) {
                        dist[r][c] = Math.min(dist[r][c], dist[r + 1][c] + 2);
                        if (c > 0) dist[r][c] = Math.min(dist[r][c], dist[r + 1][c - 1] + 3);
                        if (c < cols - 1) dist[r][c] = Math.min(dist[r][c], dist[r + 1][c + 1] + 3);
                    }
                    if (c < cols - 1) {
                        dist[r][c] = Math.min(dist[r][c], dist[r][c + 1] + 2);
                    }
                }
            }
        }

        /*
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                dist[r][c] /= 2.0;
            }
        }
         */

        return dist;
    }
}
