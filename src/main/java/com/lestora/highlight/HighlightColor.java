package com.lestora.highlight;

public class HighlightColor {
    final float red, green, blue, alpha;

    public float getRed() { return red; }
    public float getGreen() { return green; }
    public float getBlue() { return blue; }
    public float getAlpha() { return alpha; }

    HighlightColor(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public static HighlightColor red(float alpha) {
        return new HighlightColor(1.0F, 0.0F, 0.0F, alpha);
    }

    public static HighlightColor green(float alpha) {
        return new HighlightColor(0.0F, 1.0F, 0.0F, alpha);
    }

    public static HighlightColor blue(float alpha) {
        return new HighlightColor(0.0F, 0.0F, 1.0F, alpha);
    }

    public static HighlightColor yellow(float alpha) {
        return new HighlightColor(1.0F, 1.0F, 0.0F, alpha);
    }

    public static HighlightColor cyan(float alpha) {
        return new HighlightColor(0.0F, 1.0F, 1.0F, alpha);
    }

    public static HighlightColor magenta(float alpha) {
        return new HighlightColor(1.0F, 0.0F, 1.0F, alpha);
    }

    public static HighlightColor white(float alpha) {
        return new HighlightColor(1.0F, 1.0F, 1.0F, alpha);
    }

    public static HighlightColor black(float alpha) {
        return new HighlightColor(0.0F, 0.0F, 0.0F, alpha);
    }

    public static HighlightColor grey(float alpha) {
        return new HighlightColor(0.5F, 0.5F, 0.5F, alpha);
    }

    public static HighlightColor red() { return red(0.5f); }

    public static HighlightColor green() { return green(0.5f); }

    public static HighlightColor blue() { return blue(0.5f); }

    public static HighlightColor yellow() { return yellow(0.5f); }

    public static HighlightColor cyan() { return cyan(0.5f); }

    public static HighlightColor magenta() { return magenta(0.5f); }

    public static HighlightColor white() { return white(0.5f); }

    public static HighlightColor black() { return black(0.5f); }

    public static HighlightColor grey() { return grey(0.5f); }
}
