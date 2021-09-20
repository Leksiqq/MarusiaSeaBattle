package net.leksi.sea_battle;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class MarusiaTestServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SeaBattleProcessor proc = new SeaBattleProcessor();
        proc.reset(EnumSet.of(PlayerType.ENEMY));

        String ct = req.getHeader("content-type");
        String[] parts = ct.split(";");
        String charset = "utf-8";
        for(int i = 1; i < parts.length; i++) {
            String[] name_value = parts[i].split("=");
            String value = "";
            if(name_value.length == 2) {
                value = name_value[1];
            }
            if(name_value[0].equalsIgnoreCase("charset") ){
                charset = value;
            }
        }

        String content = null;
        try (
                InputStreamReader isr = new InputStreamReader(req.getInputStream(), charset.toUpperCase());
                BufferedReader br = new BufferedReader(isr);
        ) {
            content = br.lines().collect(Collectors.joining("\n"));
        }

        JSONObject jo = new JSONObject(content);
        JSONObject session = (JSONObject)jo.get("session");
        JSONObject request = (JSONObject)jo.get("request");

        JSONObject jo1 = new JSONObject();
        JSONObject response = new JSONObject();
        jo1.put("response", response);
        response.put("text", proc.toBeautifulString(PlayerType.ME, false));
        response.put("end_session", false);
        jo1.put("session", new JSONObject(session, new String[]{"session_id", "user_id", "message_id"}));
        jo1.put("version", jo.get("version"));
        String tmp = jo1.toString();
        resp.setStatus(200);
        resp.getWriter().println(tmp);
    }
}
