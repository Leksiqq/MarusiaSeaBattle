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

import static net.leksi.sea_battle.MarusiaCommands.*;
import static net.leksi.sea_battle.MarusiaPatterns.*;
import static net.leksi.sea_battle.MarusiaPhrases.*;
import static net.leksi.sea_battle.MarusiaStage.*;

public class MarusiaServlet1 extends HttpServlet {
    static final TreeMap<String, MarusiaSession> sessions = new TreeMap<>();
    static final int[] RULES_IMAGE_ID = new int[]{0, 457239019, 457239017};

    static final String BUTTONS = "buttons";

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
        JSONObject jo = null;
        JSONObject session = null;
        JSONObject request = null;
        JSONObject meta = null;
        boolean sole = false;
        String sessiond_id = null;
        String user_id = null;
        InputHolder input = null;
        boolean session_new = true;
        try (
                InputStreamReader isr = new InputStreamReader(req.getInputStream(), charset.toUpperCase());
                BufferedReader br = new BufferedReader(isr)
        ) {
            content = br.lines().collect(Collectors.joining("\n"));
            jo = new JSONObject(content);
            session = (JSONObject) jo.get("session");
            JSONObject fin_req = request = (JSONObject) jo.get("request");
            meta = (JSONObject) jo.get("meta");
            sole = req.getRequestURI().contains("/sole");
            sessiond_id = sole ? "sole" : session.getString("session_id");
            user_id = sole ? "sole" : session.getString("user_id");
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
        }
        MarusiaSession ms = sessions.getOrDefault(user_id, null);
        if(session_new) {
            if(ms != null) {
                if (ms.stage == WAIT_ENEMY_SHOOT || ms.stage == WAIT_ENEMY_ANSWER || ms.stage == WAIT_ENEMY_READY) {
                    ms.wait_continue_or_new = true;
                }
                ms.rules_part = 0;
                ms.in_rules = false;
            }
        }

        JSONObject jo1 = new JSONObject();
        JSONObject response = new JSONObject();
        response.put(BUTTONS, new JSONArray());

        response.put("end_session", false);


        getServletContext().log(request.toString() + ", " + user_id + ", " + session_new);

        int[] me_shoot = new int[2];
        int[] enemy_shoot = new int[2];

        do {

            if (ms == null) {
                session_new = true;
                ms = new MarusiaSession();
                ms.processor = new SeaBattleProcessor();
                ms.comparator = new TanimotoComparator();
                sessions.put(user_id, ms);
            }
//            String[] phrase = MarusiaPhrases.get_phrase(ms.rules_part);
//            add_to_response(phrase[0], false, phrase[1], response);
//            ms.rules_part++;
//            if(true) break;

            boolean in_rules = ms.in_rules;
            boolean wait_continue_or_new = ms.wait_continue_or_new;

            if ("on_interrupt".equals(input.command)) {
                add_to_response(TXT_SO_LONG, false, TTS_SO_LONG, response);
                response.put("end_session", true);
                sessions.remove(user_id);
                ms = null;
                break;
            }
            if (session_new || command_is_rules(input, ms)) {
                rules_response(response, ms, false);
                break;
            }
            ms.rules_part = 1;
            if(command_is_new_game(input, ms)) {
                sessions.remove(user_id);
                ms = null;
                continue;
            }
            if (in_rules) {
                if (command_is_play(input, ms)) {
                    ms.in_rules = false;
                    ms.rules_part = 1;
                } else {
                    dont_understand_response(response);
                    rules_response(response, ms, true);
                    break;
                }
            }
            if (ms.wait_continue_or_new) {
                if (session_new || in_rules) {
                    continue_game_response(response, false);
                    break;
                }
                if (command_is_play(input, ms)) {
                    ms.wait_continue_or_new = false;
                }
                dont_understand_response(response);
                continue_game_response(response, true);
                break;
            }

            if(ms.stage == NEW) {
                ms.stage = WAIT_TYPE;
                what_way_response(response);
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
                            break;
                        case PAYLOAD_PLAYER_ENEMY:
                            set.add(PlayerType.ENEMY);
                            break;
                        case PAYLOAD_PLAYER_ME:
                            set.add(PlayerType.ME);
                            break;
                    }
                    ms.processor.reset(set);
                    if (set.size() == 1) {
                        if (set.stream().findAny().get() == PlayerType.ME) {
                            say_when_ready_response(response, false);
                            ms.stage = WAIT_ENEMY_READY;
                            break;
                        }
                        start_response(response);
                        ms.stage = WAIT_ENEMY_SHOOT;
                        break;
                    }
                    start_when_ready_response(response);
                    ms.stage = WAIT_ENEMY_SHOOT;
                    break;
                }
                if(!in_rules) {
                    dont_understand_response(response);
                }
                what_way_response(response);
                break;
            }

            if(in_rules) {
                if(ms.last_response != null) {
                    response = ms.last_response;
                }
                break;
            }

            if(ms.stage == WAIT_ENEMY_SHOOT) {
                String command = command_as_shoot(input, enemy_shoot, getServletContext());
                getServletContext().log(command + ", " + Arrays.toString(enemy_shoot));
                if (command.equals("shoot")) {
                    ResultType res = ms.processor.enemy_shoot(enemy_shoot);
                    if (res == ResultType.MISSED || res == ResultType.INJURED || res == ResultType.KILLED) {
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
                        if (!ms.processor.getState().isOutOfGame()) {
                            ms.last_response = response;
                            if (ms.processor.getSet().size() == 2 && ms.processor.getState().state() == State.WAIT_ME_SHOOT) {
                                ms.stage = WAIT_ME_SHOOT;
                                continue;
                            }
                            enemy_move_response(response);
                            break;
                        }
                    }
                }
                if (!ms.processor.getState().isOutOfGame()) {
                    milk_shoot_response(input.ou, response);
                    enemy_move_response(response);
                    break;
                }
            }

            if(ms.stage == WAIT_ENEMY_READY || ms.stage == WAIT_ME_SHOOT) {
                if(ms.stage == WAIT_ENEMY_READY && !command_is_ready(input, ms)) {
                    dont_understand_response(response);
                    say_when_ready_response(response, true);
                    break;
                }
                me_shoot[0] = me_shoot[1] = -1;
                ResultType res = ms.processor.my_shoot(me_shoot);
                if(res == ResultType.OK) {
                    shoot_response(me_shoot, true, true, response);
                    ms.last_response = response;
                    ms.stage = WAIT_ENEMY_ANSWER;
                    break;
                }
            }

            if(ms.stage == WAIT_ENEMY_ANSWER) {
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
                    if(answer != ResultType.OK) {
                        ResultType res = ms.processor.enemy_answer(answer);
                        if (res == ResultType.OK && !ms.processor.getState().isOutOfGame()) {
                            if (ms.processor.getSet().size() == 2 && ms.processor.getState().state() == State.WAIT_ENEMY_SHOOT) {
                                enemy_move_response(response);
                                ms.stage = WAIT_ENEMY_SHOOT;
                            } else {
                                ms.stage = WAIT_ME_SHOOT;
                                continue;
                            }
                        }
                    }
                } else {
                    if (ms.processor.remind_me_last_shoot(me_shoot) == ResultType.OK) {
                        answer_to_my_shoot_response(response);
                    } else {
                        dont_understand_response(response);
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
                        } else if (my_killed < enemy_killed) {
                            add_to_response(TXT_YOU_WIN, true, TTS_YOU_WIN, response);
                        } else {
                            add_to_response(TXT_DRAW, true, TTS_DRAW, response);
                        }
                    } else {
                        add_to_response(TXT_GAME_OVER, true, TTS_GAME_OVER, response);
                    }
                } else if (ms.processor.getState().state() == State.FAILED) {
                    add_to_response(TXT_FAILED,
                            false, TTS_FAILED, response);
                    if (ms.processor.getSet().size() == 2) {
                        add_to_response(TXT_YOU_LOOSE,
                                true, TTS_YOU_LOOSE, response);
                    } else {
                        add_to_response(TXT_GAME_OVER,
                                true, TTS_GAME_OVER, response);
                    }
                }
                response.put("end_session", true);
                ms.stage = NEW;
                break;
            }


            break;
        } while (true);

        if (!response.getBoolean("end_session")) {
            if (!ms.in_rules) {
            }
        }
        getServletContext().log(response.toString());
        jo1.put("response", response);
        jo1.put("session", new JSONObject(session, "session_id", "user_id", "message_id"));
        jo1.put("version", jo.get("version"));
        String tmp = jo1.toString();

        resp.setStatus(200);
        resp.getWriter().println(tmp);

    }

    private void answer_to_my_shoot_response(JSONObject response) {
        add_to_response(TXT_DO_ANSWER, false, TTS_DO_ANSWER, response);
    }

    private void killed_response(JSONObject response) {
        add_to_response(": " + TXT_KILLED, false, TTS_KILLED, response);
    }

    private void injured_response(JSONObject response) {
        add_to_response(": " + TXT_INJURED, false, TTS_INJURED, response);
    }

    private void missed_response(JSONObject response) {
        add_to_response(": " + TXT_MISSED, false, TTS_MISSED, response);
    }

    private void milk_shoot_response(String ou, JSONObject response) {
        add_to_response(TXT_YOU_MISSED_MAP, true,
                TTS_YOU_MISSED_MAP, response);
    }

    private void enemy_move_response(JSONObject response) {
        add_to_response(TXT_YOUR_MOVE, false, TTS_YOUR_MOVE, response);
    }

    private void shoot_response(int[] shoot, boolean bubble, boolean with_buttons, JSONObject response) {
        String text = letter_keys.get(shoot[1]).toUpperCase() + " " + (shoot[0] + 1);
        String tts = "^«" + letter_names.get(shoot[1]) + "»^ -- ^" + numerical_keys.get(shoot[0]) + "^. -- ";
        add_to_response((with_buttons ? TXT_MY_MOVE : "") + text, bubble, (with_buttons ? TTS_MY_MOVE : "" ) + tts, response);
        if (with_buttons) {
            response.getJSONArray(BUTTONS).put(create_button(PATTERN_MISSED[0], PAYLOAD_ANSWER_MISSED));
            response.getJSONArray(BUTTONS).put(create_button(PATTERN_INJURED[0], PAYLOAD_ANSWER_INJURED));
            response.getJSONArray(BUTTONS).put(create_button(PATTERN_KILLED[0], PAYLOAD_ANSWER_KILLED));
        }
    }

    private void start_when_ready_response(JSONObject response) {
        add_to_response(TXT_PLACE_SHIPS_AND_START_GAME, false, TTS_PLACE_SHIPS_AND_START_GAME, response);
    }

    private void start_response(JSONObject response) {
        add_to_response(TXT_START_GAME, false, TTS_START_GAME, response);
    }

    private void continue_game_response(JSONObject response, boolean buttons_only) {
        if(!buttons_only) {
            add_to_response(TXT_GAME_LEFT, true,
                    null, response);
        }
        response.getJSONArray(BUTTONS).put(create_button(PATTERN_CONTINUE, PAYLOAD_READY));
        response.getJSONArray(BUTTONS).put(create_button(PATTERN_NEW_GAME, PAYLOAD_NEW_GANE));
    }

    private void dont_understand_response(JSONObject response) {
        add_to_response(TXT_DONT_UNDERSTAND, false,
                TTS_DONT_UNDERSTAND, response);
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
            response.getJSONArray(BUTTONS).put(create_button(PATTERN_FARTHER, PAYLOAD_RULES));
        } else {
            ms.rules_part = 1;
        }
        response.getJSONArray(BUTTONS).put(create_button(PATTERN_PLAY[0], PAYLOAD_PLAY));
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

    private JSONObject create_button(String title, Object payload) {
        JSONObject res = new JSONObject();
        res.put("title", title.substring(0, 1).toUpperCase() + title.substring(1));
        res.put("payload", new JSONObject() {{
            put("data", payload);
        }});
        return res;
    }

    private void what_way_response(JSONObject response) {
        add_to_response(TXT_GAME_TYPE, true,
                TTS_GAME_TYPE, response);
        response.getJSONArray(BUTTONS).put(create_button(PATTERN_BOTH, PAYLOAD_PLAYER_BOTH));
        response.getJSONArray(BUTTONS).put(create_button(PATTERN_ENEMY[0], PAYLOAD_PLAYER_ENEMY));
        response.getJSONArray(BUTTONS).put(create_button(PATTERN_ME[0], PAYLOAD_PLAYER_ME));
    }

    private void say_when_ready_response(JSONObject response, boolean bubble) {
        add_to_response(TXT_PLACE_SHIPS, bubble, TTS_PLACE_SHIPS, response);
        response.getJSONArray(BUTTONS).put(create_button(PATTERN_READY, PAYLOAD_READY));
    }

}
