package io.github.orlouge.landmarks.utils;

public class GaussianBlur {
    public static double[][] gaussianBlur(double[][] input, boolean[][] mask, int radius, double sigma) {
        int height = input.length;
        int width = input[0].length;
        double[][] output = new double[height][width];

        double[] kernel = kernel(radius, sigma);

        double[][] temp = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask != null && mask[y][x]) {
                    temp[y][x] = input[y][x];
                    continue;
                }

                double sum = 0.0;
                double weightSum = 0.0;
                for (int k = -radius; k <= radius; k++) {
                    int nx = x + k;
                    if (nx < 0 || nx >= width) continue;
                    //if (mask != null && mask[y][nx]) continue;

                    double w = kernel[Math.abs(k)];
                    sum += input[y][nx] * w;
                    weightSum += w;
                }
                temp[y][x] = (weightSum > 0) ? sum / weightSum : input[y][x];
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask != null && mask[y][x]) {
                    output[y][x] = temp[y][x];
                    continue;
                }

                double sum = 0.0;
                double weightSum = 0.0;
                for (int k = -radius; k <= radius; k++) {
                    int ny = y + k;
                    if (ny < 0 || ny >= height) continue;
                    //if (mask != null && mask[ny][x]) continue;

                    double w = kernel[Math.abs(k)];
                    sum += temp[ny][x] * w;
                    weightSum += w;
                }
                output[y][x] = (weightSum > 0) ? sum / weightSum : temp[y][x];
            }
        }

        return output;
    }

    private static double[] kernel(int radius, double sigma) {
        double[] kernel = new double[radius + 1];
        double sum = 0.0;
        double sigma2 = 2 * sigma * sigma;

        for (int i = 0; i <= radius; i++) {
            double value = Math.exp(-(i * i) / sigma2);
            kernel[i] = value;
            sum += (i == 0) ? value : 2 * value;
        }

        for (int i = 0; i <= radius; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }
}