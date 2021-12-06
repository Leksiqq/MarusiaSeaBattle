package net.leksi.sea_battle;

import jakarta.servlet.ServletContext;

import java.util.*;
import java.util.stream.Collectors;

import static net.leksi.sea_battle.MarusiaPatterns.*;

public class MarusiaCommands {
    static final double TANIMOTO_THRESHOLD = 0.6;
    static final String SPACE = " ";

    static final String PAYLOAD_HELP = "help";
    static final String PAYLOAD_PLAY = "play";
    static final String PAYLOAD_RULES = "rules";
    static final String PAYLOAD_READY = "ready";
    static final String PAYLOAD_NEW_GAME = "new_game";
    static final String PAYLOAD_PLAYER_BOTH = "player:both";
    static final String PAYLOAD_PLAYER_ENEMY = "player:enemy";
    static final String PAYLOAD_PLAYER_ME = "player:me";
    static final String PAYLOAD_ANSWER_MISSED = "answer:missed";
    static final String PAYLOAD_ANSWER_INJURED = "answer:injured";
    static final String PAYLOAD_ANSWER_KILLED = "answer:killed";

    static final TreeMap<String, Integer> numericals = new TreeMap<>() {{
        put("один", 0);
        put("два", 1);
        put("три", 2);
        put("четыре", 3);
        put("пять", 4);
        put("шесть", 5);
        put("семь", 6);
        put("восемь", 7);
        put("девять", 8);
        put("десять", 9);
    }};
    static final TreeMap<String, Integer> letters = new TreeMap<>() {{
        put("а", 0);
        put("б", 1);
        put("в", 2);
        put("г", 3);
        put("д", 4);
        put("е", 5);
        put("ж", 6);
        put("з", 7);
        put("и", 8);
        put("к", 9);
        put("a", 10);
        put("b", 11);
        put("v", 12);
        put("g", 13);
        put("d", 14);
        put("e", 15);
        put("i", 18);
        put("k", 19);
        put("z", 17);
    }};

    static final List<String> letter_keys = letters.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).
            map(v -> v.getKey()).collect(Collectors.toList());
    static final ArrayList<String> letter_names = new ArrayList<>(){{
        add("а");
        add("бэ");
        add("вэ");
        add("гэ");
        add("дэ");
        add("е");
        add("жэ");
        add("зэ");
        add("и");
        add("кa");
    }};
    static final List<String> numerical_keys = numericals.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).
            map(v -> v.getKey()).collect(Collectors.toList());
    static boolean command_is_new_game(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.equals("new_game")) {
            return true;
        }
        return command_is_pattern(input.tokens, new String[]{PATTERN_NEW_GAME});
    }

    static boolean command_is_help(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.equals(PAYLOAD_HELP)) {
            return true;
        }
        return command_is_pattern(input.tokens, PATTERN_HELP);
    }

    static boolean command_is_repeat(InputHolder input, MarusiaSession ms) {
        return command_is_pattern(input.tokens, PATTERN_REPEAT);
    }

    static boolean command_is_play(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.equals(PAYLOAD_PLAY)) {
            return true;
        }
        return command_is_pattern (input.tokens, new ArrayList<String>() {{
            addAll(Arrays.stream(PATTERN_PLAY).collect(Collectors.toList()));
        }}.stream().toArray(String[]::new));
    }

    static boolean command_is_continue(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.equals(PAYLOAD_PLAY)) {
            return true;
        }
        return command_is_pattern (input.tokens, new ArrayList<String>() {{
            addAll(Arrays.stream(PATTERN_CONTINUE).collect(Collectors.toList()));
        }}.stream().toArray(String[]::new));
    }

    static boolean command_is_rules(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.equals(PAYLOAD_RULES)) {
            return true;
        }
        return command_is_pattern (input.tokens, new ArrayList<String>() {{
            if(ms.in_rules) {
                addAll(Arrays.stream(PATTERN_FARTHER).collect(Collectors.toList()));
            }
            add(PATTERN_RULES);
        }}.stream().toArray(String[]::new));
    }

    static String command_as_player(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.startsWith("player:")) {
            return input.payload;
        }
        if(command_is_pattern(input.tokens, PATTERN_ENEMY)) {
            return PAYLOAD_PLAYER_ENEMY;
        }
        if(command_is_pattern(input.tokens, PATTERN_ME)) {
            return PAYLOAD_PLAYER_ME;
        }
        if(command_is_pattern(input.tokens, new String[]{PATTERN_BOTH})) {
            return PAYLOAD_PLAYER_BOTH;
        }
        return "";
    }

    static boolean command_is_ready(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.equals(PAYLOAD_READY)) {
            return true;
        }
        return command_is_pattern(input.tokens, PATTERN_READY);
    }

    static String command_as_shoot(InputHolder input, int[] shoot, ServletContext servletContext) {
        shoot[0] = shoot[1] = -1;
        String[] tmp = new String[2];
        boolean found = false;
        if (input.tokens.length >= 2) {
            tmp[0] = input.tokens[0].substring(0, 1);
            tmp[1] = input.tokens[input.tokens.length - 1];
            found = true;
        } else {
            tmp[0] = input.tokens[0].substring(0, 1);
            tmp[1] = input.tokens[0].substring(1);
            found = true;
        }
        if (found) {
            if (letters.containsKey(tmp[0])) {
                shoot[1] = letters.get(tmp[0]) % 10;
                try {
                    shoot[0] = Integer.parseInt(tmp[1]) - 1;
                    if(shoot[0] < 0 || shoot[0] > 9) {
                        shoot[1] = shoot[0] = -1;
                    }
                } catch (NumberFormatException ex) {
                    if (numericals.containsKey(tmp[1])) {
                        shoot[0] = numericals.get(tmp[1]);
                    } else {
                        shoot[1] = -1;
                    }
                }
            }
        }
        return shoot[0] == -1 ? "milk" : "shoot";
    }

    static String command_as_answer(InputHolder input, MarusiaSession ms) {
        if (input.payload != null && input.payload.startsWith("answer:")) {
            return input.payload;
        }
        if(command_is_pattern(input.tokens, PATTERN_MISSED)) {
            return PAYLOAD_ANSWER_MISSED;
        }
        if(command_is_pattern(input.tokens, PATTERN_INJURED)) {
            return PAYLOAD_ANSWER_INJURED;
        }
        if(command_is_pattern(input.tokens, PATTERN_KILLED)) {
            return PAYLOAD_ANSWER_KILLED;
        }
        return "";
    }

    static boolean command_is_pattern(String[] tokens, String[] pattern) {
        return Arrays.stream(pattern).anyMatch(p -> {
            String pat = String.join(" ", p.toLowerCase().split("\\s+"));
            return String.join(" ", tokens).equals(pat);
        });
    }

}
