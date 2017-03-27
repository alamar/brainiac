package ru.lj.alamar.brainiac;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import ru.lj.alamar.brainiac.Hominin.Meme;
import ru.lj.alamar.brainiac.Hominin.Trait;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

public class World {

    private static final DecimalFormat FMT = new DecimalFormat("0.#####", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private ListF<ListF<Hominin>> tribes;
    private Random r;
    private int population;
    private float gamePerHunter;
    private float gEffect;
    private int splitOn;
    private float leadershipCoeff;

    public World(Properties model, Random r) {
        this.r = r;
        this.tribes = Cf.arrayList();
        this.population = Integer.parseInt(model.getProperty("population"));
        this.gamePerHunter = Float.parseFloat(model.getProperty("game"));
        this.gEffect = Float.parseFloat(model.getProperty("g.effect"));
        this.leadershipCoeff = Float.parseFloat(model.getProperty("leadership.coeff"));
        this.splitOn = Integer.parseInt(model.getProperty("split.on"));
        ListF<Hominin> fates = Cf.arrayList();
        for (int i = 0; i < population; i++) {
            fates.add(Hominin.create(r));
        }
        tribes.add(fates);
    }

    public String advance() {
        ListF<ListF<Hominin>> nextStepTribes = Cf.arrayList();
        Hominin traveller = null;

        int population = 0;
        for (ListF<Hominin> fates : tribes) {
            ListF<Hominin> underAge = Cf.arrayList();
            ListF<Hominin> inAge = Cf.arrayList();
            ListF<Hominin> expectant = Cf.arrayList();
            ListF<Hominin> hunters = Cf.arrayList();
            ListF<Hominin> potentialLeaders = Cf.arrayList();
            ListF<Hominin> freeriders = Cf.arrayList();
            ListF<Hominin> detectives = Cf.arrayList();
            int deadCount = 0;
            for (Hominin person : fates) {
                if (!person.liveOn(r)) {
                    deadCount++;
                } else if (person.underAge()) {
                    underAge.add(person);
                } else {
                    inAge.add(person);
                    if (person.willReproduce(r)) {
                        expectant.add(person);
                    } else if (person.trigger(r, Trait.DETECT_AFF)) {
                        detectives.add(person);
                    } else if (person.trigger(r, Trait.FREERIDE_AFF)) {
                        freeriders.add(person);
                    } else {
                        hunters.add(person);
                        if (person.trigger(r, Trait.LEADERSHIP)) {
                            potentialLeaders.add(person);
                        }
                    }
                }
            }
            assert (underAge.size() + expectant.size() + detectives.size() + freeriders.size() + hunters.size()
                    == fates.size() - deadCount);

            for (Hominin child : underAge) {
                if (inAge.isNotEmpty()/* && child.trigger(r, Trait.LEARNING_AFF)*/) {
                    if (child.trigger(r, Trait.LEARNING_SKL)) {
                        if (child.trigger(r, Trait.LEARNING_SKL)) {
                            if (child.discoverMeme(r)) {
                                child.spend(1);
                                continue;
                            }
                        }
                        if (child.transferMeme(r, inAge.get(r.nextInt(inAge.size())), true)) {
                            child.spend(1);
                        }
                    }
                }
            }

            for (Hominin grownup : inAge) {
                if (underAge.isNotEmpty() && grownup.memes().length > 0) {
                    for (Hominin child : underAge) {
                        if (grownup.trigger(r, Trait.TEACHING_SKL)) {
                            if (child.transferMeme(r, grownup, false)) {
                                grownup.spend(1);
                                break;
                            }
                        }
                    }
                }
            }

            ListF<Hominin> newBorn = Cf.arrayList();
            {
                ListF<Hominin> parents = expectant;
                while (parents.length() > 1) {
                    newBorn.add(Hominin.reproduce(r, gEffect, parents.last(),
                            parents.get(parents.length() - 2)));
                    parents = parents.rdrop(2);
                }
                hunters.addAll(parents);
                expectant = expectant.rdrop(parents.length());
            }

            float totalHuntingSkill = 0f;
            ListF<Hominin> punished = Cf.arrayList();
            for (Hominin person : detectives) {
                person.spend(1);
                if (person.trigger(r, Trait.DETECT_SKL) && freeriders.isNotEmpty()) {
                    punished.add(freeriders.first());
                    freeriders = freeriders.drop(1);
                }
            }

            for (Hominin person : hunters.plus(detectives)) {
                person.spend(2);
                totalHuntingSkill += person.traitAfterMeme(Trait.HUNT_SKL);
            }
            for (Hominin person : potentialLeaders) {
                person.spend(1);
            }
            float coordGamePerHunter = gamePerHunter;
            if (potentialLeaders.isNotEmpty()) {
                Hominin leader = potentialLeaders.get(r.nextInt(potentialLeaders.length()));
                coordGamePerHunter += leader.leadership() * leadershipCoeff;
            }

            int game = 10 + (int) ((coordGamePerHunter - Math.log(tribes.size()))
                    * totalHuntingSkill);

            ListF<Hominin> eaters = hunters.plus(detectives).plus(expectant).plus(freeriders).plus(underAge);
            while (game > 0 && eaters.isNotEmpty()) {
                for (Hominin eater : eaters) {
                    eater.spend(-1);
                    if (--game == 0) {
                        break;
                    }
                }
            }

            assert (eaters.size() + punished.size() == fates.size() - deadCount);
            fates = eaters.plus(punished).plus(newBorn);
            for (Hominin hominin : fates) {
                hominin.spend(Hominin.TURN_COST);
                if (hominin.trigger(r, Trait.CANTRIP_AFF)) {
                    hominin.spend(1);
                }
            }

            if (fates.isEmpty()) {
                continue;
            }

            Collections.shuffle(fates, r);
            population += fates.size();
            if (traveller != null) {
                traveller = fates.set(0, traveller);
            } else {
                traveller = fates.first();
            }

            if (fates.size() > splitOn) {
                int splitAt = 1 + r.nextInt(splitOn - 2);
                nextStepTribes.add(Cf.arrayList(fates.take(splitAt)));
                nextStepTribes.add(Cf.arrayList(fates.drop(splitAt)));
            } else if (fates.isNotEmpty()) {
                nextStepTribes.add(fates);
            }
        }
        for (ListF<Hominin> tribe : nextStepTribes) {
            if (traveller != null && !tribe.isEmpty()) {
                tribe.set(0, traveller);
                break;
            }
        }
        this.tribes = nextStepTribes;
        return turnStats(population);
    }

    private String turnStats(int population) {
        ListF<String> stats = Cf.arrayList();
        stats.add(Integer.toString(population));
        stats.add(Integer.toString(tribes.length()));
        double totalG = 0.0;
        double[] totalTraits = new double[Hominin.arrayLength];
        int[] totalBoostMemes = new int[Hominin.arrayLength];
        int[] totalInhibMemes = new int[Hominin.arrayLength];
        for (Hominin hominin : getFates()) {
            totalG += hominin.g();
            for (Trait trait : Trait.values()) {
                totalTraits[trait.idx] += hominin.trait(trait);
            }
            for (Meme meme : hominin.memes()) {
                (meme.isBoosting ? totalBoostMemes : totalInhibMemes)[meme.trait.idx]++;
            }
        }
        stats.add(FMT.format(totalG / (double) population));
        for (double trait : totalTraits) {
            stats.add(FMT.format(trait / (double) population));
        }
        for (int memes : totalBoostMemes) {
            stats.add(Integer.toString(memes));
        }
        for (int memes : totalInhibMemes) {
            stats.add(Integer.toString(memes));
        }
        return stats.mkString("\t");
    }

    public String header() {
        ListF<String> traitColumns = Cf.arrayList();
        ListF<String> memeBoostColumns = Cf.arrayList();
        ListF<String> memeInhibColumns = Cf.arrayList();
        for (Trait trait : Trait.values()) {
            for (ListF<String> columns : Cf.arrayList(traitColumns, memeBoostColumns, memeInhibColumns)) {
                while (columns.size() <= trait.idx) {
                    columns.add("");
                }
            }
            traitColumns.set(trait.idx, trait.tag);
            memeBoostColumns.set(trait.idx, "+" + trait.tag);
            memeInhibColumns.set(trait.idx, "-" + trait.tag);
        }

        return Cf.arrayList("pop", "tribes", "avg g")
                .plus(traitColumns)
                .plus(memeBoostColumns)
                .plus(memeInhibColumns)
                .mkString("\t");
    }

    public ListF<Hominin> getFates() {
        return tribes.<Hominin>flatten();
    }

    public boolean isEmpty() {
        return tribes.size() <= 1;
    }

}
