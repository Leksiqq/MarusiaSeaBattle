package net.leksi.sea_battle;

class Cell {
    final int place;
    final int row;
    final int col;
    int ship = -1;
    int position = -1;
    CellState state = CellState.SEA;

    void clear() {
        state = CellState.SEA;
        ship = -1;
        position = -1;
    }

    Cell(final int place) {
        this.place = place;
        this.row = place / SeaBattleProcessor.FIELD_SIZE;
        this.col = place % SeaBattleProcessor.FIELD_SIZE;
    }
}
