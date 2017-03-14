package ru.lj.alamar.brainiac;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function1B;

public class Hominin {
    public static final int TURN_COST = 5;

    private static final DecimalFormat FMT = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private static final double REPRODUCTION_CHANCE = 0.5;
    private static final int REPRODUCTION_COST = 10;
    private static final int MAX_MEMES = 3;

    public static int arrayLength;

    public enum Trait {
        HUNT_SKL(0, 0.8f),
        FREERIDE_AFF(1, 0.1f),
//        FREERIDE_SKL(2, 0.25f),
        DETECT_AFF(3, 0.1f),
        DETECT_SKL(4, 0.1f),
        CANTRIP_AFF(5, 0.1f),
        //LEARNING_AFF(6, 0.1f),
        LEARNING_SKL(7, 0.25f),
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

    public static class Meme {
        public final Trait trait;
        public final boolean isBoosting;

        public Meme(Trait trait, boolean isBoosting) {
            this.trait = trait;
            this.isBoosting = isBoosting;
        }

        @Override
        public String toString() {
            return (isBoosting ? "+" : "-") + trait;
        }
    }

    public static final int YOUNG_AGE = 0;
    public static final int MATURE_AGE = 10;
    public static final int FINAL_AGE = 30;

    private int age;
    private final float g;
    private float effectiveG;
    private float[] traits;
    private Meme[] memes;
    private int credit;

    public Hominin(int age, float g, float effectiveG, float[] traits, int credits) {
        this.age = age;
        this.g = g;
        this.effectiveG = effectiveG;
        this.traits = traits;
        this.memes = new Meme[MAX_MEMES];
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

    public boolean discoverMeme(Random r) {
        Meme discovered = new Meme(Trait.values()[r.nextInt(Trait.values().length)], r.nextBoolean());
        return pushMeme(discovered);
    }

    public boolean transferMeme(Random r, Hominin source) {
        ListF<Meme> memes = Cf.list(source.memes).filter(Function1B.<Meme>notNullF());
        return memes.isNotEmpty() && pushMeme(memes.get(r.nextInt(memes.size())));
    }

    private boolean pushMeme(Meme meme) {
        for (int i = 0; i < memes.length; i++) {
            if (memes[i] == null) {
                memes[i] = meme;
                return true;
            }
        }
        return false;
    }

    public static Hominin reproduce(Random r, float gEffect, Hominin person, Hominin pair) {
        int childCredits = person.credit / 2 + pair.credit / 2 - REPRODUCTION_COST / 2;
        person.spend(person.credit / 2);
        pair.spend(pair.credit / 2);

        float[] childTraits = new float[arrayLength];
        for (Trait trait : Trait.values()) {
            childTraits[trait.idx] = mutate(r, person.traits[trait.idx], pair.traits[trait.idx]);
        }
        float childG = mutate(r, person.g, pair.g);
        return new Hominin(0, childG, childG * gEffect + (childG * (1f - gEffect) * r.nextFloat()),
                childTraits, childCredits);
    }

    private static float mutate(Random r, float a, float b) {
        float value = Math.min(a, b) + r.nextFloat() * Math.abs(a - b);
        float mut = r.nextFloat() * 0.05f - 0.025f;

        return Math.max(0f, Math.min(1f, value + mut));
    }

    public boolean trigger(Random r, Trait trait) {
        float traitValue = traits[trait.idx];
        for (Meme meme : memes) {
            if (meme != null && meme.trait == trait) {
                if (meme.isBoosting) {
                    traitValue = 1f - ((1f - traitValue) / 2f);
                } else {
                    traitValue /= 2f;
                }
            }
        }
        traitValue = traitValue * effectiveG;
        return r.nextFloat() < traitValue;
    }

    public void spend(int cost) {
        this.credit -= cost;
    }

    public float g() {
        return g;
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
        for (Meme meme : memes) {
            if (meme != null) {
                result.append("\t").append(meme);
            }
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
