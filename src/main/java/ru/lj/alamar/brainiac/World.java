package ru.lj.alamar.brainiac;

import java.util.Properties;
import java.util.Random;

import ru.lj.alamar.brainiac.Hominin.Trait;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

public class World {

    private ListF<Hominin> fates;
    private Random r;
    private int population;
    private int gamePerHunter;

    public World(Properties model, Random r) {
        this.r = r;
        this.fates = Cf.arrayList();
        this.population = Integer.parseInt(model.getProperty("population"));
        this.gamePerHunter = Integer.parseInt(model.getProperty("game"));
        for (int i = 0; i < population; i++) {
            fates.add(Hominin.create());
        }
    }

    public String advance() {
        //ListF<Hominin> nextStepFates = Cf.arrayList();
        ListF<Hominin> underAge = Cf.arrayList();
        ListF<Hominin> expectant = Cf.arrayList();
        ListF<Hominin> hunters = Cf.arrayList();
        ListF<Hominin> freeriders = Cf.arrayList();

        for (Hominin person : fates.shuffle()) {
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
        do {
            for (Hominin eater : eaters) {
                eater.spend(-1);
                if (--game == 0) {
                    break;
                }
            }
        } while (game > 0);

        fates = eaters.plus(punished).plus(newBorn);
        for (Hominin hominin : fates) {
            hominin.spend(Hominin.TURN_COST);
        }
        return "population size: " + fates.size();
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
        return fates.unmodifiable();
    }

    public boolean isEmpty() {
        return fates.size() <= 1;
    }

}
