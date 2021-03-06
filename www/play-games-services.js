var exec = require('cordova/exec');
var PLAY_GAMES_SERVICES = 'PlayGamesServices';
var PlayGamesServices = function () {
    this.name = PLAY_GAMES_SERVICES;
};
var actions = ['signIn', 'signOut', 'isSignedIn','startMatch','checkMatch','playMatch','reMatch','finishMatch','cancelMatch','leaveMatch',
                'autoMatch', 'showAchievements', 'showLeaderBoard','submitScore'];

actions.forEach(function (action) {
    PlayGamesServices.prototype[action] = function (data, success, failure) {
        var defaultSuccessCallback = function () {
                console.log(PLAY_GAMES_SERVICES + '.' + action + ': executed successfully');
            };

        var defaultFailureCallback = function () {
                console.warn(PLAY_GAMES_SERVICES + '.' + action + ': failed on execution');
            };

        if (typeof data === 'function') {
            failure = success || defaultFailureCallback;
            success = data;
            data = {};
        } else {
            data = data || {};
            success = success || defaultSuccessCallback;
            failure = failure || defaultFailureCallback;
        }

        exec(success, failure, PLAY_GAMES_SERVICES, action, [data]);
    };
});

module.exports = new PlayGamesServices();

