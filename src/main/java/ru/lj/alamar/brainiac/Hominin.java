package ru.lj.alamar.brainiac;

public class Hominin {

    public static final int MATURE_AGE = 10;
    public static final int OLD_AGE = 20;

    private final float g;

    private int age;

    public Hominin(float g, int age) {
        this.g = g;
        this.age = age;
    }

    public boolean liveOn() {
        return (age++ < OLD_AGE);
    }

    public static Hominin create() {
        return new Hominin(1.0f, MATURE_AGE);
    }
}
