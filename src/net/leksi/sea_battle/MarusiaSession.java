package net.leksi.sea_battle;

import org.json.JSONObject;

public class MarusiaSession {
    SeaBattleProcessor processor;
    TanimotoComparator comparator;
    int rules_part = 0;
    boolean in_rules = false;
    boolean wait_continue_or_new = false;
    JSONObject last_response = null;
    MarusiaStage stage = MarusiaStage.NEW;
}
