package net.leksi.sea_battle;

import java.util.ArrayList;

class Ship {
    final int id;
    int bow = -1;
    int orientation = 0;
    int size;
    ArrayList<Integer> decks = new ArrayList<>();

    Ship(final int id, final int size) {
        this.id = id;
        this.size = size;
    }

    void clear() {
        decks.clear();
        bow = -1;
        orientation = 0;
    }
}
