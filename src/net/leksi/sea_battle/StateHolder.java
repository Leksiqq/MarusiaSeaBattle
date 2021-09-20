package net.leksi.sea_battle;

public class StateHolder {
    private State state = State.WAIT_RESET;
    private ResultType failed = null;
    private StackTraceElement[] stack_trace = null;

    public State state() {
        return state;
    }

    void state(State value) {
        boolean done = false;
        switch (state) {
            case WAIT_RESET:
                if(value == State.WAIT_FIRST_SHOOT || value == State.WAIT_ME_SHOOT || value == State.WAIT_ENEMY_SHOOT) {
                    done = true;
                }
                break;
            case WAIT_FIRST_SHOOT:
                if(value == State.WAIT_ME_SHOOT || value == State.WAIT_ENEMY_SHOOT) {
                    done = true;
                }
                break;
            case WAIT_ME_SHOOT:
                if(value == State.WAIT_ENEMY_ANSWER) {
                    done = true;
                }
                break;
            case WAIT_ENEMY_SHOOT:
                if(value == State.WAIT_ME_SHOOT || value == State.WAIT_ENEMY_SHOOT || value == State.OVER) {
                    done = true;
                }
                break;
            case WAIT_ENEMY_ANSWER:
                if(value == State.WAIT_ME_SHOOT || value == State.WAIT_ENEMY_SHOOT || value == State.OVER) {
                    done = true;
                }
                break;
        }
        if(done) {
            state = value;
        }
    }

    public ResultType failed() {
        return failed;
    }

    public StackTraceElement[] stackTrace() {
        return stack_trace;
    }

    void failed(ResultType value) {
        if(failed == null && value != null) {
            stack_trace = new Error().getStackTrace();
            state = State.FAILED;
            failed = value;
        }
    }

    void reset () {
        state = State.WAIT_FIRST_SHOOT;
        failed = null;
        stack_trace = null;
    }

    public boolean isOutOfGame() {
        return state == State.WAIT_RESET || state == State.OVER  || state == State.FAILED;
    }
}
