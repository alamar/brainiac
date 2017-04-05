package ru.lj.alamar.brainiac;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

public class Hominin {
    public static final int TURN_COST = 5;

    private static final DecimalFormat FMT = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private static final int REPRODUCTION_COST = 10;
    private static final Meme[] NO_MEMES = new Meme[0];


    public enum Trait {
        HUNT_SKL(0, "HUNT"),
        FREERIDE_AFF(1, "FRRIDE"),
//        FREERIDE_SKL(2, 0.25f),
        DETECT_AFF(2, "DTC_AF"),
        DETECT_SKL(3, "DTC_SK"),
        CANTRIP_AFF(4, "CANTRP"),
        //LEARNING_AFF(6, 0.1f),
        LEARNING_SKL(5, "LEARN"),
        TEACHING_SKL(6, "TEACH"),
        LEADERSHIP(7, "LEADSP"),
        BOASTING(8, "BOAST"),
        ;

        public final int idx;
        public final String tag;

        private Trait(int idx, String tag) {
            this.idx = idx;
            this.tag = tag;
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

    private final Setup s;
    private int age;
    private final float g;
    private float effectiveG;
    private float[] traits;
    private Meme[] memes;
    private int credit;

    public Hominin(Setup s, int age, float g, float effectiveG, float[] traits, int credits) {
        this.s = s;
        this.age = age;
        this.g = g;
        this.effectiveG = effectiveG;
        this.traits = traits;
        this.memes = NO_MEMES;
        this.credit = credits;
    }

    public boolean liveOn(Random r) {
        age += 1;
        return age < FINAL_AGE && credit >= 0;
    }

    public boolean willReproduce(Random r) {
        return age >= MATURE_AGE && credit >= REPRODUCTION_COST
                && r.nextFloat() > s.reproduceChance;
    }

    public boolean discoverMeme(Random r) {
        Meme discovered = new Meme(Trait.values()[r.nextInt(Trait.values().length)], r.nextBoolean());
        return pushMeme(discovered);
    }

    public boolean transferMeme(Random r, Hominin source, boolean passTeaching) {
        if (source.memes.length == 0) {
            return false;
        }

        Meme meme = source.memes[r.nextInt(source.memes.length)];
        if (!passTeaching && meme.isBoosting && meme.trait == Trait.TEACHING_SKL) {
            return false;
        }
        return pushMeme(meme);
    }

    private boolean pushMeme(Meme meme) {
        if (memes.length < s.maxMemes && traits[meme.trait.idx] > 0f) {
            Meme[] newMemes = new Meme[memes.length + 1];
            System.arraycopy(memes, 0, newMemes, 0, memes.length);
            newMemes[memes.length] = meme;
            memes = newMemes;
            return true;
        }
        return false;
    }

    public static Hominin reproduce(Random r, float gEffect, Hominin person, Hominin pair) {
        Setup s = person.s;
        int childCredits = person.credit / 2 + pair.credit / 2 - REPRODUCTION_COST / 2;
        person.spend(person.credit / 2);
        pair.spend(pair.credit / 2);

        float[] childTraits = new float[s.arrayLength];
        for (Trait trait : Trait.values()) {
            childTraits[trait.idx] = mutate(r, person.traits[trait.idx], pair.traits[trait.idx]);
        }
        float childG = mutate(r, person.g, pair.g);
        return new Hominin(s, 0, childG, childG * gEffect + (childG * (1f - gEffect) * r.nextFloat()),
                childTraits, childCredits);
    }

    private static float mutate(Random r, float a, float b) {
        float value = Math.min(a, b) + r.nextFloat() * Math.abs(a - b);
        if (value == 0f) {
            return value;
        }
        float mut = r.nextFloat() * 0.05f - 0.025f;

        return Math.max(0f, Math.min(1f, value + mut));
    }

    public boolean trigger(Random r, Trait trait) {
        float traitValue = traitAfterMeme(trait);
        traitValue = traitValue * effectiveG;
        return r.nextFloat() < traitValue;
    }

    public float traitAfterMeme(Trait trait) {
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
        return traitValue;
    }

    public float leadership() {
        return effectiveG * traitAfterMeme(Trait.LEADERSHIP);
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

    public Meme[] memes() {
        return memes;
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

    public static Hominin create(Setup s, Random r) {
        float[] traits = new float[s.arrayLength];
        for (Trait trait : Trait.values()) {
            float startingValue = s.startingTraits[trait.idx];
            traits[trait.idx] = startingValue + (r.nextFloat() - 0.5f) * startingValue;
        }
        return new Hominin(s, MATURE_AGE,
                s.startingG + (r.nextFloat() - 0.5f) * s.startingG,
                s.startingG, traits, 10);
    }
}
