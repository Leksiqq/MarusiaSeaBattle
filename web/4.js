(() => {

const font_size_factor = 0.4;
const button_font_size_factor = 0.5;
const main = (window.location.pathname.endsWith('/') ? '' : '/') + 'main';

let state = {};
let changes = [];

let orientation = '';
    
$(window).ready(() => {
    $('#enemy_field').hide();
    $(window).click(click_manager);
    render_field('#my_field');
    render_field('#enemy_field');
    resize();
    get_state().then(apply_changes);
});

function click_manager(evt) {
    console.log(evt);
    if(state.modal !== undefined) {
        if($(evt.target).is(state.modal + ' button')) {
            close_message_box();
            delete state.modal;
            if($(evt.target).is('#game_over button')) {
                if(state.gate_over_shown === undefined) {
                    state.gate_over_shown = true;
                    open_menu();
                }
            } else if($(evt.target).parents('.game_start').length) {
                let id = $(evt.target).parents('.game_start').attr('id');
                let players = id.substring(0, id.length - '_start'.length);
                get_state('players=' + players).then(apply_changes);
            }
            evt.stopPropagation();
            return;
        }
    }
    if(state.modal == undefined) {
        if($(evt.target).is('#menu')) {
            evt.stopPropagation();
        } else {
            if(!$(evt.target).parents('#menu').length && $('#menu').is(':visible')) {
                console.log('here');
                close_menu();
                evt.stopPropagation();
                return;
            }
            if($(evt.target).is('#menu button:not(#rules, #other_field, #new_game)')) {
                close_menu();
                if(state.players !== undefined) {
                    state = {};
                    render_field('#my_field');
                    render_field('#enemy_field');
                    resize();
                }
                open_message_box('#' + evt.target.id + '_start');
            } else if($(evt.target).is('button#rules')) {
                alert('TODO');
            } else if($(evt.target).is('.menu_link')) {
                if(!$('#menu').is(':visible')) {
                    if(state.game !== 'NEW') {
                        open_menu();
                        evt.stopPropagation();
                        return;
                    }
                }
            } else if($(evt.target).is('#my_field .cell.sea') && state.next !== undefined && state.next === 'ENEMY') {
                if(state.game === 'OVER') {
                    open_message_box('#game_over');
                } else if(state.game === 'WRONG_LAYOUT') {
                    open_message_box('#game_over_wrong_layout', message_select.bind('game_over_wrong_layout'));
                } else if(state.game === 'WRONG_ANSWER') {
                    open_message_box('#game_over_wrong_answer', message_select.bind('game_over_wrong_answer'));
                } else {
                    $('#spinner').css({left: $(evt.target).offset().left + 'px', top: $(evt.target).offset().top + 'px'});
                    $('#spinner').show();
                    get_state('shoot.row=' + $(evt.target).data('row') + '&' + 'shoot.col=' + $(evt.target).data('col')).then(apply_changes);
                }
            } else if($(evt.target).is('#new_game')) {
                $('#spinner').hide();
                state = {new: true};
                render_field('#my_field');
                render_field('#enemy_field');
                resize();
                close_menu();
                open_menu();
                evt.stopPropagation();
                return;
            } else if($(evt.target).is('#other_field')) {
                if($('#enemy_field').is(':visible')) {
                    $('#spinner').hide();
                    $('#enemy_answer').hide();
                    $('#enemy_field').hide();
                    $('#my_field').show();
                } else {
                    $('#my_field').hide();
                    $('#enemy_field').show();
                    apply_changes();
                }
                resize();
            } else if($(evt.target).is('#enemy_answer button')) {
                $('#enemy_answer').hide();
                get_state('enemy_answer=' + evt.target.id).then(apply_changes);
            }
            if(state.game !== 'NEW') {
                close_menu();
            }
        }
    }
}

function open_message_box(id, extra_action) {
    if(extra_action !== undefined) {
        extra_action();
    }
    $('.field').addClass('blur');
    $('.field').prop('disabled',true);
    $(id).show();
    state.modal = id;
}

function close_message_box() {
    $(state.modal).hide();
    $('.field').removeClass('blur');
    $('.field').prop('disabled',false);
}

function message_select() {
    if(state.players.length == 2) {
        $('#' + this +' .game_failed').hide();
        $('#' + this +' .you_loose').show();
    } else {
        $('#' + this +' .you_loose').hide();
        $('#' + this +' .game_failed').show();
    }
}

function apply_changes() {
    console.log(changes);
    $('#spinner').hide();
    if(state.game === 'NEW') {
        open_menu();
    } else {
        for(let r = 0; r < 10; r++) {
            for(let c = 0; c < 10; c++) {
                (state.players !== undefined ? state.players : ['ENEMY']).forEach(v => {
                    let field = v === 'ME' ? 'enemy_field' : 'my_field';
                    if(changes.includes(field)) {
                        let cell = $($($('#' + field).find('.row')[r]).find('.cell')[c]);
                        cell.removeClass('sea');
                        cell.removeClass('missed');
                        cell.removeClass('injured');
                        cell.removeClass('killed');
                        cell.addClass(state[field][r][c].toLowerCase());
                        if(state[field][r][c] == 'MISSED') {
                            cell.html('●');
                        } else if(state[field][r][c] == 'INJURED') {
                            cell.html('✖');
                        } else if(state[field][r][c] == 'KILLED') {
                            cell.css({backgroundColor: 'lightgray'})
                            cell.html('❌');
                        }
                        if(r > 0) {
                            if(
                                state[field][r][c] == 'KILLED' && state[field][r - 1][c] != 'KILLED' ||
                                state[field][r][c] != 'KILLED' && state[field][r - 1][c] == 'KILLED'
                            ) {
                                $($($('#' + field).find('.row')[r - 1]).find('.cell')[c]).css({borderBottom: 'solid 3px red'});
                            }
                            if(r == 9) {
                                if(
                                    state[field][r][c] == 'KILLED'
                                ) {
                                    cell.css({borderBottom: 'solid 3px red'});
                                }
                            }
                        } else {
                            if(
                                state[field][r][c] == 'KILLED'
                            ) {
                                cell.css({borderTop: 'solid 3px red'});
                            }
                        }
                        if(c > 0) {
                            if(
                                state[field][r][c] == 'KILLED' && state[field][r][c - 1] != 'KILLED' ||
                                state[field][r][c] != 'KILLED' && state[field][r][c - 1] == 'KILLED'
                            ) {
                                $($($('#' + field).find('.row')[r]).find('.cell')[c - 1]).css({borderRight: 'solid 3px red'});
                            }
                            if(c == 9) {
                                if(
                                    state[field][r][c] == 'KILLED'
                                ) {
                                    cell.css({borderRight: 'solid 3px red'});
                                }
                            }
                        } else {
                            if(
                                state[field][r][c] == 'KILLED'
                            ) {
                                cell.css({borderLeft: 'solid 3px red'});
                            }
                        }
                    }
                });
            }
        }
    }
    if(state.game === 'OVER') {
        open_message_box('#game_over');
    } else if(state.result === 'WRONG_ANSWER') {
        open_message_box('#game_over_wrong_answer', message_select.bind('game_over_wrong_answer'));
    } else if(state.game === 'WRONG_LAYOUT') {
        open_message_box('#game_over_wrong_layout', message_select.bind('game_over_wrong_layout'));
    } else {
        show_next_field();
    }
}

function show_next_field() {
    if(state.next === 'ENEMY') {
        if($('#enemy_field').is(':visible')) {
            $('#enemy_field').hide();
            $('#my_field').show();
            resize();
        }
    } else {
        if($('#my_field').is(':visible')) {
            $('#my_field').hide();
            $('#enemy_field').show();
            resize();
        }   
        if(state.game === 'OK') {
            let cell = $($($('#enemy_field').find('.row')[state.shoot[0]]).find('.cell')[state.shoot[1]]);
            $('#spinner').css({left: cell.offset().left + 'px', top: cell.offset().top + 'px'});
            $('#spinner').show();
            $('#my_shoot').html($($('#enemy_field').find('.head_row').find('.head_cell')[state.shoot[1]]).html() + ' ' + $($('#enemy_field').find('.row')[state.shoot[0]]).find('.left_cell').html());
            let top = cell.offset().top;
            let left = cell.offset().left;
            let radius = '';
            $('#enemy_answer').css({['border-top-left-radius']: '', ['border-top-right-radius']: '', ['border-bottom-left-radius']: '', ['border-bottom-right-radius']: ''});
            $('#enemy_answer').show();
            if(state.shoot[0] < 5) {
                top += cell.height();
                radius = 'top';
            } else {
                top -= $('#enemy_answer').height() + cell.height() / 2;
                radius = 'bottom';
            }
            if(state.shoot[1] < 5) {
                left += cell.width();
                radius += '-left';
            } else {
                left -= $('#enemy_answer').width() + cell.width() / 2;
                radius += '-right';
            }
            if(radius !== '') {
                $('#enemy_answer').css({['border-' + radius + '-radius']: 0});
            }
            $('#enemy_answer').css({left: left, top: top});
        }
    }
}

function open_menu() {
    if(state.game !== 'OK') {
        $('#new_game').hide();
        $('#other_field').hide();
        $('#both_players').show();
        $('#player_me').show();
        $('#player_enemy').show();
    } else {
        $('#new_game').show();
        // if(state.players !== undefined && state.players.length == 2) {
            $('#other_field').show();
        // }
        $('#both_players').hide();
        $('#player_me').hide();
        $('#player_enemy').hide();
    }
    $('.field').addClass('blur');
    $('.field').prop('disabled',true);
    $('#menu').show();
}

function close_menu() {
    if($('#menu').is(':visible')) {
        $('#menu').hide();
        $('.field').removeClass('blur');
        $('.field').prop('disabled',false);
    }
}

function render_field(id) {
    $(id).html('');
    if($(id + ' .head_row').length == 0) {
        let row = $('<div class="head_row" style="height: 1px"/>');
        $('<div class="corner_cell menu_link">☰</div>').appendTo(row);
        for(let j = 1; j <= 10; j++) {
            let cell = $('<div class="head_cell"/>');
            cell.html(String.fromCharCode('А'.charCodeAt(0) + j - 1 + (j < 10 ? 0 : 1)));
            cell.appendTo(row);
        }
        $('<div class="corner_cell"/>').appendTo(row);
        row.appendTo(id);
        for(let i = 0; i < 10; i++) {
            row = $('<div class="row"/>');
            let cell = $('<div class="left_cell"/>');
            cell.html((i + 1).toString());
            cell.appendTo(row);
            for(let j = 0; j < 10; j++) {
                let cell = $('<div class="cell sea">❌</div>');
                cell.data({row: i, col: j})
                cell.appendTo(row);
            }
            $('<div class="left_cell">&#160;</div>').appendTo(row);
            row.appendTo(id);
        }
        row = $('<div class="row"  style="height: 1px"/>');
        $('<div class="corner_cell"/>').appendTo(row);
        for(let j = 1; j <= 10; j++) {
            $('<div class="footer_cell">&#160;</div>').appendTo(row);
        }
        $('<div class="corner_cell"/>').appendTo(row);
        row.appendTo(id);
    }
}

function get_state(query) {
    return fetch(main + (query ? '?' + query : '')).then(data => data.json()).then(data => {
        changes.splice(0, changes.length);
        Object.keys(data).forEach(k => {
            state[k] = data[k];
            changes.push(k);
        });
        return Promise.resolve();
    });
}    

function resize() {
    if(orientation === 'portrait' && window.innerHeight < window.innerWidth || orientation === 'landscape' && window.innerHeight > window.innerWidth) {
        window.location.reload();
    }
    let cell_size = 0;
    let field = $('.field:visible')
    if(window.innerHeight < window.innerWidth) {
        field.height(window.innerHeight);
        field.width('auto');
        cell_size = field.find('.cell').height();
        field.find('.cell').width(cell_size);
        orientation = 'landscape';
    } else {
        field.width(window.innerWidth);
        field.height('auto');
        cell_size = field.find('.cell').width()
        field.find('.cell').height(cell_size);
        orientation = 'portrait';
    }
    font_size = Math.floor(cell_size * font_size_factor);
    button_font_size = Math.floor(cell_size * button_font_size_factor);

    field.find('.head_row, .left_cell, .cell').css({fontSize: font_size + 'px'});
    // field.find('.left_cell').css({fontSize: font_size + 'px'});

    let width = field.outerWidth();
    let height = field.outerHeight();
    field.css({left: (window.innerWidth - width) / 2, top: (window.innerHeight - height) / 2});
    
    let c = $($($('.field:visible').find('.row')[0]).find('.cell')[0]);
    $('#menu').css({left: c.offset().left + 'px', top: c.offset().top + 'px'});
    $('#spinner').css({width: cell_size + 'px', height: cell_size + 'px'});
    $('.game_start, .game_over, #game_over_wrong_answer, #game_over_wrong_layout').each((i, e) => {
        $(e).css({left: (window.innerWidth - $(e).width()) / 2, top: (window.innerHeight - $(e).height()) / 2});
    });
}

























})();