package com.skelly.excamate;

public final class ExcaMateXpCapture {
    private static final ThreadLocal<Integer> CAPTURED_XP = new ThreadLocal<>();

    private ExcaMateXpCapture() {
    }

    public static void start() {
        CAPTURED_XP.set(0);
    }

    public static boolean isCapturing() {
        return CAPTURED_XP.get() != null;
    }

    public static void add(int xp) {
        Integer currentXp = CAPTURED_XP.get();
        if (currentXp != null && xp > 0) {
            CAPTURED_XP.set(currentXp + xp);
        }
    }

    public static int stop() {
        Integer capturedXp = CAPTURED_XP.get();
        CAPTURED_XP.remove();
        return capturedXp == null ? 0 : capturedXp;
    }
}
