package net.leksi.sea_battle;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static net.leksi.sea_battle.MarusiaCommands.*;
import static net.leksi.sea_battle.MarusiaPatterns.*;
import static net.leksi.sea_battle.MarusiaPhrases.*;
import static net.leksi.sea_battle.MarusiaStage.*;

public class MarusiaServlet extends HttpServlet {
    static private final Map<String, MarusiaSession> sessions = Collections.synchronizedMap(new TreeMap<>());

    static private volatile boolean expirator_working = false;
    static private volatile long prev_expirator_working_time = 0;
    static private final long EXPIRATOR_PERIOD_SECS = 10;
    static private final long EXPIRATION_TIME_SECS = 60;

    static private volatile String data_dir = null;

    static final int[] RULES_IMAGE_ID = new int[]{0, 457239019, 457239017};

    static final String BUTTONS = "buttons";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        check_data_dir(req);
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
        JSONObject jo = null;
        JSONObject session = null;
        JSONObject request = null;
        boolean sole = false;
        boolean speaker = false;
        String user_id = null;
        InputHolder input = null;
        boolean session_new = true;
        int message_id;
        try (
                InputStreamReader isr = new InputStreamReader(req.getInputStream(), charset.toUpperCase());
                BufferedReader br = new BufferedReader(isr)
        ) {
            content = br.lines().collect(Collectors.joining("\n"));
            jo = new JSONObject(content);
            session = (JSONObject) jo.get("session");
            JSONObject fin_req = request = (JSONObject) jo.get("request");
            sole = req.getRequestURI().contains("/sole");
            speaker = req.getRequestURI().contains("/speaker") || session.getJSONObject("application").getString("application_type").equals("speaker");
            user_id = sole ? "sole" : session.getString("user_id");
            message_id = session.getInt("message_id");
            input = new InputHolder() {
                {
                    tokens = fin_req.getJSONObject("nlu").getJSONArray("tokens").toList().stream().toArray(String[]::new);
                    command = fin_req.getString("command");
                    ou = fin_req.getString("original_utterance");
                    payload = fin_req.getString("type").equals("ButtonPressed") && fin_req.has("payload") ?
                            fin_req.getJSONObject("payload").getString("data") : null;
                }
            };
            session_new = session.getBoolean("new");
        } catch (Exception ex) {
            resp.setStatus(500);
            resp.getWriter().println("Request failed");
            ex.printStackTrace(resp.getWriter());
            resp.getWriter().println("content: " + content);
            return;
        }
        MarusiaSession ms = sessions.getOrDefault(user_id, null);
        if(session_new) {
            if(ms != null) {
                if (ms.stage == WAIT_ENEMY_SHOOT || ms.stage == WAIT_ENEMY_ANSWER || ms.stage == WAIT_ENEMY_READY) {
                    ms.wait_continue_or_new = true;
                    ms.rules_part = 1;
                } else {
                    ms.processor.reset(EnumSet.noneOf(PlayerType.class));
                    ms.last_response = null;
                    ms.repeat_response = null;
                    ms.stage = NEW;
                    ms.wait_continue_or_new = false;
                    ms.rules_part = 0;
                }
                ms.in_rules = false;
            }
        }
        JSONObject jo1 = new JSONObject();
        JSONObject response = new JSONObject();
        if(!speaker) {
            response.put(BUTTONS, new JSONArray());
        }

        response.put("end_session", false);

        int[] me_shoot = new int[2];
        int[] enemy_shoot = new int[2];

        long now = new Date().getTime();

        boolean wait_continue_or_new = ms != null && ms.wait_continue_or_new;

        do {

            if (ms == null) {
                session_new = true;
                ms = new MarusiaSession();
                ms.user_id = user_id;
                ms.speaker = speaker;
                ms.processor = new SeaBattleProcessor();
            }

            synchronized (ms) {
                ms.timestamp = now;
            }

            boolean in_rules = ms.in_rules;

            if ("on_interrupt".equals(input.command)) {
                add_to_response(TXT_SO_LONG, false, TTS_SO_LONG, response);
                response.put("end_session", true);
                sessions.remove(user_id);
                MarusiaSession ms1 = ms;
                new Thread(() -> archive_session(ms1)).start();
                ms = null;
                break;
            }
            if (session_new && !ms.wait_continue_or_new || command_is_rules(input, ms)) {
                rules_response(response, ms, false, speaker);
                if(speaker) {
                    if(ms.rules_part != -1) {
                        rules_or_play_response(response, true, speaker);
                    } else {
                        in_rules_end_response(response, true, speaker);
                    }
                }
                if(ms.rules_part == -1) {
                    ms.rules_part = 1;
                }
                break;
            }
            if(command_is_repeat(input, ms) && ms.repeat_response != null) {
                response = ms.repeat_response;
                break;
            }
            if(command_is_help(input, ms)) {
                if(in_rules) {
                    in_rules_help_response(response, false, speaker);
                    break;
                }
                if(ms.wait_continue_or_new) {
                    wait_continue_or_new_help_response(response, false, speaker);
                    break;
                }
                if(ms.stage == WAIT_ENEMY_ANSWER) {
                    wait_answer_help_response(response, false, speaker);
                    break;
                }
                if(ms.stage == WAIT_ENEMY_SHOOT) {
                    wait_shoot_help_response(response, false, speaker);
                    break;
                }
                if(ms.stage == WAIT_ENEMY_READY) {
                    wait_ready_help_response(response, false, speaker);
                    break;
                }
                if(ms.stage == WAIT_TYPE) {
                    wait_type_help_response(response, false, speaker);
                    break;
                }
            }
            if(command_is_new_game(input, ms)) {
                wait_continue_or_new = false;
                ms.processor.reset(EnumSet.noneOf(PlayerType.class));
                ms.last_response = null;
                ms.repeat_response = null;
                ms.stage = NEW;
                ms.wait_continue_or_new = false;
                ms.in_rules = false;
                ms.rules_part = 0;
            }
            if (in_rules) {
                if (command_is_play(input, ms)) {
                    in_rules = ms.in_rules = false;
                    ms.rules_part = 1;
                    if(ms.stage == WAIT_ENEMY_ANSWER && ms.last_response != null) {
                        response = ms.last_response;
                        break;
                    }
                    if(ms.stage == WAIT_ENEMY_SHOOT && ms.last_response != null) {
                        response = ms.last_response;
                        break;
                    }

                } else {
//                    dont_understand_response(response, speaker);
                    if(!speaker) {
                        rules_response(response, ms, !speaker, speaker);
                    } else {
                        in_rules_help_response(response, true, speaker);
                    }
                    break;
                }
            }
            if (ms.wait_continue_or_new) {
                if (session_new || in_rules) {
                    continue_game_response(response, false, speaker);
                    break;
                }
                if (command_is_continue(input, ms)) {
                    ms.wait_continue_or_new = false;
                } else {
//                    dont_understand_response(response, speaker);
                    continue_game_response(response, true, speaker);
                    break;
                }
            }

            if(ms.stage == NEW) {
                ms.stage = WAIT_TYPE;
                what_way_response(response, speaker);
                break;
            }

            if(ms.stage == WAIT_TYPE) {
                String command = command_as_player(input, ms);
                if (command.startsWith("player:")) {
                    EnumSet<PlayerType> set = EnumSet.noneOf(PlayerType.class);
                    switch (command) {
                        case PAYLOAD_PLAYER_BOTH:
                            set.add(PlayerType.ME);
                            set.add(PlayerType.ENEMY);
                            ms.both_started++;
                            break;
                        case PAYLOAD_PLAYER_ENEMY:
                            set.add(PlayerType.ENEMY);
                            ms.enemy_started++;
                            break;
                        case PAYLOAD_PLAYER_ME:
                            set.add(PlayerType.ME);
                            ms.me_started++;
                            break;
                    }
                    ms.processor.reset(set);
                    if (set.size() == 1) {
                        if (set.stream().findAny().get() == PlayerType.ME) {
                            say_when_ready_response(response, false, speaker);
                            if(speaker) {
                                wait_ready_help_response(response, true, speaker);
                            }
                            ms.stage = WAIT_ENEMY_READY;
                            break;
                        }
                        start_response(response, speaker);
                        ms.stage = WAIT_ENEMY_SHOOT;
                        break;
                    }
                    start_when_ready_response(response, speaker);
                    ms.stage = WAIT_ENEMY_SHOOT;
                    break;
                }
                if(!in_rules) {
//                    dont_understand_response(response, speaker);
                }
                what_way_response(response, speaker);
                break;
            }

            if(in_rules) {
                if(ms.last_response != null) {
                    response = ms.last_response;
                }
                break;
            }

            if(ms.stage == WAIT_ENEMY_SHOOT) {
                if(wait_continue_or_new && ms.last_response != null) {
                    response = ms.last_response;
                } else {
                    String command = command_as_shoot(input, enemy_shoot, getServletContext());
                    if (command.equals("shoot")) {
                        ResultType res = ms.processor.enemy_shoot(enemy_shoot);
                        if (res == ResultType.MISSED || res == ResultType.INJURED || res == ResultType.KILLED) {
                            shoot_response(enemy_shoot, false, false, response, speaker);
                            switch (res) {
                                case MISSED:
                                    missed_response(response, speaker);
                                    break;
                                case INJURED:
                                    injured_response(response, speaker);
                                    break;
                                case KILLED:
                                    killed_response(response, speaker);
                                    break;
                            }
                            if (!ms.processor.getState().isOutOfGame()) {
                                if (ms.processor.getSet().size() == 2 && ms.processor.getState().state() == State.WAIT_ME_SHOOT) {
                                    ms.stage = WAIT_ME_SHOOT;
                                    ms.last_response = response;
                                    continue;
                                }
                                enemy_move_response(response, speaker);
                                ms.last_response = response;
                                break;
                            }
                        }
                    }
                    if (!ms.processor.getState().isOutOfGame()) {
                        milk_shoot_response(input.ou, response, speaker);
                        enemy_move_response(response, speaker);
                        ms.last_response = response;
                        break;
                    }
                }
            }

            if(ms.stage == WAIT_ENEMY_READY || ms.stage == WAIT_ME_SHOOT) {
                if(wait_continue_or_new) {
                    if(ms.stage == WAIT_ENEMY_READY) {
                        say_when_ready_response(response, false, speaker);
                        if(speaker) {
                            wait_ready_help_response(response, true, speaker);
                        }
                        break;
                    }
                } else {
                    if (ms.stage == WAIT_ENEMY_READY && !command_is_ready(input, ms)) {
//                        dont_understand_response(response, speaker);
                        say_when_ready_response(response, true, speaker);
                        if (speaker) {
                            wait_ready_help_response(response, true, speaker);
                        }
                        break;
                    }
                    me_shoot[0] = me_shoot[1] = -1;
                    ResultType res = ms.processor.my_shoot(me_shoot);
                    if (res == ResultType.OK) {
                        shoot_response(me_shoot, true, true, response, speaker);
                        ms.last_response = response;
                        ms.stage = WAIT_ENEMY_ANSWER;
                        break;
                    }
                }
            }

            if(ms.stage == WAIT_ENEMY_ANSWER) {
                if(wait_continue_or_new && ms.last_response != null) {
                    response = ms.last_response;
                } else {
                    String command = command_as_answer(input, ms);
                    if (command.startsWith("answer:")) {
                        ResultType answer = ResultType.OK;
                        switch (command) {
                            case PAYLOAD_ANSWER_MISSED:
                                answer = ResultType.MISSED;
                                break;
                            case PAYLOAD_ANSWER_INJURED:
                                answer = ResultType.INJURED;
                                break;
                            case PAYLOAD_ANSWER_KILLED:
                                answer = ResultType.KILLED;
                                break;
                        }
                        if (answer != ResultType.OK) {
                            ResultType res = ms.processor.enemy_answer(answer);
                            if (res == ResultType.OK && !ms.processor.getState().isOutOfGame()) {
                                if (ms.processor.getSet().size() == 2 && ms.processor.getState().state() == State.WAIT_ENEMY_SHOOT) {
                                    enemy_move_response(response, speaker);
                                    ms.last_response = response;
                                    ms.stage = WAIT_ENEMY_SHOOT;
                                } else {
                                    ms.stage = WAIT_ME_SHOOT;
                                    continue;
                                }
                            }
                        }
                    } else {
                        if (ms.processor.remind_me_last_shoot(me_shoot) == ResultType.OK) {
                            answer_to_my_shoot_response(response, speaker);
                            wait_answer_help_response(response, true, speaker);
                        } else {
//                            dont_understand_response(response, speaker);
                        }
                    }
                }
            }

            if(ms.processor.getState().isOutOfGame()) {
                if (ms.processor.getState().state() == State.OVER) {
                    if (ms.processor.getSet().size() == 2) {
                        int my_killed = ms.processor.getStat(PlayerType.ME).killed;
                        int enemy_killed = ms.processor.getStat(PlayerType.ENEMY).killed;
                        if (my_killed > enemy_killed) {
                            add_to_response(TXT_YOU_LOOSE, true, TTS_YOU_LOOSE, response);
                            ms.both_lost++;
                        } else if (my_killed < enemy_killed) {
                            add_to_response(TXT_YOU_WIN, true, TTS_YOU_WIN, response);
                            ms.both_winned++;
                        }
                    } else {
                        add_to_response(TXT_GAME_OVER, true, TTS_GAME_OVER, response);
                        if(ms.processor.getSet().contains(PlayerType.ME)) {
                            ms.me_finished++;
                        } else {
                            ms.enemy_finished++;
                        }
                    }
                } else if (ms.processor.getState().state() == State.FAILED) {
                    add_to_response(TXT_FAILED, true, TTS_FAILED, response);
                    if (ms.processor.getSet().size() == 2) {
                        ms.both_failed++;
                    } else {
                        if(ms.processor.getSet().contains(PlayerType.ME)) {
                            ms.me_failed++;
                        } else {
                            ms.enemy_failed++;
                        }
                    }
                }
                add_to_response(TXT_PLAY_AGAIN, true, TTS_PLAY_AGAIN, response);
                ms.stage = NEW;
                break;
            }


            break;
        } while (true);

        if (!response.getBoolean("end_session")) {
            if (!ms.in_rules) {
            }
        }
        if(!response.has("text")) {
            dont_understand_response(response, speaker);
        }
        jo1.put("response", response);
        if(ms != null) {
            ms.repeat_response = response;
        }
        jo1.put("session", new JSONObject(session, "session_id", "user_id", "message_id"));
        jo1.put("version", jo.get("version"));
        String tmp = jo1.toString();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(200);
        resp.getWriter().println(tmp);

        if(ms != null) {
            sessions.put(user_id, ms);
        } else {
            sessions.remove(user_id);
        }

        if(!expirator_working) {
            synchronized (MainServlet.class) {
                if(!expirator_working) {
                    if (now - prev_expirator_working_time > EXPIRATOR_PERIOD_SECS * 1000 && !expirator_working) {
                        prev_expirator_working_time = now;
                        expirator_working = true;
                        new Thread(() -> expirator(now)).start();
                    }
                }
            }
        }

    }

    private void wait_type_help_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_WAIT_TYPE_HELP, bubble, TTS_WAIT_TYPE_HELP, response);
    }

    private void wait_continue_or_new_help_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_GAME_LEFT_HELP, bubble, TTS_GAME_LEFT_HELP, response);
    }

    static private void expirator(long now) {
        ArrayList<String> to_remove = new ArrayList<>();
        sessions.forEach((k, v) -> {
            if(now - v.timestamp > EXPIRATION_TIME_SECS * 1000) {
                v.timestamp = now;
                to_remove.add(k);
            }
        });
        if(!to_remove.isEmpty()) {
            try(
                    FileWriter fr = new FileWriter(Paths.get(data_dir, "stat.txt").toString(), true);
                    PrintWriter pw = new PrintWriter(fr);
                    ) {
                to_remove.forEach(k -> {
                    pw.println("=============================");
                    pw.print(sessions.get(k).getStat());
                    sessions.remove(k);
                });
            } catch (IOException e) { }
            to_remove.forEach(k -> sessions.remove(k));
        }
        synchronized (MainServlet.class) {
            expirator_working = false;
        }
    }

    static private void archive_session(MarusiaSession session) {
        try(
                FileWriter fr = new FileWriter(Paths.get(data_dir, "stat.txt").toString(), true);
                PrintWriter pw = new PrintWriter(fr);
        ) {
            pw.println("=============================");
            pw.print(session.getStat());
        } catch (IOException e) { }
    }

    private void rules_or_play_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_RULES_OR_PLAY, bubble, TTS_RULES_OR_PLAY, response);
    }

    private void wait_shoot_help_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_WAIT_SHOOT_HELP, bubble, TTS_WAIT_SHOOT_HELP, response);
    }

    private void wait_ready_help_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_WAIT_READY_HELP, bubble, TTS_WAIT_READY_HELP, response);
    }

    private void wait_answer_help_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_WAIT_ANSWER_HELP, bubble, TTS_WAIT_ANSWER_HELP, response);
    }

    private void in_rules_end_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_RULES_END, bubble, TTS_RULES_END, response);
    }

    private void in_rules_help_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_RULES_HELP, bubble, TTS_RULES_HELP, response);
    }

    private void answer_to_my_shoot_response(JSONObject response, boolean speaker) {
        add_to_response(TXT_DO_ANSWER, false, TTS_DO_ANSWER, response);
    }

    private void killed_response(JSONObject response, boolean speaker) {
        add_to_response(": " + TXT_KILLED, false, TTS_KILLED, response);
    }

    private void injured_response(JSONObject response, boolean speaker) {
        add_to_response(": " + TXT_INJURED, false, TTS_INJURED, response);
    }

    private void missed_response(JSONObject response, boolean speaker) {
        add_to_response(": " + TXT_MISSED, false, TTS_MISSED, response);
    }

    private void milk_shoot_response(String ou, JSONObject response, boolean speaker) {
        add_to_response(TXT_YOU_MISSED_MAP, true,
                TTS_YOU_MISSED_MAP, response);
    }

    private void enemy_move_response(JSONObject response, boolean speaker) {
        add_to_response(TXT_YOUR_MOVE, true, TTS_YOUR_MOVE, response);
    }

    private void shoot_response(int[] shoot, boolean bubble, boolean with_buttons, JSONObject response, boolean speaker) {
        String text = letter_keys.get(shoot[1]).toUpperCase() + " " + (shoot[0] + 1);
        String tts = "^«" + letter_names.get(shoot[1]) + "»^ -- ^" + numerical_keys.get(shoot[0]) + "^. -- ";
        add_to_response((with_buttons ? TXT_MY_MOVE : "") + text, bubble, (with_buttons ? TTS_MY_MOVE : "" ) + tts, response);
        if (with_buttons) {
            create_button(PATTERN_MISSED[0], PAYLOAD_ANSWER_MISSED, response);
            create_button(PATTERN_INJURED[0], PAYLOAD_ANSWER_INJURED, response);
            create_button(PATTERN_KILLED[0], PAYLOAD_ANSWER_KILLED, response);
        }
    }

    private void start_when_ready_response(JSONObject response, boolean speaker) {
        add_to_response(TXT_PLACE_SHIPS_AND_START_GAME, false, TTS_PLACE_SHIPS_AND_START_GAME, response);
    }

    private void start_response(JSONObject response, boolean speaker) {
        add_to_response(TXT_START_GAME, false, TTS_START_GAME, response);
    }

    private void continue_game_response(JSONObject response, boolean buttons_only, boolean speaker) {
        if(!buttons_only) {
            add_to_response(TXT_GAME_LEFT, true, TTS_GAME_LEFT +  " " + TTS_GAME_LEFT_HELP, response);
        }
        create_button(PATTERN_CONTINUE[0], PAYLOAD_READY, response);
        create_button(PATTERN_NEW_GAME, PAYLOAD_NEW_GAME, response);
    }

    private void dont_understand_response(JSONObject response, boolean speaker) {
        add_to_response(TXT_DONT_UNDERSTAND, false, TTS_DONT_UNDERSTAND, response);
    }

    private void rules_response(JSONObject response, MarusiaSession ms, boolean buttons_only, boolean speaker) {
        ms.in_rules = true;
        if (!buttons_only) {
            String text = read_file("rules." + ms.rules_part + ".txt");
            String tts = read_file("rules." + ms.rules_part + ".tts");
            add_to_response(text, false, tts, response);
            if (!speaker && RULES_IMAGE_ID.length > ms.rules_part && RULES_IMAGE_ID[ms.rules_part] != 0) {
                response.put("card", new JSONObject() {{
                    put("type", "BigImage");
                    put("image_id", RULES_IMAGE_ID[ms.rules_part]);
                }});
            }
            ms.rules_part++;
        }
        if (file_exists("rules." + ms.rules_part + ".txt")) {
            create_button(PATTERN_FARTHER[0], PAYLOAD_RULES, response);
        } else {
            ms.rules_part = -1;
        }
        create_button(PATTERN_PLAY[0], PAYLOAD_PLAY, response);
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

    private void create_button(String title, Object payload, JSONObject response) {
        if(response.has(BUTTONS)) {
            JSONObject res = new JSONObject();
            res.put("title", title.substring(0, 1).toUpperCase() + title.substring(1));
            res.put("payload", new JSONObject() {{
                put("data", payload);
            }});
            response.getJSONArray(BUTTONS).put(res);
        }
    }

    private void what_way_response(JSONObject response, boolean speaker) {
        String[] text = read_file("what_way.txt").split("\\n");
        String tts = read_file("what_way" + (speaker ? ".speaker" : "") + ".tts");
        for(int i = 0; i < text.length; i++) {
            add_to_response(text[i], i > 0, i == 0 ? tts : null, response);
        }
        create_button(PATTERN_BOTH, PAYLOAD_PLAYER_BOTH, response);
        create_button(PATTERN_ENEMY[0], PAYLOAD_PLAYER_ENEMY, response);
        create_button(PATTERN_ME[0], PAYLOAD_PLAYER_ME, response);
    }

    private void say_when_ready_response(JSONObject response, boolean bubble, boolean speaker) {
        add_to_response(TXT_PLACE_SHIPS, bubble, TTS_PLACE_SHIPS, response);
        create_button(PATTERN_READY[0], PAYLOAD_READY, response);
    }


    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        check_data_dir(req);
        PrintWriter pw = resp.getWriter();
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(200);
        if(req.getRequestURI().equals(req.getContextPath() + "/" + getServletContext().getInitParameter("marusia.sessions.path"))
                || req.getRequestURI().equals(req.getContextPath() + getServletContext().getInitParameter("marusia.sessions.path"))) {
            int[] count = new int[1];
            long now = new Date().getTime();
            pw.println("Sessions");
            sessions.forEach((k, v) -> {
                pw.println("=============================");
                pw.println("    stage: " + v.stage);
                pw.println("    status: " + v.processor.getState().state());
                pw.print("    " + v.getStat().replace("\n", "\n    "));
                pw.println("" + (EXPIRATION_TIME_SECS * 1000 - now + v.timestamp) / 1000 + " sec. before expiration");
                count[0]++;
            });
            pw.println("=============================");
            pw.println("Total: " + count[0]);
        } else {
            pw.println("it works!");
        }
    }

    private void check_data_dir(HttpServletRequest req) {
        if (data_dir == null) {
            synchronized (MarusiaServlet.class) {
                if (data_dir == null) {
                    data_dir = getServletContext().getRealPath(("/data"));
                }
            }
        }
    }

}
