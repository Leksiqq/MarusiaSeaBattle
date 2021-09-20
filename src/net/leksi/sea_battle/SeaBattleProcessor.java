package net.leksi.sea_battle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SeaBattleProcessor {

    static final int FIELD_SIZE = 10;
    static final int FIELD_SQUARE = FIELD_SIZE * FIELD_SIZE;
    static final String LETTERS = "АБВГДЕЖЗИК";

    PlayerHolder my_holder = new PlayerHolder();
    PlayerHolder enemy_holder = new PlayerHolder();

    EnumSet<PlayerType> set = null;

    final private HashMap<PlayerType, PlayerHolder> players = new HashMap<>() {{
        put(PlayerType.ME, my_holder);
        put(PlayerType.ENEMY, enemy_holder);
    }};

    private final Random random = new Random();

    private final TreeSet<Integer> queue = new TreeSet<>();
    private int my_shoot = -1;
    private final StateHolder state = new StateHolder();

    public ResultType my_shoot(int[] shoot) {
        if(state.isOutOfGame()) {
            return ResultType.OUT_OF_GAME;
        }
        if(state.state() != State.WAIT_ME_SHOOT && state.state() != State.WAIT_FIRST_SHOOT) {
            return ResultType.OUT_OF_ORDER;
        }
        state.state(State.WAIT_ME_SHOOT);
        my_shoot = -1;
        while (!queue.isEmpty()) {
            int it = queue.stream().collect(Collectors.toList()).get(random.nextInt(queue.size()));
            queue.remove(it);
            enemy_holder.available_cells.remove(it);
            if(enemy_holder.cells[it].state == CellState.SEA) {
                my_shoot = it;
                break;
            }
        }
        if(my_shoot == -1) {
            while (!enemy_holder.available_cells.isEmpty()) {
                int it = enemy_holder.available_cells.stream().collect(Collectors.toList()).get(random.nextInt(enemy_holder.available_cells.size()));
                enemy_holder.available_cells.remove(it);
                if (enemy_holder.cells[it].state == CellState.SEA) {
                    my_shoot = it;
                    break;
                }
            }
        }
        if(my_shoot == -1) {
            state.failed(ResultType.WRONG_LAYOUT);
            return state.failed();
        }
        if(shoot != null) {
            shoot[0] = my_shoot / FIELD_SIZE;
            shoot[1] = my_shoot % FIELD_SIZE;
        }
        state.state(State.WAIT_ENEMY_ANSWER);
        return ResultType.OK;
    }

    public ResultType enemy_answer(ResultType answer) {
        if(state.isOutOfGame()) {
            return ResultType.OUT_OF_GAME;
        }
        if(state.state() != State.WAIT_ENEMY_ANSWER) {
            return ResultType.OUT_OF_ORDER;
        }
        if(answer == ResultType.MISSED || answer == ResultType.INJURED || answer == ResultType.KILLED) {
            my_holder.stat.moves++;

            if(answer == ResultType.MISSED) {
                if(enemy_holder.ships[FIELD_SIZE].size > 0 && queue.isEmpty()) {
                    state.failed(ResultType.WRONG_ANSWER);
                    return state.failed();
                }
                enemy_holder.cells[my_shoot].state = CellState.MISSED;
            } else {
                Ship sh = null;
                int r = my_shoot / FIELD_SIZE;
                int c = my_shoot % FIELD_SIZE;
                for(int i = -1; i <= 1; i++) {
                    if(r + i >= 0 && r + i < FIELD_SIZE) {
                        for (int j = -1; j <= 1; j++) {
                            if(c + j >= 0 && c + j < FIELD_SIZE) {
                                int ptr = my_shoot + i * FIELD_SIZE + j;
                                if (ptr == my_shoot) {
                                    enemy_holder.cells[ptr].state = CellState.INJURED;
                                } else if (i == 0 || j == 0) {
                                    if (enemy_holder.cells[ptr].state == CellState.SEA) {
                                        queue.add(ptr);
                                    } else if (enemy_holder.cells[ptr].state == CellState.INJURED) {
                                        enemy_holder.cells[my_shoot].ship = FIELD_SIZE;
                                        sh = enemy_holder.ships[FIELD_SIZE];
                                        sh.size++;
                                        if (i == 0) {
                                            if (sh.orientation == 0) {
                                                sh.orientation = 1;
                                            } else if (sh.orientation == FIELD_SIZE) {
                                                System.out.println(new Error().getStackTrace()[0]);
                                                state.failed(ResultType.WRONG_LAYOUT);
                                                return state.failed();
                                            }
                                        } else {
                                            if (sh.orientation == 0) {
                                                sh.orientation = FIELD_SIZE;
                                            } else if (sh.orientation == 1) {
                                                state.failed(ResultType.WRONG_LAYOUT);
                                                return state.failed();
                                            }
                                        }
                                        if (my_shoot < sh.bow) {
                                            sh.bow = my_shoot;
                                        }
                                    }
                                } else {
                                    if (enemy_holder.cells[ptr].state == CellState.SEA) {
                                        enemy_holder.cells[ptr].state = CellState.CLOSED;
                                        queue.remove(ptr);
                                        enemy_holder.available_cells.remove(ptr);
                                    }
                                }
                            }
                        }
                    }
                }
                if(sh == null) {
                    enemy_holder.cells[my_shoot].ship = FIELD_SIZE;
                    sh = enemy_holder.ships[FIELD_SIZE];
                    sh.size = 1;
                    sh.bow = my_shoot;
                }
                int size = sh.size;
                List<Ship> available_ships = Arrays.stream(enemy_holder.ships).
                        filter(v -> v.id < FIELD_SIZE && (answer == ResultType.INJURED && v.size > size ||
                                answer == ResultType.KILLED && v.size == size
                                )).sorted(Comparator.comparingInt(x -> x.size)).collect(Collectors.toList());
                if(available_ships.isEmpty()) {
                    state.failed(ResultType.WRONG_LAYOUT);
                    return state.failed();
                }
                if(answer == ResultType.KILLED) {
                    Ship sh1 = available_ships.get(0);
                    sh1.bow = sh.bow;
                    sh1.size = sh.size;
                    sh1.orientation = sh.orientation;
                    sh.size = 0;
                    sh.orientation = 0;
                    sh.bow = -1;
                    for(int i = 0; i < sh1.size; i++) {
                        Cell cell = enemy_holder.cells[sh1.bow + i * sh1.orientation];
                        cell.ship = sh1.id;
                        cell.state = CellState.KILLED;
                        my_holder.stat.killed++;
                    }
                    if(my_holder.stat.killed == FIELD_SIZE * 2) {
                        state.state(State.OVER);
                    }
                    for(int ptr: queue.stream().collect(Collectors.toList())) {
                        enemy_holder.cells[ptr].state = CellState.CLOSED;
                        queue.remove(ptr);
                        enemy_holder.available_cells.remove(ptr);
                    }
                } else {
                    if(queue.isEmpty()) {
                        state.failed(ResultType.WRONG_ANSWER);
                        return state.failed();
                    }
                }
            }

            if(set.contains(PlayerType.ENEMY) && answer == ResultType.MISSED) {
                state.state(State.WAIT_ENEMY_SHOOT);
            } else {
                state.state(State.WAIT_ME_SHOOT);
            }
            my_shoot = -1;
            return ResultType.OK;
        }
        return answer;
    }

    private ResultType me_answer = null;
    private int[] enemy_shoot = new int[2];

    public SeaBattleProcessor() {
        ArrayList<Integer> types = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        for(int i = 0; i < FIELD_SIZE; i++) {
            players.get(PlayerType.ME).ships[i] = new Ship(i, types.size());
            players.get(PlayerType.ENEMY).ships[i] = new Ship(i, types.size());
            int cn = types.get(0) - 1;
            if(cn > 0) {
                types.set(0, cn);
            } else {
                types.remove(0);
            }
        }
        players.get(PlayerType.ME).ships[FIELD_SIZE] = new Ship(FIELD_SIZE, 0);
        players.get(PlayerType.ENEMY).ships[FIELD_SIZE] = new Ship(FIELD_SIZE, 0);
        for(int i = 0; i < FIELD_SQUARE; i++) {
            players.get(PlayerType.ME).cells[i] = new Cell(i);
            players.get(PlayerType.ENEMY).cells[i] = new Cell(i);
        }
        long time = new Date().getTime();
        long seed = Long.valueOf(Long.toString(time).chars().distinct().mapToObj(v -> String.valueOf((char) v)).collect(Collectors.joining())) + hashCode();
        random.setSeed(seed);
    }

    private void clear(PlayerType whos) {
        players.get(whos).clear();
        if(whos == PlayerType.ME) {
            queue.clear();
        }
    }

    public Stat getStat(PlayerType whos) {
        return players.get(whos).stat;
    }

    public String toBeautifulString(PlayerType whos, boolean hide) {
        StringBuilder sb = new StringBuilder();
        sb.append("   ");
        for(int i = 0; i < FIELD_SIZE; i++) {
            sb.append("  ").append(LETTERS.charAt(i)).append(" ");
        }
        sb.append("\n   ");
        for(int i = 0; i < FIELD_SIZE; i++) {
            if(i == 0) {
                sb.append("┌───");
            } else if(i == FIELD_SIZE - 1) {
                sb.append("┬───┐");
            } else {
                sb.append("┬───");
            }
        }
        sb.append("\n");
        for(int r = 1; r <= FIELD_SIZE; r++) {
            sb.append(String.format("%2s ", Integer.toString(r)));
            PlayerHolder ph = players.get(whos);
            for (int c = 0; c < FIELD_SIZE; c++) {
                sb.append("│ ");
                Cell cell = ph.cells[(r - 1) * FIELD_SIZE + c];
                switch (cell.state) {
                    case SEA:
                        sb.append(' ');
                        break;
                    case MISSED:
                        sb.append('•');
                        break;
                    case CLOSED:
                        if (hide) {
                            sb.append(' ');
                        } else {
                            sb.append('-');
                        }
                        break;
                    case SHIP:
                        if (hide) {
                            sb.append(0);
                        } else {
                            if (cell.ship >= 0 && cell.ship < FIELD_SIZE) {
                                sb.append(ph.ships[cell.ship].size);
                            } else {
                                sb.append('S');
                            }
                        }
                        break;
                    case KILLED:
                        sb.append('X');
                        break;
                    case INJURED:
                        sb.append('/');
                        break;
                }
                sb.append(" ");
            }
            sb.append("│\n");
            sb.append("   ");
            for(int i = 0; i < FIELD_SIZE; i++) {
                if(r < FIELD_SIZE) {
                    if (i == 0) {
                        sb.append("├───");
                    } else if (i == FIELD_SIZE - 1) {
                        sb.append("┼───┤");
                    } else {
                        sb.append("┼───");
                    }
                } else {
                    if (i == 0) {
                        sb.append("└───");
                    } else if (i == FIELD_SIZE - 1) {
                        sb.append("┴───┘");
                    } else {
                        sb.append("┴───");
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    String toString(EnumSet<PlayerType> whos, int[] hide) {
        final int SPACE = 5;
        StringBuilder sb = new StringBuilder();
        List<PlayerHolder> phs = whos.stream().map(v -> players.get(v)).collect(Collectors.toList());
        sb.append("   ");
        for(PlayerType pt: whos) {
            sb.append(String.format("%-" + (FIELD_SIZE + SPACE) + "s", pt.toString()));
        }
        sb.append("\n");
        int i = -1;
        sb.append("   ");
        for(PlayerType pt: whos) {
            i++;
            if(i == 1) {
                sb.append(String.format("%" + SPACE + "s", ""));
            }
            for (int c = 0; c < FIELD_SIZE; c++) {
                sb.append(c);
            }
        }
        sb.append("\n");
        i = -1;
        sb.append("   ");
        for(PlayerType pt: whos) {
            i++;
            if(i == 1) {
                sb.append(String.format("%" + SPACE + "s", ""));
            }
            sb.append(String.format("%" + FIELD_SIZE + "s", "").replace(' ', '-'));
        }
        sb.append("\n");
        for(int r = 0; r < FIELD_SIZE; r++) {
            sb.append(String.format("%-3s", r + ": "));
            i = -1;
            for(PlayerType pt: whos) {
                i++;
                if(i == 1) {
                    sb.append(String.format("%" + SPACE + "s", ""));
                }
                PlayerHolder ph = players.get(pt);
                for (int c = 0; c < FIELD_SIZE; c++) {
                    Cell cell = ph.cells[r * FIELD_SIZE + c];
                    switch (cell.state) {
                        case SEA:
                            sb.append(0);
                            break;
                        case MISSED:
                            sb.append('.');
                            break;
                        case CLOSED:
                            if (hide[i] != 0) {
                                sb.append(0);
                            } else {
                                sb.append('-');
                            }
                            break;
                        case SHIP:
                            if (hide[i] != 0) {
                                sb.append(0);
                            } else {
                                if (cell.ship >= 0 && cell.ship < FIELD_SIZE) {
                                    sb.append(ph.ships[cell.ship].size);
                                } else {
                                    sb.append('S');
                                }
                            }
                            break;
                        case KILLED:
                            sb.append('X');
                            break;
                        case INJURED:
                            sb.append('/');
                            break;
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void reset(EnumSet<PlayerType> set) {
        me_answer = null;
        this.set = EnumSet.copyOf(set);
        state.reset();
        clear(PlayerType.ENEMY);
        clear(PlayerType.ME);
        if(set.contains(PlayerType.ENEMY)) {
            place_ships();
        }
//        if(set.size() == 1) {
//            switch(set.stream().findAny().get()) {
//                case ME:
//                    state.state(State.WAIT_ME_SHOOT);
//                    break;
//                default:
//                    state.state(State.WAIT_ENEMY_SHOOT);
//                    break;
//            }
//        }
    }

    public StateHolder getState() {
        return state;
    }

    public ResultType enemy_shoot(int[] shoot) {
        if(state.isOutOfGame()) {
            return ResultType.OUT_OF_GAME;
        }
        if(state.state() != State.WAIT_FIRST_SHOOT && state.state() != State.WAIT_ENEMY_SHOOT) {
            return ResultType.OUT_OF_ORDER;
        }
        if(shoot[0] < 0 || shoot[0] >= FIELD_SIZE || shoot[1] < 0 || shoot[1] >= FIELD_SIZE) {
            return ResultType.OUT_OF_RANGE;
        }
        state.state(State.WAIT_ENEMY_SHOOT);
        ResultType res = ResultType.MISSED;
        Cell target = my_holder.cells[shoot[0] * FIELD_SIZE + shoot[1]];
        if(target.state != CellState.SEA && target.state != CellState.CLOSED) {
            if(target.state != CellState.SHIP) {
                switch (target.state) {
                    case MISSED:
                        res = ResultType.MISSED;
                        break;
                    case INJURED:
                        res = ResultType.INJURED;
                        break;
                    case KILLED:
                        res = ResultType.KILLED;
                        break;
                }
            } else {
                target.state = CellState.INJURED;
                if(IntStream.range(0, my_holder.ships[target.ship].size).allMatch(v ->
                        my_holder.cells[my_holder.ships[target.ship].bow + v * my_holder.ships[target.ship].orientation].
                                state == CellState.INJURED
                )) {
                    IntStream.range(0, my_holder.ships[target.ship].size).forEach(v -> {
                        my_holder.cells[my_holder.ships[target.ship].bow + v * my_holder.ships[target.ship].orientation].
                                state = CellState.KILLED;
                        enemy_holder.stat.killed++;
                    });
                    if(enemy_holder.stat.killed == FIELD_SIZE * 2) {
                        state.state(State.OVER);
                    }
                    res = ResultType.KILLED;
                } else {
                    res = ResultType.INJURED;
                }
            }
        } else {
            target.state = CellState.MISSED;
        }
        enemy_holder.stat.moves++;
        if(set.contains(PlayerType.ME) && res == ResultType.MISSED) {
            state.state(State.WAIT_ME_SHOOT);
        } else {
            state.state(State.WAIT_ENEMY_SHOOT);
        }
        enemy_shoot[0] = shoot[0];
        enemy_shoot[1] = shoot[1];
        me_answer = res;
        return res;
    }

    private void place_ships() {
        clear(PlayerType.ME);
        ArrayList<Integer> used = new ArrayList<>();
        l0: while(true) {
            l1: for (Ship sh : my_holder.ships) {
                l2: while(true) {
                    if(my_holder.available_cells.isEmpty()) {
                        clear(PlayerType.ME);
                        continue l0;
                    }
                    sh.orientation = 1 + random.nextInt(2) * (FIELD_SIZE - 1);
                    used.add(my_holder.available_cells.stream().collect(Collectors.toList()).get(random.nextInt(my_holder.available_cells.size())));
                    my_holder.available_cells.remove(used.get(used.size() - 1));
                    sh.bow = used.get(used.size() - 1);
                    int r = sh.bow / FIELD_SIZE;
                    int c = sh.bow % FIELD_SIZE;
                    if(sh.orientation == 1 && c + sh.size <= FIELD_SIZE || sh.orientation == 10 && r + sh.size <= FIELD_SIZE) {
                        for(int i = 0; i < sh.size; i++) {
                            int ptr = sh.bow + i * sh.orientation;
                            if(my_holder.cells[ptr].state != CellState.SEA) {
                                continue l2;
                            }
                            if(i == 0) {
                                if(sh.orientation == 1 || sh.size == 1) {
                                    if (my_holder.cells[ptr].col > 0) {
                                        if (my_holder.cells[ptr - 1].state != CellState.SEA) {
                                            continue l2;
                                        }
                                    }
                                }
                                if(sh.orientation == FIELD_SIZE || sh.size == 1) {
                                    if (my_holder.cells[ptr].row > 0) {
                                        if (my_holder.cells[ptr - FIELD_SIZE].state != CellState.SEA) {
                                            continue l2;
                                        }
                                    }
                                }
                            } else if(i == sh.size - 1) {
                                if(sh.orientation == 1) {
                                    if (my_holder.cells[ptr].col < FIELD_SIZE - 1) {
                                        if (my_holder.cells[ptr + 1].state != CellState.SEA) {
                                            continue l2;
                                        }
                                    }
                                } else {
                                    if (my_holder.cells[ptr].row < FIELD_SIZE - 1) {
                                        if (my_holder.cells[ptr + FIELD_SIZE].state != CellState.SEA) {
                                            continue l2;
                                        }
                                    }
                                }
                            }
                            for(int j = -1; j <= 1; j += 2) {
                                int r1 = my_holder.cells[ptr].row + j;
                                if(r1 >= 0 && r1 < FIELD_SIZE) {
                                    for(int k = -1; k <= 1; k += 2) {
                                        int c1 = my_holder.cells[ptr].col + k;
                                        if(c1 >= 0 && c1 < FIELD_SIZE) {
                                            if(my_holder.cells[r1 * FIELD_SIZE + c1].state != CellState.SEA) {
                                                continue l2;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        for(int i = 0; i < sh.size; i++) {
                            int ptr = sh.bow + i * sh.orientation;
                            my_holder.cells[ptr].ship = sh.id;
                            my_holder.cells[ptr].position = i;
                            my_holder.cells[ptr].state = CellState.SHIP;
                            my_holder.available_cells.remove(ptr);
                            if(i == 0) {
                                if(sh.orientation == 1 || sh.size == 1) {
                                    if (my_holder.cells[ptr].col > 0) {
                                        my_holder.cells[ptr - 1].state = CellState.CLOSED;
                                        my_holder.available_cells.remove(ptr - 1);
                                    }
                                }
                                if(sh.orientation == FIELD_SIZE || sh.size == 1) {
                                    if (my_holder.cells[ptr].row > 0) {
                                        my_holder.cells[ptr - FIELD_SIZE].state = CellState.CLOSED;
                                        my_holder.available_cells.remove(ptr - FIELD_SIZE);
                                    }
                                }
                            }
                            if(i == sh.size - 1) {
                                if (sh.orientation == 1 || sh.size == 1) {
                                    if (my_holder.cells[ptr].col < FIELD_SIZE - 1) {
                                        my_holder.cells[ptr + 1].state = CellState.CLOSED;
                                        my_holder.available_cells.remove(ptr + 1);
                                    }
                                }
                                if(sh.orientation == FIELD_SIZE || sh.size == 1) {
                                    if (my_holder.cells[ptr].row < FIELD_SIZE - 1) {
                                        my_holder.cells[ptr + FIELD_SIZE].state = CellState.CLOSED;
                                        my_holder.available_cells.remove(ptr + FIELD_SIZE);
                                    }
                                }
                            }
                            for(int j = -1; j <= 1; j += 2) {
                                int r1 = my_holder.cells[ptr].row + j;
                                if(r1 >= 0 && r1 < FIELD_SIZE) {
                                    for(int k = -1; k <= 1; k += 2) {
                                        int c1 = my_holder.cells[ptr].col + k;
                                        if(c1 >= 0 && c1 < FIELD_SIZE) {
                                            my_holder.cells[r1 * FIELD_SIZE + c1].state = CellState.CLOSED;
                                            my_holder.available_cells.remove(r1 * FIELD_SIZE + c1);
                                        }
                                    }
                                }
                            }
                        }
                        used.forEach(v -> {
                            if(my_holder.cells[v].state == CellState.SEA) {
                                my_holder.available_cells.add(v);
                            }
                        });
                        used.clear();
                        break;
                    }
                }

            }
            break;
        }
        assert my_holder.available_cells.stream().allMatch(v ->my_holder.cells[v].state == CellState.SEA);
        assert Arrays.stream(my_holder.ships).allMatch(v -> v.bow != -1);
        assert Arrays.stream(my_holder.cells).filter(v -> v.ship != -1).count() == FIELD_SIZE * 2 : toString(EnumSet.of(PlayerType.ME), new int[2]);
        assert Arrays.stream(my_holder.cells).filter(v -> v.ship != -1).allMatch(v -> {
            boolean res = true;
            l: for(int j = -1; j <= 1; j++) {
                int r1 = v.row + j;
                if(r1 >= 0 && r1 < FIELD_SIZE) {
                    for(int k = -1; k <= 1; k++) {
                        if(k != 0 && j != 0) {
                            int c1 = v.col + k;
                            if (c1 >= 0 && c1 < FIELD_SIZE) {
                                if(my_holder.cells[r1 * FIELD_SIZE + c1].state == CellState.SHIP && my_holder.cells[r1 * FIELD_SIZE + c1].ship != v.ship) {
                                    res = false;
                                    break l;
                                }
                            }
                        }
                    }
                }
            }
            if(!res) {
                System.out.println(toString(EnumSet.of(PlayerType.ME), new int[2]));
            }
            return res;
        });
    }

    public EnumSet<PlayerType> getSet() {
        return EnumSet.copyOf(set);
    }

    public ResultType remind_me_answer(int[] shoot) {
        if(state.isOutOfGame()) {
            return ResultType.OUT_OF_GAME;
        }
        shoot[0] = enemy_shoot[0];
        shoot[1] = enemy_shoot[1];
        return me_answer;
    }

    public ResultType remind_me_last_shoot(int[] shoot) {
        if(state.isOutOfGame()) {
            return ResultType.OUT_OF_GAME;
        }
        if(my_shoot == -1) {
            shoot[0] = -1;
            shoot[1] = -1;
            return ResultType.OUT_OF_ORDER;
        }
        shoot[0] = my_shoot / FIELD_SIZE;
        shoot[1] = my_shoot % FIELD_SIZE;
        return ResultType.OK;
    }

    public JSONObject toJSON(EnumSet<PlayerType> types) {
        return new JSONObject(){{
            put("state", state.state().toString());
            if(!state.isOutOfGame()) {
                set.forEach(v -> append("players", v.toString()));
                if(types != null && types.contains(PlayerType.ME) || types == null && set.contains(PlayerType.ME)) {
                    put("enemy_field", field_to_JSON(enemy_holder.cells, false));
                }
                if(types != null && types.contains(PlayerType.ENEMY) || types == null && set.contains(PlayerType.ENEMY)) {
                    put("my_field", field_to_JSON(my_holder.cells, true));
                }
                put("score", new JSONObject(){{
                    if(set.contains(PlayerType.ME)) {
                        put("my_killed", my_holder.stat.killed);
                        put("my_moves", my_holder.stat.moves);
                    }
                    if(set.contains(PlayerType.ENEMY)) {
                        put("enemy_killed", enemy_holder.stat.killed);
                        put("enemy_moves", enemy_holder.stat.moves);
                    }
                }});
            }
        }};
    }

    JSONArray field_to_JSON(PlayerType player, boolean hide) {
        if(player == PlayerType.ENEMY) {
            return field_to_JSON(my_holder.cells, hide);
        } else if(player == PlayerType.ME) {
            return field_to_JSON(enemy_holder.cells, hide);
        }
        return new JSONArray();
    }

    private JSONArray field_to_JSON(Cell[] field, boolean hide) {
        return new JSONArray(){
            {
                for (int i = 0; i < FIELD_SIZE; i++) {
                    int i1 = i;
                    put(new JSONArray() {
                        {
                            for (int j = 0; j < FIELD_SIZE; j++) {
                                int ptr = i1 * FIELD_SIZE + j;
                                if (hide) {
                                    switch (field[ptr].state) {
                                        case MISSED:
                                        case KILLED:
                                        case INJURED:
                                            put(field[ptr].state.toString());
                                            break;
                                        default:
                                            put(CellState.SEA.toString());
                                    }
                                } else {
                                    put(field[ptr].state.toString());
                                }
                            }
                        }});
                }
            }};
    }
}
