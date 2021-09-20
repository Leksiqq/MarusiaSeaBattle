package net.leksi.sea_battle;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class PlayerHolder {
    Cell[] cells = new Cell[SeaBattleProcessor.FIELD_SQUARE];
    Ship[] ships = new Ship[SeaBattleProcessor.FIELD_SIZE + 1];
    TreeSet<Integer> available_cells = new TreeSet<>();

    void clear() {
        Arrays.stream(ships).forEach(Ship::clear);
        ships[SeaBattleProcessor.FIELD_SIZE].size = 0;
        Arrays.stream(cells).forEach(Cell::clear);
        available_cells.addAll(IntStream.range(0, SeaBattleProcessor.FIELD_SQUARE).boxed().collect(Collectors.toList()));
        stat.clear();
    }

    Stat stat = new Stat();
}
