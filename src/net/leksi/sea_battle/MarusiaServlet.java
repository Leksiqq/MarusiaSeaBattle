package net.leksi.sea_battle;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class MarusiaServlet extends HttpServlet {
    static final TreeMap<String, MarusiaSession> sessions = new TreeMap<>();
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
    }};
    static final List<String> letter_keys = letters.keySet().stream().collect(Collectors.toList());
    static final List<String> numerical_keys = numericals.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).
            map(v -> v.getKey()).collect(Collectors.toList());
    static final double TANIMOTO_THRESHOLD = 0.6;
    static final int[] RULES_IMAGE_ID = new int[]{0, 457239019, 457239017};

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ct = req.getHeader("content-type");
        String[] parts = ct.split(";");
        String charset = "utf-8";
        for (int i = 1; i < parts.length; i++) {
            String[] name_value = parts[i].split("=");
            String value = "";
            if (name_value.length == 2) {
                value = name_value[1];
            }
            if (name_value[0].equalsIgnoreCase("charset")) {
                charset = value;
            }
        }
        String content = null;
        try (
                InputStreamReader isr = new InputStreamReader(req.getInputStream(), charset.toUpperCase());
                BufferedReader br = new BufferedReader(isr)
        ) {
            content = br.lines().collect(Collectors.joining("\n"));
        }
        JSONObject jo = new JSONObject(content);
        SeaBattleProcessor proc = null;
        TanimotoComparator cmp = null;
        JSONObject session = (JSONObject) jo.get("session");
        JSONObject request = (JSONObject) jo.get("request");
        JSONObject meta = (JSONObject) jo.get("meta");
        boolean sole = req.getRequestURI().contains("/sole");
        String sessiond_id = sole ? "sole" : session.getString("session_id");
        String user_id = sole ? "sole" : session.getString("user_id");
        InputHolder input = new InputHolder() {
            {
                tokens = request.getJSONObject("nlu").getJSONArray("tokens").toList().stream().toArray(String[]::new);
                command = request.getString("command");
                ou = request.getString("original_utterance");
                payload = request.getString("type").equals("ButtonPressed") && request.has("payload") ?
                        request.getJSONObject("payload").getString("data") : null;
            }
        };
        MarusiaSession ms = sessions.getOrDefault(user_id, null);
        boolean session_new = session.getBoolean("new");
        boolean continue_game = session_new && ms != null;
        if (ms == null) {
            session_new = true;
            proc = new SeaBattleProcessor();
            cmp = new TanimotoComparator();
            ms = new MarusiaSession();
            ms.processor = proc;
            ms.comparator = cmp;
            sessions.put(user_id, ms);
        } else {
            proc = ms.processor;
            cmp = ms.comparator;
        }
        JSONObject jo1 = new JSONObject();
        JSONObject response = new JSONObject();
        response.put("buttons", new JSONArray());

        response.put("end_session", false);

        getServletContext().log(request.toString() + ", " + user_id + ", " + session_new);

        int[] me_shoot = new int[]{-1, -1};
        int[] enemy_shoot = new int[]{-1, -1};

        if ("on_interrupt".equals(input.command)) {
            getServletContext().log("1");
            response.put("text", "До встречи!");
            response.put("end_session", true);
        } else if (continue_game) {
            getServletContext().log("2");
            continue_game_response(response);
        } else if (session_new || command_is_rules(input, cmp)) {
            getServletContext().log("3");
            rules_response(response, ms, false);
        } else if (command_is_new_game(input, cmp)) {
            getServletContext().log("4");
            sessions.remove(user_id);
            proc = new SeaBattleProcessor();
            cmp = new TanimotoComparator();
            ms = new MarusiaSession();
            ms.processor = proc;
            ms.comparator = cmp;
            sessions.put(user_id, ms);
            rules_response(response, ms, false);
        } else if (command_is_play(input, cmp)) {
            getServletContext().log("5");
            if (ms.last_response != null) {
                getServletContext().log("5.1");
                response = ms.last_response;
            } else if (proc.getState().state() == State.WAIT_RESET) {
                getServletContext().log("5.2");
                what_way_response(response);
            } else if (!proc.getState().isOutOfGame() && (proc.getState().state() == State.WAIT_FIRST_SHOOT ||
                    proc.getState().state() == State.WAIT_ME_SHOOT)) {
                getServletContext().log("5.3");
                proc.my_shoot(me_shoot);
            } else {
                getServletContext().log("5.4");
                dont_understand_response(input.ou, response);
            }
        } else if (command_is_ready(input, cmp)) {
            getServletContext().log("7");
            if (!proc.getState().isOutOfGame() && (proc.getState().state() == State.WAIT_FIRST_SHOOT ||
                    proc.getState().state() == State.WAIT_ME_SHOOT)) {
                getServletContext().log("7.1");
                proc.my_shoot(me_shoot);
            } else {
                getServletContext().log("7.2");
                dont_understand_response(input.ou, response);
            }
        } else if(ms.in_rules) {
            dont_understand_response(input.ou, response);
            rules_response(response, ms, true);
        } else {
            getServletContext().log("6");
            getServletContext().log(input.toString());
            ms.rules_part = 1;
            ms.in_rules = false;
            if (proc.getState().state() == State.WAIT_RESET) {
                getServletContext().log("6.1");
                String command = command_as_player(input, cmp);
                if (command.startsWith("player:")) {
                    getServletContext().log("6.2");
                    EnumSet<PlayerType> set = EnumSet.noneOf(PlayerType.class);
                    switch (command) {
                        case "player:both":
                            set.add(PlayerType.ME);
                            set.add(PlayerType.ENEMY);
                            break;
                        case "player:enemy":
                            set.add(PlayerType.ENEMY);
                            break;
                        case "player:me":
                            set.add(PlayerType.ME);
                            break;
                    }
                    proc.reset(set);
                    if (set.size() == 1) {
                        if (set.stream().findAny().get() == PlayerType.ME) {
                            say_when_ready_response(response);
                        } else {
                            start_response(response);
                        }
                    } else {
                        start_when_ready_response(response);
                    }
                } else {
                    getServletContext().log("6.3");
                    dont_understand_response(input.ou, response);
                    what_way_response(response);
                }
            } else if (!proc.getState().isOutOfGame()) {
                getServletContext().log("6.4");
                if (proc.getState().state() == State.WAIT_FIRST_SHOOT || proc.getState().state() == State.WAIT_ENEMY_SHOOT) {
                    getServletContext().log("6.5");
                    String command = command_as_shoot(input, cmp, enemy_shoot);
                    getServletContext().log(command + ", " + Arrays.toString(enemy_shoot));
                    if (command.equals("shoot")) {
                        ResultType res = proc.enemy_shoot(enemy_shoot);
                        if (res == ResultType.MISSED || res == ResultType.INJURED || res == ResultType.KILLED) {
                            getServletContext().log("6.6");
                            shoot_response(enemy_shoot, false, false, response);
                            switch (res) {
                                case MISSED:
                                    missed_response(response);
                                    break;
                                case INJURED:
                                    injured_response(response);
                                    break;
                                case KILLED:
                                    killed_response(response);
                                    break;
                            }
                            if (!proc.getState().isOutOfGame()) {
                                if (proc.getSet().size() == 2 && proc.getState().state() == State.WAIT_ME_SHOOT) {
                                    proc.my_shoot(me_shoot);
                                } else {
                                    enemy_move(response);
                                }
                            }
                        } else {
                            getServletContext().log("6.7");
                            milk_shoot_response(input.ou, response);
                            enemy_move(response);
                        }
                    } else {
                        getServletContext().log("6.8");
                        milk_shoot_response(input.ou, response);
                        enemy_move(response);
                    }
                } else {
                    getServletContext().log("6.9");
                    String command = command_as_answer(input, cmp);
                    if (command.startsWith("answer:")) {
                        getServletContext().log("6.10");
                        ResultType answer = ResultType.OK;
                        switch (command) {
                            case "answer:missed":
                                answer = ResultType.MISSED;
                                break;
                            case "answer:injured":
                                answer = ResultType.INJURED;
                                break;
                            case "answer:killed":
                                answer = ResultType.KILLED;
                                break;
                        }
                        ResultType res = proc.enemy_answer(answer);
                        if (res == ResultType.OK && !proc.getState().isOutOfGame()) {
                            if (proc.getSet().size() == 2 && proc.getState().state() == State.WAIT_ENEMY_SHOOT) {
                                enemy_move(response);
                            } else {
                                proc.my_shoot(me_shoot);
                            }
                        }
                    } else {
                        getServletContext().log("6.11");
                        if (proc.remind_me_last_shoot(me_shoot) == ResultType.OK) {
                            answer_to_my_shoot_response(response);
                        } else {
                            dont_understand_response(input.ou, response);
                        }
                    }
                }
                if (proc.getState().isOutOfGame()) {
                    getServletContext().log("6.12");
                    if (proc.getState().state() == State.OVER) {
                        if (proc.getSet().size() == 2) {
                            int my_killed = proc.getStat(PlayerType.ME).killed;
                            int enemy_killed = proc.getStat(PlayerType.ENEMY).killed;
                            if (my_killed > enemy_killed) {
                                add_to_response("Вы проиграли", true, null, response);
                            } else if (my_killed < enemy_killed) {
                                add_to_response("Вы победили!", true, null, response);
                            } else {
                                add_to_response("Ничья.", true, null, response);
                            }
                        } else {
                            add_to_response("Игра окончена.", true, null, response);
                        }
                    } else if (proc.getState().state() == State.FAILED) {
                        add_to_response("Вы дали ложный ответ или Ваши корабли расставлены не по правилам.",
                                false, null, response);
                        if (proc.getSet().size() == 2) {
                            add_to_response("Вы проиграли.",
                                    true, null, response);
                        } else {
                            add_to_response("Игра окончена.",
                                    true, null, response);
                        }
                    }
                    response.put("end_session", true);
                    ms.last_response = null;
                }
            }
        }

        if (response.getBoolean("end_session")) {
            sessions.remove(user_id);
        } else {
            if (me_shoot[0] != -1) {
                shoot_response(me_shoot, true, true, response);
            }
            if (!ms.in_rules) {
                ms.last_response = response;
            }
        }
        jo1.put("response", response);
        jo1.put("session", new JSONObject(session, "session_id", "user_id", "message_id"));
        jo1.put("version", jo.get("version"));
        String tmp = jo1.toString();

        resp.setStatus(200);
        resp.getWriter().println(tmp);
    }

    private void milk_shoot_response(String ou, JSONObject response) {
        add_to_response("Вы не попали по карте.", true,
                null, response);
    }

    private boolean command_is_new_game(InputHolder input, TanimotoComparator cmp) {
        if (input.payload != null && input.payload.equals("new_game")) {
            return true;
        }
        cmp.setPattern(input.tokens);
        return (cmp.get_score("заново") > TANIMOTO_THRESHOLD || cmp.get_score("новая") > TANIMOTO_THRESHOLD);
    }

    private void continue_game_response(JSONObject response) {
        add_to_response("Наша игра не была закончена.", true,
                null, response);
        response.getJSONArray("buttons").put(create_button("Продолжить", "ready"));
        response.getJSONArray("buttons").put(create_button("Начать заново", "new_game"));
    }

    private String command_as_answer(InputHolder input, TanimotoComparator cmp) {
        if (input.payload != null && input.payload.startsWith("answer:")) {
            return input.payload;
        }
        cmp.setPattern(input.tokens);
        getServletContext().log("мимо: " + cmp.get_score("мимо"));
        getServletContext().log("подбит: " + cmp.get_score("подбит"));
        getServletContext().log("потоп: " + cmp.get_score("потоп"));
        if (cmp.get_score("мимо") > TANIMOTO_THRESHOLD) {
            return "answer:missed";
        }
        if (cmp.get_score("подбит") > TANIMOTO_THRESHOLD) {
            return "answer:injured";
        }
        if (cmp.get_score("потоп") > TANIMOTO_THRESHOLD) {
            return "answer:killed";
        }
        return "";
    }

    private String command_as_shoot(InputHolder input, TanimotoComparator cmp, int[] shoot) {
        shoot[0] = shoot[1] = -1;
        String[] tmp = new String[2];
        boolean found = false;
        if (input.tokens.length == 2) {
            tmp[0] = input.tokens[0].substring(0, 1);
            tmp[1] = input.tokens[1];
            found = true;
        } else {
            tmp[0] = input.tokens[0].substring(0, 1);
            tmp[1] = input.tokens[0].substring(1);
            found = true;
        }
        if (found) {
            if (letters.containsKey(tmp[0])) {
                shoot[1] = letters.get(tmp[0]);
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

    private String command_as_player(InputHolder input, TanimotoComparator cmp) {
        if (input.payload != null && input.payload.startsWith("player:")) {
            return input.payload;
        }
        if (input.tokens[0].equals("я") || input.tokens.length > 1 && input.tokens[1].equals("я")) {
            return "player:enemy";
        }
        if (input.tokens[0].equals("маруся") || input.tokens.length > 1 && input.tokens[1].equals("маруся")) {
            return "player:me";
        }
        cmp.setPattern(input.tokens);
        if (cmp.get_score("очеред") > TANIMOTO_THRESHOLD) {
            return "player:both";
        }
        return "";
    }

    private boolean command_is_ready(InputHolder input, TanimotoComparator cmp) {
        if (input.payload != null && input.payload.equals("ready")) {
            return true;
        }
        cmp.setPattern(input.tokens);
        return (cmp.get_score("готов") > TANIMOTO_THRESHOLD);
    }

    private boolean command_is_play(InputHolder input, TanimotoComparator cmp) {
        if (input.payload != null && input.payload.equals("play")) {
            return true;
        }
        cmp.setPattern(input.tokens);
        return (cmp.get_score("продолжить") > TANIMOTO_THRESHOLD ||
                cmp.get_score("играть") > TANIMOTO_THRESHOLD ||
                cmp.get_score("играем") > TANIMOTO_THRESHOLD);
    }

    private boolean command_is_rules(InputHolder input, TanimotoComparator cmp) {
        if (input.payload != null && input.payload.equals("rules")) {
            return true;
        }
        cmp.setPattern(input.tokens);
        return (cmp.get_score("правила") > TANIMOTO_THRESHOLD || cmp.get_score("дальше") > TANIMOTO_THRESHOLD);
    }

    private void answer_to_my_shoot_response(JSONObject response) {
        add_to_response("Вы не ответили!", false, null, response);
    }

    private void say_when_ready_response(JSONObject response) {
        add_to_response("Расставляйте корабли.", false, null, response);
        response.getJSONArray("buttons").put(create_button("Готов", "ready"));
    }

    private void rules_response(JSONObject response, MarusiaSession ms, boolean buttons_only) {
        ms.in_rules = true;
        if(!buttons_only) {
            String text = read_file("rules." + ms.rules_part + ".txt");
            String tts = read_file("rules." + ms.rules_part + ".tts");
            add_to_response(text, false, tts, response);
            if (RULES_IMAGE_ID.length > ms.rules_part && RULES_IMAGE_ID[ms.rules_part] != 0) {
                response.put("card", new JSONObject() {{
                    put("type", "BigImage");
                    put("image_id", RULES_IMAGE_ID[ms.rules_part]);
                }});
            }
            ms.rules_part++;
        }
        if (file_exists("rules." + ms.rules_part + ".txt")) {
            response.getJSONArray("buttons").put(create_button("Дальше", "rules"));
        } else {
            ms.rules_part = 1;
        }
        response.getJSONArray("buttons").put(create_button("Играем", "play"));
    }

    private void add_to_response(String text, boolean bubble, String tts, JSONObject response) {
        if (text != null && !text.isBlank()) {
            if (bubble || response.has("text") && response.get("text") instanceof JSONArray) {
                response.accumulate("text", text);
            } else {
                response.put("text", (response.has("text") ? response.getString("text") : "") + text);
            }
        }
        if (tts != null && !tts.isBlank()) {
            response.put("tts", (response.has("tts") ? response.getString("tts") : "") + tts);
        }
    }

    private void killed_response(JSONObject response) {
        add_to_response(": Потоплен. ", false, " -- потоплен. ", response);
    }

    private void injured_response(JSONObject response) {
        add_to_response(": Подбит. ", false, " -- подбит. ", response);
    }

    private void missed_response(JSONObject response) {
        add_to_response(": Мимо. ", false, " -- мимо. ", response);
    }

    private void shoot_response(int[] shoot, boolean bubble, boolean with_buttons, JSONObject response) {
        String text = letter_keys.get(shoot[1]).toUpperCase() + " " + (shoot[0] + 1);
        String tts = letter_keys.get(shoot[1]) + " " + numerical_keys.get(shoot[0]);
        add_to_response((with_buttons ? "Мой ход: " : "") + text, bubble, (with_buttons ? "-- мой ход -- " : "" ) + tts, response);
        if (with_buttons) {
            response.getJSONArray("buttons").put(create_button("Мимо", "answer:missed"));
            response.getJSONArray("buttons").put(create_button("Подбит", "answer:injured"));
            response.getJSONArray("buttons").put(create_button("Потоплен", "answer:killed"));
        }
    }

    private void start_response(JSONObject response) {
        add_to_response("Начинайте игру.", false, null, response);
    }

    private void enemy_move(JSONObject response) {
        add_to_response("Ваш ход.", true, "-- ваш ход", response);
    }

    private void start_when_ready_response(JSONObject response) {
        add_to_response("Расставляйте корабли и делайте первый ход.", false, "расставляйте корабли -- и делайте первый ход.", response);
    }

    private void dont_understand_response(String command, JSONObject response) {
        add_to_response("Не поняла Вас! ", false,
                null, response);
    }

    private void what_way_response(JSONObject response) {
        add_to_response("Выберите, как будем играть.", true,
                null, response);
        response.getJSONArray("buttons").put(create_button("По очереди", "player:both"));
        response.getJSONArray("buttons").put(create_button("Только я", "player:enemy"));
        response.getJSONArray("buttons").put(create_button("Только Маруся", "player:me"));
    }

    private JSONObject create_button(String title, Object payload) {
        JSONObject res = new JSONObject();
        res.put("title", title);
        res.put("payload", new JSONObject() {{
            put("data", payload);
        }});
        return res;
    }

    private boolean file_exists(String path) {
        File file = new File(getServletContext().getRealPath(("/WEB-INF/" + path).replaceAll("/{2,}", "/")));
        return file.exists();
    }

    private String read_file(String path) {
        StringBuilder sb = new StringBuilder();
        try (
                FileInputStream fis = new FileInputStream(getServletContext().getRealPath(("/WEB-INF/" + path).replaceAll("/{2,}", "/")));
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr)
        ) {
            br.lines().forEach(line -> sb.append(line).append("\n"));
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
