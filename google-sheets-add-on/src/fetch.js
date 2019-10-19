function fetchBackend(path, args, onSuccess, onError) {
    fetch('https://util.in/api' + path, args).then(response => {
        if (response.ok) {
            onSuccess && onSuccess(response);
        } else {
            (onError || alert)(response);
        }
    });
}

exports.get = function (args, onSuccess, onError) {
    fetchBackend(args.path, { method: 'GET' }, onSuccess, onError);
}

function post(args, onSuccess, onError) {
    fetchBackend(args.path, {
        method: 'POST',
        body: args.body,
        headers: args.headers,
    }, response => {
        response.json().then(body => {
            onSuccess && onSuccess(body);
        });
    }, onError);
}
exports.post = post;

exports.postJson = function (args, onSuccess, onError) {
    post({
        path: args.path,
        body: JSON.stringify(args.body),
        headers: { 'Content-type': 'application/json' },
    }, onSuccess, onError);
}
