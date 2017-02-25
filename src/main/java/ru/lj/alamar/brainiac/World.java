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
            if (person.liveOn(r, nextStepFates, dieOffRatio)) {
                nextStepFates.add(person);
            }
        }

        fates = nextStepFates;
        return "Population size: " + fates.size();
    }

    public ListF<Hominin> getFates() {
        return fates.unmodifiable();
    }

}
