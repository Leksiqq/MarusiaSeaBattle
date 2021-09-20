package net.leksi.sea_battle;

public class MarusiaPhrases {
    static final String TXT_SO_LONG = "До встречи! ";
    static final String TTS_SO_LONG = "до встречи! -- ";
    static final String TXT_DONT_UNDERSTAND = "Не поняла Вас! ";
    static final String TTS_DONT_UNDERSTAND = "не поняла вас! -- ";
    static final String TXT_GAME_LEFT = "Наша игра не была закончена. ";
    static final String TTS_GAME_LEFT = "наша игра не была закончена. -- ";
    static final String TXT_GAME_TYPE = "Выберите, как будем играть. ";
    static final String TTS_GAME_TYPE = "выберите, как будем играть. -- ";
    static final String TXT_PLACE_SHIPS = "Расставляйте корабли. ";
    static final String TTS_PLACE_SHIPS = "расставляйте корабли. -- ";
    static final String TXT_START_GAME = "Начинайте игру.";
    static final String TTS_START_GAME = "начинайте игру. -- ";
    static final String TXT_PLACE_SHIPS_AND_START_GAME = "Расставляйте корабли и делайте первый ход.";
    static final String TTS_PLACE_SHIPS_AND_START_GAME = "расставляйте корабли -- и делайте первый ход. -- ";
    static final String TXT_MY_MOVE = "Мой ход: ";
    static final String TTS_MY_MOVE = " -- ^мой^ ход. -- ";
    static final String TXT_YOUR_MOVE = "Ваш ход. ";
    static final String TTS_YOUR_MOVE = " -- ваш ход. -- ";
    static final String TXT_YOU_MISSED_MAP = "Вы не попали по карте.";
    static final String TTS_YOU_MISSED_MAP = "вы не попали по ^карте^. ---- ";
    static final String TXT_YOU_WIN = "Поздравляю, Вы победили!";
    static final String TTS_YOU_WIN = " -- ^поздравляю^ -- вы победили! -- ";
    static final String TXT_YOU_LOOSE = "К сожалению Вы проиграли.";
    static final String TTS_YOU_LOOSE = " -- к ^сожалению^ вы проиграли. -- ";
    static final String TXT_DRAW = "Ничья.";
    static final String TTS_DRAW = "-- ничья --";
    static final String TXT_GAME_OVER = "Игра закончена.";
    static final String TTS_GAME_OVER = " -- игра закончена. -- ";
    static final String TXT_MISSED = "Мимо. ";
    static final String TTS_MISSED = " -- мимо. ------ ";
    static final String TXT_INJURED = "Подбит. ";
    static final String TTS_INJURED = " -- подбит. ------ ";
    static final String TXT_KILLED = "Потоплен. ";
    static final String TTS_KILLED = " -- потоплен. ------ ";
    static final String TXT_DO_ANSWER = "Я жду ответ на мой выстрел.";
    static final String TTS_DO_ANSWER = "-- я жду ответ на мой ^выстрел^. -- ";
    static final String TXT_FAILED = "Вы дали ложный ответ или Ваши корабли расставлены не по правилам.";
    static final String TTS_FAILED = " -- вы дали ложный ответ -- или Ваши корабли расставлены не по правилам. -- ";

    static String[] get_phrase(int pos) {
        String[][] all = new String[][]{
                new String[]{TXT_SO_LONG, TTS_SO_LONG},
                new String[]{TXT_DONT_UNDERSTAND, TTS_DONT_UNDERSTAND},
                new String[]{TXT_GAME_LEFT, TTS_GAME_LEFT},
                new String[]{TXT_GAME_TYPE, TTS_GAME_TYPE},
                new String[]{TXT_PLACE_SHIPS, TTS_PLACE_SHIPS},
                new String[]{TXT_START_GAME, TTS_START_GAME},
                new String[]{TXT_PLACE_SHIPS_AND_START_GAME, TTS_PLACE_SHIPS_AND_START_GAME},
                new String[]{TXT_MY_MOVE, TTS_MY_MOVE},
                new String[]{TXT_YOUR_MOVE, TTS_YOUR_MOVE},
                new String[]{TXT_YOU_MISSED_MAP, TTS_YOU_MISSED_MAP},
                new String[]{TXT_YOU_WIN, TTS_YOU_WIN},
                new String[]{TXT_YOU_LOOSE, TTS_YOU_LOOSE},
                new String[]{TXT_DRAW, TTS_DRAW},
                new String[]{TXT_GAME_OVER, TTS_GAME_OVER},
                new String[]{TXT_MISSED, TTS_MISSED},
                new String[]{TXT_INJURED, TTS_INJURED},
                new String[]{TXT_KILLED, TTS_KILLED},
                new String[]{TXT_DO_ANSWER, TTS_DO_ANSWER},
                new String[]{TXT_FAILED, TTS_FAILED},
        };
        return all[pos % all.length];
    }
}
