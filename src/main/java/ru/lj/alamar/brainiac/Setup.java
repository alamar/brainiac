package ru.lj.alamar.brainiac;

import java.util.Properties;

import ru.lj.alamar.brainiac.Hominin.Trait;

public class Setup {

    public final int arrayLength;

    public final int population;
    public final float gamePerHunter;
    public final float gEffect;
    public final int splitOn;
    public final float leadershipEffect;
    public final int maxMemes;
    public final float reproduceChance;
    public final float[] startingTraits;
    public final float startingG;

    {
        int arrayLength = 0;
        for (Trait trait : Trait.values()) {
            if (trait.idx >= arrayLength) {
                arrayLength = trait.idx + 1;
            }
        }
        this.arrayLength = arrayLength;
    }

    public Setup(Properties model) {
        this.population = Integer.parseInt(model.getProperty("population"));
        this.gamePerHunter = Float.parseFloat(model.getProperty("game"));
        this.gEffect = Float.parseFloat(model.getProperty("g.effect"));
        this.leadershipEffect = Float.parseFloat(model.getProperty("leadership.effect"));
        this.splitOn = Integer.parseInt(model.getProperty("split.on"));
        this.maxMemes = Integer.parseInt(model.getProperty("max.memes"));
        this.reproduceChance = Float.parseFloat(model.getProperty("reproduce.chance"));

        this.startingG = Float.parseFloat(model.getProperty("starting.g"));
        this.startingTraits = new float[arrayLength];
        for (Trait trait : Trait.values()) {
            String key = "starting." + trait.name().replaceAll("_", ".").toLowerCase();

            this.startingTraits[trait.idx] = Float.parseFloat(model.getProperty(key));
        }
    }

}
