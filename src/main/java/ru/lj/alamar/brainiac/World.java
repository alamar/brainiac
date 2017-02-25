package ru.lj.alamar.brainiac;

import java.util.Properties;
import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

public class World {

    private ListF<Hominin> fates;
    private Random r;
    private int population;

    public World(Properties model, Random r) {
        this.r = r;
        this.fates = Cf.arrayList();
        this.population = Integer.parseInt(model.getProperty("population"));
        for (int i = 0; i < population; i++) {
            fates.add(Hominin.create());
        }
    }

    public String advance() {
        float dieOffRatio = fates.length() / (float) population;
        ListF<Hominin> nextStepFates = Cf.arrayList();
        for (Hominin person : fates) {

            if (fates.size() > 1 && person.willReproduce(r)) {
                Hominin pair = selectOther(r, fates, person);
                nextStepFates.add(Hominin.reproduce(r, person, pair));
            }

            if (person.liveOn(r, dieOffRatio)) {
                nextStepFates.add(person);
            }
        }

        fates = nextStepFates;
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

}
