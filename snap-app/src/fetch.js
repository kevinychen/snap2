const SERVER = process.env.REACT_APP_LOCAL ? "http://localhost:8080/api" : "/api";

function fetchBackend(path, args, onSuccess, onError) {
    fetch(SERVER + path, args).then(response => {
        if (response.ok) {
            onSuccess && onSuccess(response);
        } else {
            response.json().then(body => {
                alert(body.message);
            });
        }
    });
}

function get(args, onSuccess, onError) {
    fetchBackend(args.path, { method: 'GET' }, onSuccess, onError);
}
exports.get = get;

exports.getJson = function ( args, onSuccess, onError) {
    get({ path: args.path }, response => {
        response.json().then(body => {
            onSuccess && onSuccess(body);
        });
    }, onError);
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
