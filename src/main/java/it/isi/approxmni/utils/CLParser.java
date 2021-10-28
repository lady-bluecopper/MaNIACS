package it.isi.approxmni.utils;

public class CLParser {

    public static void parse(String[] args) {
        if (args != null && args.length > 0) {
            parseArgs(args);
        }
    }

    private static void parseArgs(String[] args) {
        for (String arg : args) {
            String[] parts = arg.split("=");
            parseArg(parts[0], parts[1]);
        }
    }

    private static void parseArg(String key, String value) {
    
        if (key.equalsIgnoreCase("dataFolder")) {
            Settings.dataFolder = value;
        } else if (key.equalsIgnoreCase("inputFile")) {
            Settings.inputFile = value;
        } else if (key.equalsIgnoreCase("outputFolder")) {
            Settings.outputFolder = value;
        } else if (key.equalsIgnoreCase("patternSize")) {
            Settings.patternSize = Integer.parseInt(value);
        } else if (key.equalsIgnoreCase("sampleSize")) {
            Settings.sampleSize = Integer.parseInt(value);
        } else if (key.equalsIgnoreCase("c")) {
            Settings.c = Double.parseDouble(value);
        } else if (key.equalsIgnoreCase("seed")) {
            Settings.seed = Integer.parseInt(value);
        } else if (key.equalsIgnoreCase("frequency")) {
            Settings.frequency = Double.parseDouble(value);
        } else if (key.equalsIgnoreCase("failure")) {
            Settings.failure = Double.parseDouble(value);
        } else if (key.equalsIgnoreCase("alpha")) {
            Settings.alpha = Double.parseDouble(value);
        } else if (key.equalsIgnoreCase("isExact")) {
            Settings.isExact = Boolean.valueOf(value);
        } else if (key.equalsIgnoreCase("percent")) {
            Settings.percent = Boolean.valueOf(value);
        }
    }
}
