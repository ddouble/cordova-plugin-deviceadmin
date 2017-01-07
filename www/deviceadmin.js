var channel = require('cordova/channel'),
    exec = require('cordova/exec'),
    cordova = require('cordova');

function DeviceAdmin() {

}

DeviceAdmin.prototype.register = function(success, error) {
    exec(success, error, "DeviceAdmin", "register", []);
};

DeviceAdmin.prototype.unRegister = function(success, error) {
    exec(success, error, "DeviceAdmin", "unRegister", []);
};

DeviceAdmin.prototype.isAdmin = function(success, error) {
    exec(success, error, "DeviceAdmin", "isAdmin", []);
};

DeviceAdmin.prototype.lock = function(success, error) {
    exec(success, error, "DeviceAdmin", "lock", []);
};

DeviceAdmin.prototype.turnOnScreen = function(keepOn, success, error) {
    exec(success, error, "DeviceAdmin", "turnOnScreen", [keepOn]);
};

DeviceAdmin.prototype.turnOffScreen = function(success, error) {
    exec(success, error, "DeviceAdmin", "turnOffScreen", []);
};



channel.onCordovaReady.subscribe(function() {
    // 监听来自native的消息
    exec(listener, null, 'DeviceAdmin', 'listen', []);

    function listener(message) {
        console.log(message);
        //process message from java here ...
    }
});

module.exports = new DeviceAdmin();
