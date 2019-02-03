SERVER_ORIGIN = 'https://util.in';
SERVICE_USER = 'sheets-creator@snap-187301.iam.gserviceaccount.com';

// Fill in from https://api.slack.com/apps. Use an empty string to disable Slack integration.
SLACK_TOKEN = 'token'
SLACK_EMAIL_ADDRESSES = [
  'user@domain.com'
]

/**
 * Call this function once (In the "Run" menu, select "Run function" -> "setup") to properly initialize this script.
 */
function setup() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet();
  ScriptApp.newTrigger('processEvent')
      .forSpreadsheet(sheet)
      .onEdit()
      .create();
  DriveApp.getFileById(sheet.getId()).getParents().next().addEditor(SERVICE_USER);
}

function processEvent(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet();
  var ui = SpreadsheetApp.getUi();
  var range = e.range;
  var row = range.rowStart;
  var col = range.columnStart;
  if (row == range.rowEnd && col == range.columnEnd && range.getValue()) {
    if (col == getNamedRange('PuzzleUrls').getColumn()) {
      var response = ui.alert("Generate spreadsheet?", '', ui.ButtonSet.OK_CANCEL);
      if (response == ui.Button.OK) {
        var data = post(
          Utilities.formatString(
            '%s/api/dashboard/%s/generate',
            SERVER_ORIGIN,
            SpreadsheetApp.getActiveSpreadsheet().getId()),
          {
            puzzleUrl: range.getValue(),
            title: getNamedRange('Titles').getCell(row, 1).getValue(),
            slackToken: SLACK_TOKEN,
            slackEmailAddresses: SLACK_EMAIL_ADDRESSES,
          });
        getNamedRange('Titles').getCell(row, 1).setValue(data.title);
        getNamedRange('SheetUrls').getCell(row, 1).setValue(data.sheetUrl);
        if (data.slackLink) {
          getNamedRange('SlackLinks').getCell(row, 1).setValue(data.slackLink);
        }
      }
    } else if (col == getNamedRange('Answers').getColumn()) {
      var response = ui.alert("Solve puzzle?", '', ui.ButtonSet.OK_CANCEL);
      if (response == ui.Button.OK) {
        var sheetUrl = getNamedRange('SheetUrls').getCell(row, 1).getValue();
        var sheetId = sheetUrl.match(new RegExp('https://docs\.google\.com/spreadsheets/d/([A-Za-z0-9_-]+)(/edit)?'))[1];
        post(
          Utilities.formatString(
            '%s/api/dashboard/%s/solve',
            SERVER_ORIGIN,
            SpreadsheetApp.getActiveSpreadsheet().getId()),
          {
            answer: getNamedRange('Answers').getCell(row, 1).getValue(),
            sheetId: sheetId,
            title: getNamedRange('Titles').getCell(row, 1).getValue(),
            slackToken: SLACK_TOKEN,
            slackLink: getNamedRange('SlackLinks').getCell(row, 1).getValue(),
          });
      }
    }
  }
}

function getNamedRange(name) {
  return SpreadsheetApp.getActiveSpreadsheet().getNamedRanges().filter(function(range) {
    return range.getName() == name;
  })[0].getRange();
}

function post(url, body) {
  var options = {
    method: 'POST',
    contentType: 'application/json',
    payload: JSON.stringify(body),
  };
  var httpResponse = UrlFetchApp.fetch(url, options);
  return JSON.parse(httpResponse.getContentText());
}

