package ru.lj.alamar.brainiac;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;

import ru.lj.alamar.brainiac.Hominin.Trait;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

public class World {

    private ListF<ListF<Hominin>> tribes;
    private Random r;
    private int population;
    private float gamePerHunter;
    private float gEffect;
    private int splitOn;

    public World(Properties model, Random r) {
        this.r = r;
        this.tribes = Cf.arrayList();
        this.population = Integer.parseInt(model.getProperty("population"));
        this.gamePerHunter = Float.parseFloat(model.getProperty("game"));
        this.gEffect = Float.parseFloat(model.getProperty("g.effect"));
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
            ListF<Hominin> expectant = Cf.arrayList();
            ListF<Hominin> hunters = Cf.arrayList();
            ListF<Hominin> freeriders = Cf.arrayList();
            ListF<Hominin> detectives = Cf.arrayList();
            int deadCount = 0;
            for (Hominin person : fates) {
                if (!person.liveOn(r)) {
                    deadCount++;
                } else if (person.underAge()) {
                    underAge.add(person);
                } else if (person.willReproduce(r)) {
                    expectant.add(person);
                } else if (person.trigger(r, Trait.DETECT_AFF)) {
                    detectives.add(person);
                } else if (person.trigger(r, Trait.FREERIDE_AFF)) {
                    freeriders.add(person);
                } else {
                    hunters.add(person);
                }
            }
            assert (underAge.size() + expectant.size() + detectives.size() + freeriders.size() + hunters.size()
                    == fates.size() - deadCount);

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
                totalHuntingSkill += person.trait(Trait.HUNT_SKL);
            }

            int game = 10 + (int) ((gamePerHunter - Math.log(tribes.size())) * totalHuntingSkill);

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
        return "population " + population + " in " + tribes.length() + " tribes";
    }

    public ListF<Hominin> getFates() {
        return tribes.<Hominin>flatten();
    }

    public boolean isEmpty() {
        return tribes.size() <= 1;
    }

}
