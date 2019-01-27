function post(args, callback) {
  $('#loader').show();
  fetch('/api' + args.path, { method: 'POST', body: args.body, headers: args.headers })
    .then(response => {
      $('#loader').hide();
      response.json().then(body => {
        if (response.ok) {
          if (callback) {
            callback(body);
          }
        } else if (body.message) {
          alert(body.message);
        } else {
          alert('An unknown error occurred.');
        }
      });
    })
}

function postJson(args, callback, errorCallbacks) {
  post({
    path: args.path,
    body: JSON.stringify(args.body),
    headers: { 'Content-type': 'application/json' },
  }, callback, errorCallbacks);
}
