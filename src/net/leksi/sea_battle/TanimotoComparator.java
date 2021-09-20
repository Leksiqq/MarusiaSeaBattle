package net.leksi.sea_battle;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TanimotoComparator implements Comparator<Map.Entry<String, String>> {
    private Object sets[] = null;
    private int[] len = null;
    private double[] c = null;
    private TreeSet<String> set = new TreeSet<>();

    void setPattern(final String[] patterns) {
        set.clear();
        set.addAll(Arrays.stream(patterns).map(String::toLowerCase).collect(Collectors.toList()));
        len = new int[set.size()];
        c = new double[set.size()];
        sets = new Object[set.size()];
        int pos = 0;
        while(!set.isEmpty()) {
            sets[pos] = new TreeSet<Integer>();
            String pattern = set.pollFirst();
            ((TreeSet<Integer>)sets[pos]).addAll(pattern.chars().boxed().collect(Collectors.toList()));
            len[pos] = pattern.length();
            pos++;
        }
    }

    double get_score(String cmd) {
        int a = cmd.length();
        return IntStream.range(0, len.length).mapToDouble(pos -> {
            TreeSet<Integer> ts = (TreeSet<Integer>) sets[pos];
            c[pos] = 0;
            cmd.chars().forEach(ch -> {
                if(ts.contains(ch)) {
                    c[pos]++;
                }
            });
            return c[pos] / (a + len[pos] - c[pos]);
        }).max().getAsDouble();
    }

    @Override
    public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
        return (int)Math.signum(get_score(o2.getKey()) - get_score(o1.getKey()));
    }
}
