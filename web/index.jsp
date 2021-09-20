<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Морской бой!</title>
    <link rel="stylesheet" href="<% out.print(request.getContextPath()); %>/3.css"/>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js" type="text/javascript"></script>
    <script src="<% out.print(request.getContextPath()); %>/4.js" type="text/javascript"></script>
</head>
<body>
    <div id="my_field" class="field"></div>
    <div id="enemy_field" class="field"></div>
    <div id="menu">
        <p><button id="new_game">Новая игра</button></p>
        <p><button id="other_field">Другое поле</button></p>
        <p><button id="both_players">Обычная игра</button></p>
        <p><button id="player_me">Только загадываю</button></p>
        <p><button id="player_enemy">Только отгадываю</button></p>
        <p><button id="rules">Правила</button></p>
    </div>
    <div id="spinner"></div>
    <div id="you_win" class="game_over">
        <p>Вы выиграли!</p>
        <button>Хорошо</button>
    </div>
    <div id="you_loose" class="game_over">
        <p>Вы проиграли...</p>
        <button>Понятно</button>
    </div>
    <div id="game_over" class="game_over">
        <p>Игра окончена.</p>
        <button>Понятно</button>
    </div>
    <div id="game_over_wrong_layout">
        <p>Ваш флот выстроен с нарушением <a href="">правил</a>!</p>
        <p class="you_loose">Вы проиграли.</p>
        <p class="game_failed">Игра окончена.</p>
        <button>Понятно</button>
    </div>
    <div id="game_over_wrong_answer">
        <p>Вы должны были выбрать "Потоплен" вместо "Повреждён".</p>
        <p>Это нарушение <a href="">правил</a>!</p>
        <p class="you_loose">Вы проиграли.</p>
        <p class="game_failed">Игра окончена.</p>
        <button>Понятно</button>
    </div>
    <div id="player_enemy_start" class="game_start">
        <p>Делайте первый ход.</p>
        <button>Хорошо</button>
    </div>
    <div id="player_me_start" class="game_start">
        <p>Заполните своё игровое поле.</p>
        <button>Готово</button>
    </div>
    <div id="both_players_start" class="game_start">
        <p>Заполните своё игровое поле и делайте первый ход.</p>
        <button>Хорошо</button>
    </div>
    <div id="enemy_answer">
        <p id="my_shoot"></p>
        <p><button id="missed">Мимо</button></p>
        <p><button id="injured">Повреждён</button></p>
        <p><button id="killed">Потоплен</button></p>
    </div>
</body>
</html>