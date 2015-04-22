var Context = {};
var ALBus = {};
ALBus.invoke = function (options) {
    options = options || {};
    var url = options.url + '/' + options.tx;
    if (options.version !== undefined) url = url + '/' + options.version;
    if (options.context !== undefined) for (var prop in options.context)
        Context[prop] = options.context[prop];
    var headers = {};
    for (var prop in Context)
        headers['X-BUS-Context-' + prop] = Context[prop];
    var auth_pwd = options.username !== undefined && options.password != undefined;
    var auth_key = options.token != undefined;
    if (auth_pwd) {
        headers['X-BUS-Context-USERNAME'] = options.username;
        headers['X-BUS-Context-PASSWORD'] = options.password;
        headers['X-BUS-Context-TOKEN'] = undefined;
    }
    if (auth_key) {
        headers['X-BUS-Context-USERNAME'] = undefined;
        headers['X-BUS-Context-PASSWORD'] = undefined;
        headers['X-BUS-Context-TOKEN'] = options.token;
    }

    $.ajax(url, {
        type: 'post',
        data: JSON.stringify(options.arguments),
        success: function (data) {
            // copy headers into context, except token based on
            // auth_pwd/auth_key flag
            if (data.error && options.exception) options.exception(data.error.code, data.error.message);
            else if (options.callback) options.callback(data.result);
        },

        // constants
        contentType: 'application/json',
        accepts: {
            json: 'application/json'
        },
        processData: false,
        dataType: 'json',
        headers: headers
    });
};
ALBus.service = function (url, tx, version) {
    return new function () {
    };
}
