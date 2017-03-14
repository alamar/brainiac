package ru.lj.alamar.brainiac;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import ru.lj.alamar.brainiac.Hominin.Meme;
import ru.lj.alamar.brainiac.Hominin.Trait;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class Model {

    private static class PropertiesFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".properties");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: model {model-name} [RNG-seed] [key=value]...");
            System.err.println("See MODELS directory");
            System.exit(1);
        }
        String firstArg = args[0];
        if (args.length > 1 && args[1] == null) {
            args = new String[] { firstArg }; /* anti-maven */
        }

        File maybeDir = new File("models/" + firstArg);
        if (!maybeDir.isDirectory()) {
            runOneModel(args);
            return;
        }
        for (String fileName : maybeDir.list(new PropertiesFilter())) {
            String[] params = Arrays.copyOf(args, args.length);
            params[0] = firstArg + "/" + fileName;
            runOneModel(params);
        }
    }

    public static void runOneModel(String[] params) throws Exception {
        params[0] = params[0].replace(".properties", "");
        String modelFullName = modelWithParameters(params);
        PrintWriter out = output(modelFullName);
        String title = params[0].replaceAll(".*[/\\\\]", "").replaceAll("_", " ") + " " +
                Cf.list(params).drop(1).mkString(" ");
        try {
            Properties model = loadModel(params, out);
            print(out, "model = " + title);
            Random r = new XorShiftRandom(Long.parseLong(model.getProperty("seed")));
            int steps = Integer.parseInt(model.getProperty("steps"));
            World world = new World(model, r);
            for (int s = 0; s < steps; s++) {
                String stats = world.advance();
                print(out, "Step:" + s + ", " + stats);
                if (s % 10 == 0) {
                    out.flush();
                }
                if (world.isEmpty()) {
                    break;
                }
            }

            float totalG = 0;
            float[] totalTraits = new float[Hominin.arrayLength];
            MapF<String, Integer> totalMemes = Cf.hashMap();

            ListF<Hominin> fates = world.getFates();
            for (int i = 0; i < fates.length(); i++) {
                Hominin survivor = fates.get(i);
                totalG += survivor.g();
                for (Trait trait : Trait.values()) {
                    totalTraits[trait.idx] += survivor.trait(trait);
                }
                for (Meme meme : survivor.memes()) {
                    if (meme != null) {
                        String key = meme.toString();
                        totalMemes.put(key, totalMemes.getOrElse(key, 0) + 1);
                    }
                }
                if ((i % Math.max(1, fates.length() / 20)) == 0) {
                    Model.print(out, fates.get(i).toString());
                }
            }
            print(out, "avg G: " + totalG / (float) fates.length());
            for (Trait trait : Trait.values()) {
                print(out, "avg " + trait + ": " + totalTraits[trait.idx] / (float) fates.length());
            }
            print(out, totalMemes.entries().sortBy2().reverse().mkString("\n", ":\t"));
        } finally {
            out.close();
            System.out.println("Simulation complete for model: " + title);
        }
    }

    static void print(PrintWriter out, String line) {
        System.out.println(line);
        out.println(line);
    }

    static PrintWriter output(String modelName) throws IOException {
        File output = new File("models/" + modelName + ".csv");
        if (output.exists()) {
            System.err.println("Creating back-up copy of simulation results");
            output.renameTo(new File(output.getPath() + ".bak"));
        }
        System.err.println("Writing simulation results to: " + output.getPath());
        return new PrintWriter(output);
    }

    static String modelWithParameters(String[] args) {
        String modelName = args[0];
        for (int a = 1; a < args.length; a++) {
            modelName += "-" + args[a].replaceAll(" ", "").replaceAll("=", "-").replaceAll("\\.", "");
        }
        return modelName;
    }

    static Properties loadModel(String[] args, final PrintWriter out) throws IOException {
        Properties model = new Properties() {
            public String getProperty(String name) {
                String value = super.getProperty(name);
                print(out, name + " = " + value);
                return value;
            }
        };

        loadPropertiesFile(model, "default");
        loadPropertiesFile(model, args[0]);
        String baseModelName = model.getProperty("base.model");
        if (baseModelName != null) {
            // No support for nesting!
            // XXX do we need it at all when we have command-line properties?
            loadPropertiesFile(model, baseModelName);
            loadPropertiesFile(model, args[0]);
        }
        for (int a = 1; a < args.length; a++) {
            String arg = args[a];
            if (arg.matches("^[0-9]+$")) {
                model.setProperty("seed", arg);
                continue;
            }
            int eq = arg.indexOf("=");
            if (eq <= 0) {
                throw new RuntimeException("Cannot parse key=value: " + arg);
            }
            model.setProperty(arg.substring(0, eq).trim(), arg.substring(eq + 1).trim());
        }
        return model;
    }

    static void loadPropertiesFile(Properties model, String modelName) throws IOException {
        FileInputStream stream = new FileInputStream(new File("models/" + modelName + ".properties"));
        try {
            model.load(stream);
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}
