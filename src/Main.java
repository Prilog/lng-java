import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Main {
    /**
     * Class for storing parsed strings. Also contains their initial index and number of a group.
     */
    private static class Triplet {
        int index;
        List<String> strings;

        Triplet(int ind, String a, String b, String c) {
            index = ind;
            strings = Arrays.asList(a, b, c);
        }

        @Override
        public String toString() {
            return strings.get(0) + ":" + strings.get(1) + ":" + strings.get(2);
        }
    }

    /**
     * Generic implementation of optimised DSU.
     */
    private static class DSU<T> {
        List<Integer> parent;
        List<Integer> rank;
        int sets;

        DSU(List<T> lst) {
            Integer[] array = IntStream.range(0, lst.size()).boxed().toArray(Integer[]::new);
            parent = Arrays.asList(array);
            rank = new ArrayList<>(Collections.nCopies(lst.size(), 0));
        }

        public int findSet(int ind) {
            if (ind == parent.get(ind)) {
                return ind;
            }
            return parent.set(ind, findSet(parent.get(ind)));
        }

        public void unionSets(int a, int b) {
            a = findSet(a);
            b = findSet(b);
            if (a != b) {
               if (rank.get(a) < rank.get(b)) {
                   int c = a;
                   a = b;
                   b = c;
               }
               parent.set(b, a);
               if (rank.get(a).equals(rank.get(b))) {
                   rank.set(a, rank.get(a) + 1);
               }
            }
        }

        public List<Integer> generateListOfSets() {
            int totalSets = 0;
            int n = parent.size();
            List<Integer> result = new ArrayList<>(Collections.nCopies(n, -1));
            for (int i = 0; i < n; i++) {
                int currentSet = findSet(i);
                if (result.get(currentSet) == -1) {
                    result.set(currentSet, totalSets++);
                }
                result.set(i, result.get(currentSet));
            }
            sets = totalSets;
            return result;
        }
    }

    /**
     * Applies regexp to a string and generates its Triplet
     * @param arg String to be analyzed
     * @param ind Index of string in initial list
     * @return Triplet of analyzed string or null if string is incorrect
     */
    private static Triplet allow(String arg, int ind) {
        Pattern pattern = Pattern.compile("(\".*\");(\".*\");(\".*\")");
        Matcher matcher = pattern.matcher(arg);
        if (matcher.find() && arg.equals(arg.substring(matcher.start(), matcher.end()))) {
            return new Triplet(ind, matcher.group(1), matcher.group(2), matcher.group(3));
        }
        return null;
    }

    /**
     * Generates groups of Triplets by using DSU
     * @param lines list of analyzed strings
     * @param empty string which is consider to be empty
     * @return list of groups
     */
    private static List<List<String>> connectLines(List<Triplet> lines, String empty) {
        // Create DSU
        DSU<Triplet> dsu = new DSU<>(lines);
        // For each column of strings
        for (int i = 0; i < 3; i++) {
            // Sort Triplets by column
            int finalI = i;
            Comparator<Triplet> cmp = Comparator.comparing(o -> o.strings.get(finalI));
            lines.sort(cmp);
            // Go through and unite Triplets with same strings
            for (int j = 1; j < lines.size(); j++) {
                if (!lines.get(j).strings.get(i).equals(empty) &&
                        lines.get(j).strings.get(i).compareTo(lines.get(j - 1).strings.get(i)) == 0) {
                    dsu.unionSets(lines.get(j - 1).index, lines.get(j).index);
                }
            }
        }
        // Generate a result
        List<Integer> sets = dsu.generateListOfSets();
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < dsu.sets; i++) {
            result.add(new ArrayList<>());
        }
        for (Triplet t : lines) {
            int set = sets.get(t.index);
            result.get(set).add(t.toString());
        }
        return result;
    }

    /**
     * Writes string in file of std
     * @param str String to be written
     * @param writer Writer or null if it is std
     */
    private static void write(String str, FileWriter writer) {
        if (writer == null) {
            System.out.println(str);
            return;
        }
        try {
            writer.write(str + "\n");
        } catch (IOException e) {
            System.out.println("An error occurred during writing: " + e);
        }
    }

    /**
     * Analyzes strings and sorts by groups
     * @param args Arguments of command prompt: input file name, empty string flag, output file name.
     *             Empty string flag is 1 if string "" consider empty anything otherwise.
     *             Output file name is optional.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Not enough arguments. " +
                    "Format : \"inputFileName emptyStringFlag outputFileName\". " +
                    "emptyStringFlag is 1 if \"\" is consider empty. outputFileName is optional.");
        }
        if (args.length > 3) {
            System.out.println("Too many arguments. " +
                    "Format : \"inputFileName emptyStringFlag outputFileName\". " +
                    "emptyStringFlag is 1 if \"\" is consider empty. outputFileName is optional.");
        }
        File input = new File(args[0]);
        String empty = "";
        if (args[1].equals("1")) {
            empty = "\"\"";
        }
        File output = null;
        if (args.length == 3) {
            output = new File(args[2]);
        }
        if (!input.exists()) {
            System.out.println("File " + args[1] + " does not exists.");
            return;
        }
        List<Triplet> lines = new ArrayList<>();
        try {
            Scanner reader = new Scanner(input);
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                Triplet triplet = allow(data, lines.size());
                if (triplet != null) {
                    lines.add(triplet);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error occurred during reading: " + e);
            return;
        }
        List<List<String>> result = connectLines(lines, empty);
        FileWriter writer = null;
        if (output != null) {
            try {
                if (output.createNewFile()) {
                    System.out.println("Created an " + output);
                }
                writer = new FileWriter(output);

            } catch (IOException e) {
                System.out.println("An error during writing occurred: " + e);
            }
        }
        write("Total groups: " + result.size(), writer);
        write("Total groups with size over 1: " +
                result.stream().filter(i -> i.size() > 1).count(), writer);
        Comparator<List<String>> cmp = Comparator.comparingInt(List::size);
        result.sort(cmp.reversed());
        for (int i = 0; i < result.size(); i++) {
            write("Group №" + (i + 1), writer);
            for (String s : result.get(i)) {
                write(s, writer);
            }
        }
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                System.out.println("An error occurred during writing: " + e);
            }
        }
    }
}
