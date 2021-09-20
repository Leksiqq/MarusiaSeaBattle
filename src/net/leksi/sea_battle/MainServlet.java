package net.leksi.sea_battle;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.TreeMap;

public class MainServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession sess = req.getSession();
        TreeMap<String, ArrayList<String>> query_map = new TreeMap<>();
        SeaBattleProcessor proc = (SeaBattleProcessor)sess.getAttribute("game");
        if(proc == null) {
            proc = new SeaBattleProcessor();
            sess.setAttribute("game", proc);
        }
        ResultType shoot_result = null;
        int[] shoot = new int[2];
        EnumSet<PlayerType> types = null;
        ResultType res = ResultType.OK;
        if(req.getQueryString() != null) {
            types = EnumSet.noneOf(PlayerType.class);
            String[] parts = req.getQueryString().split("&");
            for(String part: parts) {
                String[] name_value = part.trim().split("\\+*=\\+*");
                String name = URLDecoder.decode(name_value[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(name_value[1], StandardCharsets.UTF_8);
                if(!query_map.containsKey(name)) {
                    query_map.put(name, new ArrayList<>());
                }
                query_map.get(name).add(value);
            }
            if(query_map.containsKey("players")) {
                proc = new SeaBattleProcessor();
                sess.setAttribute("game", proc);
                EnumSet<PlayerType> set = EnumSet.noneOf(PlayerType.class);
                PlayerType first = null;
                if(query_map.get("players").get(0).equals("both_players") || query_map.get("players").get(0).equals("player_enemy")) {
                    set.add(PlayerType.ENEMY);
                    first = PlayerType.ENEMY;
                }
                if(query_map.get("players").get(0).equals("both_players") || query_map.get("players").get(0).equals("player_me")) {
                    set.add(PlayerType.ME);
                    if(query_map.get("players").get(0).equals("player_me")) {
                        first = PlayerType.ME;
                    }
                }
                proc.reset(set);
                if(first == PlayerType.ME) {
                    proc.my_shoot(shoot);
                }
            } else if(query_map.containsKey("shoot.row")) {
                shoot[0] = Integer.parseInt(query_map.get("shoot.row").get(0));
                shoot[1] = Integer.parseInt(query_map.get("shoot.col").get(0));
                shoot_result = proc.enemy_shoot(shoot);
                types.add(PlayerType.ENEMY);
            } else if(query_map.containsKey("shoot")) {
                proc.my_shoot(null);
                types.add(PlayerType.ME);
            } else if(query_map.containsKey("enemy_answer")) {
                res = proc.enemy_answer(ResultType.valueOf(query_map.get("enemy_answer").get(0).toUpperCase()));
                types.add(PlayerType.ME);
                if(!proc.getSet().contains(PlayerType.ENEMY)/* && !proc.isOver()*/) {
                    proc.my_shoot(null);
                }
            }
        }
        PrintWriter pw = resp.getWriter();
        JSONObject jo = proc.toJSON(types);
        if(query_map.containsKey("shoot.row")) {
            jo.put("shoot_result", shoot_result.toString());
        } else if(false/*proc.isWaitingForAnswer()*/) {
            proc.my_shoot(shoot);
            jo.put("shoot", shoot);
        }
        jo.put("result", res);
        jo.put("query_string", req.getQueryString());
        jo.put("map", String.valueOf(query_map));
        resp.setHeader("content-type", "application/json");
        pw.print(jo);
    }
}
