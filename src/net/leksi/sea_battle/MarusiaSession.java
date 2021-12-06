package net.leksi.sea_battle;

import org.json.JSONObject;

import javax.xml.crypto.Data;
import java.util.Date;

public class MarusiaSession {
    String user_id;
    SeaBattleProcessor processor;
    int rules_part = 0;
    boolean in_rules = false;
    boolean wait_continue_or_new = false;
    JSONObject last_response = null;
    JSONObject repeat_response = null;
    MarusiaStage stage = MarusiaStage.NEW;
    long timestamp = 0;
    int me_started = 0;
    int enemy_started = 0;
    int both_started = 0;
    int me_finished = 0;
    int enemy_finished = 0;
    int me_failed = 0;
    int enemy_failed = 0;
    int both_failed = 0;
    int both_winned = 0;
    int both_lost = 0;
    boolean speaker = false;
    final long started = new Date().getTime();

    public String getStat() {
        return "user_id: " + user_id + "\n"
                + "me_started: " + me_started + "\n"
                + "enemy_started: " + enemy_started + "\n"
                + "both_started: " + both_started + "\n"
                + "me_finished: " + me_finished + "\n"
                + "enemy_finished: " + enemy_finished + "\n"
                + "me_failed: " + me_failed + "\n"
                + "enemy_failed: " + enemy_failed + "\n"
                + "both_failed: " + both_failed + "\n"
                + "both_winned: " + both_winned + "\n"
                + "both_lost: " + both_lost + "\n"
                + "speaker: " + speaker + "\n"
                + "lifetime: " + (timestamp - started) / 1000 + "sec.\n";
    }
}
