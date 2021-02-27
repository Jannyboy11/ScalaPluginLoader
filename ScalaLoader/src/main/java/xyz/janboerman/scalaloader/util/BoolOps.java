package xyz.janboerman.scalaloader.util;

public final class BoolOps {

    private BoolOps() {
    }

    public static boolean implies(boolean lhs, boolean rhs) {
        return !lhs || rhs;
    }
}
