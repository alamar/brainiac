package ru.lj.alamar.brainiac;

import java.util.Properties;
import java.util.Random;

import ru.lj.alamar.brainiac.Hominin.Trait;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

public class World {

    private ListF<ListF<Hominin>> tribes;
    private Random r;
    private int population;
    private int gamePerHunter;
    private int splitOn;

    public World(Properties model, Random r) {
        this.r = r;
        this.tribes = Cf.arrayList();
        this.population = Integer.parseInt(model.getProperty("population"));
        this.gamePerHunter = Integer.parseInt(model.getProperty("game"));
        this.splitOn = Integer.parseInt(model.getProperty("split.on"));
        ListF<Hominin> fates = Cf.arrayList();
        for (int i = 0; i < population; i++) {
            fates.add(Hominin.create());
        }
        tribes.add(fates);
    }

    public String advance() {
        ListF<ListF<Hominin>> nextStepTribes = Cf.arrayList();

        int population = 0;
        for (ListF<Hominin> fates : tribes) {
            ListF<Hominin> underAge = Cf.arrayList();
            ListF<Hominin> expectant = Cf.arrayList();
            ListF<Hominin> hunters = Cf.arrayList();
            ListF<Hominin> freeriders = Cf.arrayList();
            for (Hominin person : fates) {
                if (!person.liveOn(r)) {
                    // Dead
                } else if (person.underAge()) {
                    underAge.add(person);
                } else if (person.willReproduce(r)) {
                    expectant.add(person);
                } else if (person.trigger(r, Trait.FREERIDE_AFF)) {
                    freeriders.add(person);
                } else {
                    hunters.add(person);
                }
            }

            ListF<Hominin> newBorn = Cf.arrayList();
            {
                ListF<Hominin> parents = expectant;
                while (parents.length() > 1) {
                    newBorn.add(Hominin.reproduce(r, parents.last(),
                            parents.get(parents.length() - 2)));
                    parents = parents.rdrop(2);
                }
            }

            float totalHuntingSkill = 0f;
            ListF<Hominin> punished = Cf.arrayList();
            for (Hominin person : hunters) {
                if (person.trigger(r, Trait.DETECT_AFF)) {
                    person.spend(1);
                    if (person.trigger(r, Trait.DETECT_SKL) && freeriders.isNotEmpty()) {
                        punished.add(freeriders.last());
                        freeriders.rdrop(1);
                    }
                }
                person.spend(2);
                totalHuntingSkill += person.trait(Trait.HUNT_SKL);
            }

            int game = (int) (gamePerHunter * totalHuntingSkill);

            ListF<Hominin> eaters = hunters.plus(expectant).plus(freeriders).plus(underAge);
            while (game > 0) {
                for (Hominin eater : eaters) {
                    eater.spend(-1);
                    if (--game == 0) {
                        break;
                    }
                }
            }

            fates = eaters.plus(punished).plus(newBorn);
            for (Hominin hominin : fates) {
                hominin.spend(Hominin.TURN_COST);
            }

            fates = fates.shuffle();
            population += fates.size();

            if (fates.size() > splitOn) {
                int splitAt = 1 + r.nextInt(splitOn - 2);
                nextStepTribes.add(fates.take(splitAt));
                nextStepTribes.add(fates.drop(splitAt));
            } else if (fates.isNotEmpty()) {
                nextStepTribes.add(fates);
            }
        }
        this.tribes = nextStepTribes;
        return "population " + population + " in " + tribes.length() + " tribes";
    }

    private static Hominin selectOther(Random r, ListF<Hominin> fates, Hominin person) {
        if (fates.size() == 1) {
            throw new IllegalStateException();
        }
        Hominin other;
        do {
            other = fates.get(r.nextInt(fates.length()));
        } while (other == person);
        return other;
    }

    public ListF<Hominin> getFates() {
        return tribes.<Hominin>flatten();
    }

    public boolean isEmpty() {
        return tribes.size() <= 1;
    }

}
