package com.ez.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameTracker {
    public enum Result { WIN, LOSS }

    private static final List<Result> HISTORY = new ArrayList<>();

    private GameTracker() {}

    public static void add(Result result) {
        HISTORY.add(result);
    }

    public static int wins() {
        return (int) HISTORY.stream().filter(r -> r == Result.WIN).count();
    }

    public static int losses() {
        return (int) HISTORY.stream().filter(r -> r == Result.LOSS).count();
    }

    public static int total() {
        return HISTORY.size();
    }

    public static double winRate() {
        return total() == 0 ? 0.0 : (wins() * 100.0 / total());
    }

    public static List<Result> history() {
        return Collections.unmodifiableList(HISTORY);
    }

    public static void clear() {
        HISTORY.clear();
    }
}
