package net.leksi.sea_battle;

import java.util.Arrays;

public class InputHolder {
    String[] tokens = null;
    String command = null;
    String ou = null;
    String payload = null;

    @Override
    public String toString() {
        return "InputHolder{" +
                "tokens=" + Arrays.toString(tokens) +
                ", command='" + command + '\'' +
                ", ou='" + ou + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
