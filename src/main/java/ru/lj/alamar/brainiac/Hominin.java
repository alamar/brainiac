package ru.lj.alamar.brainiac;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

public class Hominin {
    public static final int TURN_COST = 5;

    private static final DecimalFormat FMT = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private static final double REPRODUCTION_CHANCE = 0.5;
    private static final int REPRODUCTION_COST = 10;

    private static int arrayLength;

    public enum Trait {
        HUNT_SKL(0, 0.8f),
        FREERIDE_AFF(1, 0.1f),
//        FREERIDE_SKL(2, 0.25f),
        DETECT_AFF(3, 0.1f),
        DETECT_SKL(4, 0.1f),
        CANTRIP_AFF(5, 0.1f),
        ;

        public final int idx;
        public final float baseline;

        private Trait(int idx, float baseline) {
            this.idx = idx;
            this.baseline = baseline;
            if (idx >= arrayLength) {
                arrayLength = idx + 1;
            }
        }
    }

    public static final int YOUNG_AGE = 0;
    public static final int MATURE_AGE = 10;
    public static final int FINAL_AGE = 30;

    private int age;
    private final float g;
    private float effectiveG;
    private float[] traits;
    private int credit;

    public Hominin(int age, float g, float effectiveG, float[] traits, int credits) {
        this.age = age;
        this.g = g;
        this.effectiveG = effectiveG;
        this.traits = traits;
        this.credit = credits;
    }

    public boolean liveOn(Random r) {
        age += 1;
        return age < FINAL_AGE && credit >= 0;
    }

    public boolean willReproduce(Random r) {
        return age >= MATURE_AGE && credit >= REPRODUCTION_COST
                && r.nextFloat() > REPRODUCTION_CHANCE;
    }

    public static Hominin reproduce(Random r, Hominin person, Hominin pair) {
        int childCredits = person.credit / 2 + pair.credit / 2 - REPRODUCTION_COST / 2;
        person.spend(person.credit / 2);
        pair.spend(pair.credit / 2);

        float[] childTraits = new float[arrayLength];
        for (Trait trait : Trait.values()) {
            childTraits[trait.idx] = mutate(r, person.traits[trait.idx], pair.traits[trait.idx]);
        }
        float childG = mutate(r, person.g, pair.g);
        return new Hominin(0, childG, childG * r.nextFloat(), childTraits, childCredits);
    }

    private static float mutate(Random r, float a, float b) {
        float value = Math.min(a, b) + r.nextFloat() * Math.abs(a - b);
        return value;
        /*float mut = r.nextFloat() * 0.05f - 0.025f;

        if (mut < 0f) {
            return value * (1f + mut);
        } else {
            return 1f - (1f - value) * (1f - mut);
        }*/
    }

    public boolean trigger(Random r, Trait trait) {
        float traitValue = traits[trait.idx] * effectiveG;
        /*if (traitValue < 1f) {
            traitValue /= g;
        } else {
            traitValue *= g;
        }
        float chance = baseChance * traitValue;*/
        return r.nextFloat() < traitValue;
    }

    public void spend(int cost) {
        this.credit -= cost;
    }

    public float trait(Trait trait) {
        return traits[trait.idx];
    }

    public boolean underAge() {
        return age < MATURE_AGE;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(age).append("\t").append(FMT.format(g)).append("\t").append(credit);
        for (float trait : traits) {
            result.append("\t").append(FMT.format(trait));
        }
        return result.toString();
    }

    public static Hominin create(Random r) {
        Trait.values();
        float[] traits = new float[arrayLength];
        for (Trait trait : Trait.values()) {
            traits[trait.idx] = trait.baseline + r.nextFloat() * (1 - trait.baseline);
        }
        return new Hominin(MATURE_AGE, 0.25f + 0.75f * r.nextFloat(), 0.25f, traits, 10);
    }
}
