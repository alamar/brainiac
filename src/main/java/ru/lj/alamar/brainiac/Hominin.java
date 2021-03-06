package ru.lj.alamar.brainiac;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

import ru.yandex.bolts.collection.ListF;

public class Hominin {
    private static final DecimalFormat FMT = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    public enum Trait {
        REPRODUCTION(0),
        OLD_AGE_SURVIVAL(1),
        CONSERVATION(2)
        ;

        public final int idx;

        private Trait(int idx) {
            this.idx = idx;
        }
    }

    public static final int YOUNG_AGE = 0;
    public static final int MATURE_AGE = 10;
    public static final int OLD_AGE = 20;
    public static final int FINAL_AGE = 30;

    private int age;
    private final float g;
    private float[] traits;

    public Hominin(int age, float g, float[] traits) {
        this.g = g;
        this.age = age;
        this.traits = traits;
    }

    public boolean liveOn(Random r, float dieOffRatio) {
        age += 1;
        return (age < FINAL_AGE) && (age < OLD_AGE || trigger(r, Trait.OLD_AGE_SURVIVAL, 0.75f))
                && trigger(r, Trait.CONSERVATION, 3f - g - Math.max(1, dieOffRatio));
    }

    public boolean willReproduce(Random r) {
        return age >= MATURE_AGE && age < OLD_AGE && trigger(r, Trait.REPRODUCTION, 0.1f);
    }

    public static Hominin reproduce(Random r, Hominin person, Hominin pair) {
        float[] childTraits = new float[Trait.values().length];
        for (int i = 0; i < childTraits.length; i++) {
            childTraits[i] = mutate(r, person.traits[i], pair.traits[i]);
        }
        return new Hominin(0, Math.max(mutate(r, person.g, pair.g), 1), childTraits);
    }

    private static float mutate(Random r, float a, float b) {
        float value = Math.min(a, b) + r.nextFloat() * Math.abs(a - b);
        float mut = r.nextFloat() * 0.05f - 0.025f;

        if (mut < 0f) {
            return value * (1f + mut);
        } else {
            return 2f - (2f - value) * (1f - mut);
        }
    }

    private boolean trigger(Random r, Trait trait, float baseChance) {
        float traitValue = traits[trait.idx];
        if (traitValue < 1f) {
            traitValue /= g;
        } else {
            traitValue *= g;
        }
        float chance = baseChance * traitValue;
        return r.nextFloat() < chance;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(age).append("\t").append(FMT.format(g));
        for (float trait : traits) {
            result.append("\t").append(FMT.format(trait));
        }
        return result.toString();
    }

    public static Hominin create() {
        float[] traits = new float[Trait.values().length];
        for (int i = 0; i < traits.length; i++) {
            traits[i] = 1f;
        }
        return new Hominin(MATURE_AGE, 1f, traits);
    }
}
