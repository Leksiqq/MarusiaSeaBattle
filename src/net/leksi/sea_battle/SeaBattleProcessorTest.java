package net.leksi.sea_battle;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class SeaBattleProcessorTest {

    static class Cons implements Consumer<ResultType> {

        int n_out_of_range = 0;
        int n_repeats = 0;
        int n_missed = 0;
        int n_injured = 0;
        int n_killed = 0;
        int n_other = 0;
        @Override
        public void accept(ResultType resultType) {
            switch (resultType) {
                case KILLED:
                    n_killed++;
                    break;
                case MISSED:
                    n_missed++;
                    break;
                case INJURED:
                    n_injured++;
                    break;
                case OUT_OF_RANGE:
                    n_out_of_range++;
                    break;
                case REPEATED:
                    n_repeats++;
                    break;
                default:
                    n_other++;
                    break;
            }
//            System.out.println(resultType);
        }
    }
    @Test
    void enemy_shoot() {
        SeaBattleProcessor proc = new SeaBattleProcessor();
        Random random = new Random();
        for(int i = 0; i < 1000; i++) {
            proc.reset(EnumSet.of(PlayerType.ENEMY));
            Cons consumer = new Cons();
            ArrayList<Integer> repeats = new ArrayList<>();
            int[] shoot = new int[2];
            for(int j = 0; j < 11; j++) {
                repeats.add(random.nextInt(100));
            }
            for(int j = -1; j < 11; j++) {
                if(j >= 0) {
                    shoot[0] = repeats.remove(0);
                    shoot[1] = shoot[0] % 10;
                    shoot[0] /= 10;
                    ResultType res = proc.enemy_shoot(shoot);
                }
                for(int k = -1; k < 11; k++) {
                    shoot[0] = j;
                    shoot[1] = k;
                    consumer.accept(proc.enemy_shoot(shoot));
                }
            }
//            if(true)break;
            assert proc.getStat(PlayerType.ENEMY).killed == 20 : proc.getStat(PlayerType.ENEMY).killed;
            assert consumer.n_injured == 10 : consumer.n_injured;
            assert consumer.n_killed == 10 : consumer.n_killed;
            assert consumer.n_missed == 80 : consumer.n_missed;
            assert consumer.n_out_of_range == 44 : consumer.n_out_of_range;
            assert consumer.n_other == 0 : consumer.n_other;
        }
    }

    @Test
    void test() {
        SeaBattleProcessor proc1 = new SeaBattleProcessor();
        SeaBattleProcessor proc2 = new SeaBattleProcessor();
        EnumSet<PlayerType> set = EnumSet.of(PlayerType.ME, PlayerType.ENEMY);
        int[] hide = new int[2];
        for(int i = 0; i < 10000; i++) {
//            System.out.println("test: " + (i + 1));
            proc1.reset(set);
            proc2.reset(set);
            int[] shoot = new int[2];
            ResultType res = null;
            try(
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ) {
                pw.println();
                pw.println("1)");
                pw.println(proc1.toString(set, hide));
                pw.println("2)");
                pw.println(proc2.toString(set, hide));
                l:
                while (!proc1.getState().isOutOfGame() && !proc2.getState().isOutOfGame()) {
                    if (
                            !proc2.getState().isOutOfGame() &&
                                    (proc1.getState().state() == State.WAIT_FIRST_SHOOT || proc1.getState().state() == State.WAIT_ME_SHOOT)
                    ) {
                        res = proc1.my_shoot(shoot);
                        pw.println("1) my_shoot: " + shoot[0] + " " + shoot[1] + ", res: " + res + ", state: " + proc1.getState().state() + ", " + proc1.getState().failed());
                        res = proc2.enemy_shoot(shoot);
                        pw.println("2) enemy_shoot: res: " + res + ", state: " + proc2.getState().state() + ", " + proc2.getState().failed());
                        res = proc1.enemy_answer(res);
                        pw.println("1) enemy_answer: " + res + ", state: " + proc1.getState().state() + ", " + proc1.getState().failed());
                        pw.println("1) " + proc1.getStat(PlayerType.ME) + ", " + proc1.getStat(PlayerType.ENEMY));
                        pw.println();
                    }
                    if (!proc1.getState().isOutOfGame() && proc2.getState().state() == State.WAIT_ME_SHOOT) {
                        res = proc2.my_shoot(shoot);
                        pw.println("2) my_shoot: " + shoot[0] + " " + shoot[1] + ", res: " + res + ", state: " + proc2.getState().state() + ", " + proc2.getState().failed());
                        res = proc1.enemy_shoot(shoot);
                        pw.println("1) enemy_shoot: res: " + res + ", state: " + proc1.getState().state() + ", " + proc1.getState().failed());
                        res = proc2.enemy_answer(res);
                        pw.println("2) enemy_answer: " + res + ", state: " + proc2.getState().state() + ", " + proc2.getState().failed());
                        pw.println("2) " + proc2.getStat(PlayerType.ME) + ", " + proc2.getStat(PlayerType.ENEMY));
                        pw.println();
                    }
                }
                pw.println("1) " + proc1.getState().state() + ", " + proc1.getStat(PlayerType.ME) + ", " + proc1.getStat(PlayerType.ENEMY));
                if(proc1.getState().state() == State.FAILED) {
                    pw.println("1) " + proc1.getState().failed());
                    pw.println("1) " + Arrays.stream(proc1.getState().stackTrace()).map(v -> v.getMethodName() + ":" + v.getLineNumber()).
                            collect(Collectors.joining("\n"))
                    );
                }
                pw.println("2) " + proc2.getState().state() + ", " + proc2.getStat(PlayerType.ME) + ", " + proc2.getStat(PlayerType.ENEMY));
                if(proc2.getState().state() == State.FAILED) {
                    pw.println("2) " + proc2.getState().failed());
                    pw.println("2) " + Arrays.stream(proc2.getState().stackTrace()).map(v -> v.getMethodName() + ":" + v.getLineNumber()).
                            collect(Collectors.joining("\n"))
                    );
                }
                sw.close();
//            break;
                assert proc1.getState().state() == State.OVER && proc2.getState().state() == State.OVER : sw.toString();
                assert proc1.getStat(PlayerType.ME).killed == 20 || proc1.getStat(PlayerType.ENEMY).killed == 20 : sw.toString();
                assert proc2.getStat(PlayerType.ME).killed == 20 || proc2.getStat(PlayerType.ENEMY).killed == 20 : sw.toString();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Test
    void toBeautifulString() {
        SeaBattleProcessor proc = new SeaBattleProcessor();
        proc.reset(EnumSet.of(PlayerType.ENEMY));
        System.out.println(proc.toBeautifulString(PlayerType.ME, false));
    }

}