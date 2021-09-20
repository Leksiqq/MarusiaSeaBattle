package net.leksi.sea_battle;

public class Stat {
    int moves = 0;
    int killed = 0;

    void clear() {
        moves = 0;
        killed = 0;
    }

    public int moves() {
        return moves;
    }

    public int killed() {
        return killed;
    }

    public String toString() {
        return String.format("moves: %d, killed: %d", moves, killed);
    }
}
