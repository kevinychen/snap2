SERVER_SOCKET_ADDRESS = "167.99.173.63:8080";
SERVICE_USER = "sheets-creator@snap-187301.iam.gserviceaccount.com";

function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu('Puzzlehunt Tools')
      .addItem('Convert background colors to RGB values', 'getBackgroundRGBs')
      .addItem('Set background colors from RGB values', 'setBackgroundRGBs')
      .addItem('Make horizontal hexagonal grid', 'makeHorizontalHexagonalGrid')
      .addItem('Make vertical hexagonal grid', 'makeVerticalHexagonalGrid')
      .addItem('Configure puzzle bot', 'configureBot')
      .addToUi();
}

function getBackgroundRGBs() {
  var range = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getActiveRange();
  range.setValues(range.getBackgrounds());
}

function setBackgroundRGBs() {
  var range = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getActiveRange();
  range.setBackgrounds(range.getValues());
}

function makeHorizontalHexagonalGrid() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var range = sheet.getActiveRange();
  for (var i = 0; i < range.getNumRows(); i++) {
    for (var j = i % 2; j < range.getNumColumns() - 1; j += 2) {
      sheet.getRange(range.getRow() + i, range.getColumn() + j, 1, 2).merge();
    }
  }
}

function makeVerticalHexagonalGrid() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var range = sheet.getActiveRange();
  for (var j = 0; j < range.getNumColumns(); j++) {
    for (var i = j % 2; i < range.getNumRows() - 1; i += 2) {
      sheet.getRange(range.getRow() + i, range.getColumn() + j, 2, 1).merge();
    }
  }
}

function configureBot() {
  var spreadsheetId = SpreadsheetApp.getActiveSpreadsheet().getId();
  DriveApp.getFileById(spreadsheetId).addEditor(SERVICE_USER);

  var html = '<html><body><div>This sheet has been shared with the Snap bot. Click the links below for more resources.</div>' +
    '<div><li><a href="http://' + SERVER_SOCKET_ADDRESS + '/index.html?spreadsheetId=' +
    spreadsheetId + '" target="blank" onclick="google.script.host.close()">Open grid parser</a></li></div></body></html>';
  SpreadsheetApp.getUi().showModelessDialog(HtmlService.createHtmlOutput(html), "Shared with Snap bot");
}

/**
 * Returns all 26 Caesar shifts of the given string.
 *
 * @param {string} string The starting string.
 * @return 26 rows consisting of all Caesar shifts of the string, one per row.
 * @customfunction
 */
function CAESAR(string) {
  var shifts = [];
  for (var i = 0; i < 26; i++) {
    shifts.push(SHIFT(string, i))
  }
  return shifts;
}

/**
 * Removes characters from a string. Removing a character will only remove the first occurrence of that character in the string, but the same character can be removed multiple times.
 *
 * @param {string} string The starting string.
 * @param {string} toRemove The characters to remove.
 * @return A string consisting of the remaining characters in the same order as the starting string.
 * @customfunction
 */
function REMOVE(string, toRemove) {
  if (string.map) {
    return string.map(function(stringItem, i) {
      return REMOVE(stringItem, toRemove[i]);
    });
  }
  for (var i = 0; i < toRemove.length; i++) {
    var c = toRemove.charAt(i);
    var index = string.indexOf(c);
    if (index === -1) {
      return "No (" + c + ")";
    } else {
      string = string.substring(0, index) + string.substring(index + 1);
    }
  }
  return string;
}

/**
 * Reverses a row or column of cells.
 *
 * @param {array} values The row or column to reverse.
 * @return An array consisting of the row or column in reverse direction.
 * @customfunction
 */
function REVERSE(values) {
  return values.reverse();
}

/**
 * Shifts the string by the given shift.
 *
 * @param {string} string The starting string.
 * @param {number} shift the positive integer value to shift by.
 * @return the shifted string.
 * @customfunction
 */
function SHIFT(string, shift) {
  var shifted = "";
  for (var i = 0; i < string.length; i++) {
    var code = string.charCodeAt(i);
    if (code >= 65 && code <= 90) {
      code = 65 + (code - 65 + shift) % 26;
    } else if (code >= 97 && code <= 122) {
      code = 97 + (code - 97 + shift) % 26;
    }
    shifted += String.fromCharCode(code);
  }
  return shifted;
}

