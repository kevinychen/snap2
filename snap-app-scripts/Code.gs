SERVER_SOCKET_ADDRESS = "167.99.173.63:8080";

function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu('Puzzlehunt Tools')
      .addItem('Get background RGBs', 'getBackgroundRGBs')
      .addToUi();
}

function getBackgroundRGBs() {
  var range = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getActiveRange();
  for (var i = 1; i <= range.getNumRows(); i++) {
    for (var j = 1; j <= range.getNumColumns(); j++) {
      var cell = range.getCell(i, j);
      cell.setValue(cell.getBackground());
    }
  }
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
    var shift = "";
    for (var j = 0; j < string.length; j++) {
      var code = string.charCodeAt(j);
      if (code >= 65 && code <= 90) {
        code = 65 + (code - 65 + i) % 26;
      } else if (code >= 97 && code <= 122) {
        code = 97 + (code - 97 + i) % 26;
      }
      shift += String.fromCharCode(code);
    }
    shifts.push(shift);
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
 * Solves a trigram puzzle given a list of trigrams and a list of word lengths.
 *
 * @param {array} trigrams A row of cells, each consisting of one trigram in uppercase letters.
 * @param {string} wordLengths A string consisting of integers separated by non-digit characters.
 * @return The trigrams reordered into a single English string, with words separated by spaces.
 * @customfunction
 */
function SOLVE_TRIGRAM(trigrams, wordLengths) {
  var parsedTrigrams = trigrams[0];
  var parsedWordLengths = wordLengths.split(/[^0-9]/).filter(function(s) {
    return s.length > 0;
  }).map(function(s) {
    return parseInt(s);
  });
  var response = UrlFetchApp.fetch("http://" + SERVER_SOCKET_ADDRESS + "/api/words/trigram", {
    contentType: "application/json",
    method: "post",
    payload: JSON.stringify({
      trigrams: parsedTrigrams,
      wordLengths: parsedWordLengths,
    }),
  });
  return JSON.parse(response.getContentText()).solution.join(" ");
}

/**
 * Fetches suggestions for the given crossword clue. Note fetching suggestions is limited to roughly once every 9 seconds.
 *
 * @param {string} clue A crossword clue.
 * @param {number} numLetters An integer representing the number of letters in the solution.
 * @return A list of rows, each consisting of two columns: the crossword suggestion, and the confidence level (out of 5).
 * @customfunction
 */
function WORDPLAYS(clue, numLetters) {
  var response = UrlFetchApp.fetch("http://" + SERVER_SOCKET_ADDRESS + "/api/words/crossword", {
    contentType: "application/json",
    method: "post",
    payload: JSON.stringify({
      clue: clue,
      numLetters: numLetters,
    }),
  });
  return JSON.parse(response.getContentText()).suggestions.map(function(suggestion) {
    return [suggestion.suggestion, suggestion.confidence + " / 5"];
  });
}

