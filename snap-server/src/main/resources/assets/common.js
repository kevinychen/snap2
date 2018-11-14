function post(args, callback) {
  fetch('/api' + args.path, { method: 'POST', body: args.body, headers: args.headers })
    .then(response => {
      if (!response.ok) {
        alert('Error: ' + response.statusText);
        return;
      }
      return response.json().then(callback);
    })
}

function postJson(args, callback) {
  post({
    path: args.path,
    body: JSON.stringify(args.body),
    headers: { 'Content-type': 'application/json' },
  }, callback);
}
