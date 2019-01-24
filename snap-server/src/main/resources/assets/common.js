function post(args, callback, errorCallbacks) {
  $('#loader').show();
  fetch('/api' + args.path, { method: 'POST', body: args.body, headers: args.headers })
    .then(response => {
      $('#loader').hide();
      if (response.ok) {
        response.json().then(callback);
      } else if (errorCallbacks && errorCallbacks[response.status]) {
        errorCallbacks[response.status]();
      } else {
        alert('An unknown error occurred.');
      }
    })
}

function postJson(args, callback, errorCallbacks) {
  post({
    path: args.path,
    body: JSON.stringify(args.body),
    headers: { 'Content-type': 'application/json' },
  }, callback, errorCallbacks);
}
