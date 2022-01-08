
SERVICE_USER = "sheets-creator@snap-187301.iam.gserviceaccount.com";
CUSTOM_FUNCTION_METADATA_KEY = "SNAP_CUSTOM_FUNCTION";

function onInstall() {
  onOpen();
}

function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createAddonMenu()
    .addItem('Open sidebar', 'openSidebar')
    .addItem('Remove blank lines', 'removeBlankLines')
    .addItem('Convert background colors to RGB values', 'getBackgroundRGBs')
    .addItem('Set background colors from RGB values', 'setBackgroundRGBs')
    .addItem('Make horizontal hexagonal grid', 'makeHorizontalHexagonalGrid')
    .addItem('Make vertical hexagonal grid', 'makeVerticalHexagonalGrid')
    .addToUi();
}

/*************************************
 * HELPER FUNCTIONS USED BY onOpen() *
 *************************************/

function openSidebar() {
  var html = HtmlService.createHtmlOutputFromFile('dist/index')
      .setTitle('Snap');
  SpreadsheetApp.getUi().showSidebar(html);
}

/**
 * Shifts lines upwards to remove all empty lines.
 */
function removeBlankLines() {
  var range = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getActiveRange();
  var newValues = [];
  for (var i = 0; i < range.getNumRows(); i++) {
    if (!range.offset(i, 0, 1).isBlank()) {
      newValues.push(range.offset(i, 0, 1).getValues()[0]);
    }
  }
  var blankLine = [];
  for (var j = 0; j < range.getNumColumns(); j++) {
    blankLine.push("");
  }
  while (newValues.length < range.getNumRows()) {
    newValues.push(blankLine);
  }
  range.setValues(newValues);
}

/**
 * Fills each cell with the text corresponding to the cell's background color (e.g. "#ffffff")
 */
function getBackgroundRGBs() {
  var range = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getActiveRange();
  range.setValues(range.getBackgrounds());
}

/**
 * Sets the background color of each cell to the value of the text (which must be of the form "#ffffff")
 */
function setBackgroundRGBs() {
  var range = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getActiveRange();
  range.setBackgrounds(range.getValues());
}

/**
 * Creates a hexagonal grid by merging every other cell with the one to its right, with each row staggered from the ones above and below.
 */
function makeHorizontalHexagonalGrid() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var range = sheet.getActiveRange();
  for (var i = 0; i < range.getNumRows(); i++) {
    for (var j = i % 2; j < range.getNumColumns() - 1; j += 2) {
      sheet.getRange(range.getRow() + i, range.getColumn() + j, 1, 2).merge();
    }
  }
}

/**
 * Creates a hexagonal grid by merging every other cell with the one below it, with each column staggered from the ones beside it.
 */
function makeVerticalHexagonalGrid() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var range = sheet.getActiveRange();
  for (var j = 0; j < range.getNumColumns(); j++) {
    for (var i = j % 2; i < range.getNumRows() - 1; i += 2) {
      sheet.getRange(range.getRow() + i, range.getColumn() + j, 2, 1).merge();
    }
  }
}

/****************************************
 * HELPER FUNCTIONS USED BY CLIENT HTML *
 ****************************************/

function shareWithServer() {
  SpreadsheetApp.getActiveSpreadsheet().addEditor(SERVICE_USER);
}

function getActiveCell() {
  var sheet = SpreadsheetApp.getActiveSheet();
  var cell = sheet.getActiveCell();
  return {
    spreadsheetId: SpreadsheetApp.getActiveSpreadsheet().getId(),
    sheetId: sheet.getSheetId(),
    row: cell.getRow() - 1,
    col: cell.getColumn() - 1,
  };
}

function getSelectedRangeA1Notation() {
  return SpreadsheetApp.getActiveSheet().getActiveRange().getA1Notation();
}

function getSelectedRange() {
  var range = SpreadsheetApp.getActiveSheet().getActiveRange();
  return { rangeA1: range.getA1Notation(), values: range.getValues() };
}

function setValues(range, values) {
  return SpreadsheetApp.getActiveSheet().getRange(range).setValues(values);
}

/**
 * Given a word bank of words and a range of used words, do the following:
 * (1) Highlight in gray all words in the word bank that are used (present in the range of used words)
 * (2) Highlight in red all words in the range of used words that aren't in the word bank.
 */
function highlightUsed(wordBankRange, usedWordsRange) {
  var sheet = SpreadsheetApp.getActiveSheet();
  var rules = sheet.getConditionalFormatRules();
  var wordBankStart = Utilities.formatString('INDEX(%s, 1, 1)', wordBankRange);
  var usedWordsStart = Utilities.formatString('INDEX(%s, 1, 1)', usedWordsRange);
  var wordBankRule = SpreadsheetApp.newConditionalFormatRule()
    .withCriteria(SpreadsheetApp.BooleanCriteria.CUSTOM_FORMULA, [Utilities.formatString('=AND(COUNTIF(%s, %s) > 0, NOT(ISBLANK(%s)))', usedWordsRange, wordBankStart, wordBankStart)])
    .setBackground('#D9D9D9')
    .setRanges([sheet.getRange(wordBankRange)])
    .build();
  var usedWordsRules = SpreadsheetApp.newConditionalFormatRule()
    .withCriteria(SpreadsheetApp.BooleanCriteria.CUSTOM_FORMULA, [Utilities.formatString('=AND(COUNTIF(%s, %s) = 0, NOT(ISBLANK(%s)))', wordBankRange, usedWordsStart, usedWordsStart)])
    .setBackground('#F4CCCC')
    .setRanges([sheet.getRange(usedWordsRange)])
    .build();
  sheet.setConditionalFormatRules(rules.concat(wordBankRule, usedWordsRules));
}

function getCustomFunctionMetadata() {
  var customFunctionMetadata = SpreadsheetApp.getActiveSpreadsheet().getDeveloperMetadata().filter(function(metadata) {
    return metadata.getKey() === CUSTOM_FUNCTION_METADATA_KEY;
  })[0];
  return customFunctionMetadata;
}

function getCustomFunction() {
  var customFunctionMetadata = getCustomFunctionMetadata();
  if (customFunctionMetadata) {
    return customFunctionMetadata.getValue();
  }
}

function saveCustomFunction(customFunction) {
  var customFunctionMetadata = getCustomFunctionMetadata();
  if (customFunctionMetadata) {
    customFunctionMetadata.setValue(customFunction);
  } else {
    SpreadsheetApp.getActiveSpreadsheet().addDeveloperMetadata(CUSTOM_FUNCTION_METADATA_KEY, customFunction);
  }
}

/**
 * Rearranges the values in the currently selected range as follows.
 * First, divide the range into rectangular regions with the given starting width and height.
 * Then, change these rectangular regions into a shape with the given ending width and height.
 * The reading order of the values in each region remain the same.
 * For example, resize(range, 1, 2, 2, 1) would do the following:
 *
 * A B
 * C D      A C B D
 * E F  =>  E G F H
 * G H      I   J
 * I J
 */
function reshape(startWidth, startHeight, endWidth, endHeight) {
  var range = SpreadsheetApp.getActiveSheet().getActiveRange();
  var values = range.getValues();
  var blankValues = [];
  for (var i = 0; i < values.length; i++) {
    blankValues.push([]);
    for (var j = 0; j < values[i].length; j++) {
      blankValues[i].push("");
    }
  }
  range.setValues(blankValues);

  var numRegionsX = Math.ceil(values[0].length / startWidth);
  var numRegionsY = Math.ceil(values.length / startHeight);
  var newValues = [];
  for (var i = 0; i < numRegionsY * endHeight; i++) {
    newValues.push([]);
  }
  for (var i = 0; i < numRegionsY; i++) {
    for (var j = 0; j < numRegionsX; j++) {
      var regionValues = [];
      for (var ii = i * startHeight; ii < (i + 1) * startHeight; ii++) {
        for (var jj = j * startWidth; jj < (j + 1) * startWidth; jj++) {
          regionValues.push(ii < values.length && jj < values[ii].length ? values[ii][jj] : "");
        }
      }
      var index = 0;
      for (var ii = i * endHeight; ii < (i + 1) * endHeight; ii++) {
        for (var jj = j * endWidth; jj < (j + 1) * endWidth; jj++) {
          newValues[ii].push(index < regionValues.length ? regionValues[index] : "");
          index++;
        }
      }
    }
  }
  var newRange = SpreadsheetApp.getActiveSheet().getRange(range.getRow(), range.getColumn(), numRegionsY * endHeight, numRegionsX * endWidth)
  newRange.setValues(newValues);
  SpreadsheetApp.setActiveRange(newRange);
}

/********************
 * CUSTOM FUNCTIONS *
 ********************/

/**
 * Converts a string to uppercase and removes all non-letters.
 *
 * @param {string} input string to convert
 * @return {string} converted input
 * @customfunction
 */
function ANSWERIZE(input) {
  if (input.map) {
    return input.map(SLUG);
  }
  return input.toUpperCase().replace(/[^A-Z]/g, "");
}

/**
 * Caesar shifts each letter of the string by the given shift.
 *
 * @param {string} string The starting string.
 * @param {number=} shift the positive integer value to shift by.
 * @return the shifted string.
 * @customfunction
 */
function CAESAR(string, shift) {
  if (string.map) {
    return string.map(function(stringItem, i) {
      return CAESAR(stringItem, shift.map ? shift[i] : shift);
    });
  }
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

/**
 * Removes characters from a string. Removing a character will only remove the first occurrence of that character in the string, but the same character can be removed multiple times.
 * To remove a subsequence of characaters in order, use REMOVE_SUBSEQ instead.
 *
 * @param {string} string The starting string.
 * @param {string} toRemove The characters to remove.
 * @return A string consisting of the remaining characters in the same order as the starting string.
 * @customfunction
 */
function REMOVE(string, toRemove) {
  if (string.map) {
    return string.map(function(stringItem, i) {
      return REMOVE(stringItem, toRemove.map ? toRemove[i] : toRemove);
    });
  }
  for (var i = 0; i < toRemove.length; i++) {
    var c = toRemove.charAt(i);
    var index = string.indexOf(c);
    if (index === -1) {
      return 'No (' + c + ')';
    } else {
      string = string.substring(0, index) + string.substring(index + 1);
    }
  }
  return string;
}

/**
 * Removes a subsequence of characters from a string. The characters to remove must be in the same order as in the original string, but do not need to be adjacent.
 * To remove some characters in any order, use REMOVE instead.
 *
 * @param {string} string The starting string.
 * @param {string} toRemove The characters to remove.
 * @return A string consisting of the remaining characters in the same order as the starting string.
 * @customfunction
 */
function REMOVE_SUBSEQ(string, toRemove) {
  if (string.map) {
    return string.map(function(stringItem, i) {
      return REMOVE_SUBSEQ(stringItem, toRemove.map ? toRemove[i] : toRemove);
    });
  }
  var lastIndex = 0;
  for (var i = 0; i < toRemove.length; i++) {
    var c = toRemove.charAt(i);
    var index = string.indexOf(c, lastIndex);
    if (index === -1) {
      return 'No (' + c + ')';
    } else {
      string = string.substring(0, index) + string.substring(index + 1);
      lastIndex = index;
    }
  }
  return string;
}

/**
 * Splits a string into an array of its characters.
 *
 * @param {string} string The string to split.
 * @return The list of characters in the string.
 * @customfunction
 */
function CHARS(string) {
  if (string.map) {
    return string.map(CHARS);
  }
  return [string.split('')];
}

/**
 * Flips a range across the Y axis.
 *
 * @param {range} range The range to reverse.
 * @return The flipped range.
 * @customfunction
 */
function FLIP_HORIZ(range) {
  return range.map(function(row) {
    return row.reverse();
  });
}

/**
 * Flips a range across the X axis.
 *
 * @param {range} range The range to reverse.
 * @return The flipped range.
 * @customfunction
 */
function FLIP_VERT(range) {
  return range.reverse();
}

var ELEMENTS = [
  'H', 'He', 'Li', 'Be', 'B', 'C', 'N', 'O', 'F', 'Ne', 'Na', 'Mg', 'Al', 'Si', 'P', 'S', 'Cl', 'Ar', 'K', 'Ca', 'Sc', 'Ti', 'V', 'Cr', 'Mn', 'Fe', 'Co', 'Ni', 'Cu', 'Zn',
  'Ga', 'Ge', 'As', 'Se', 'Br', 'Kr', 'Rb', 'Sr', 'Y', 'Zr', 'Nb', 'Mo', 'Tc', 'Ru', 'Rh', 'Pd', 'Ag', 'Cd', 'In', 'Sn', 'Sb', 'Te', 'I', 'Xe', 'Cs', 'Ba', 'La', 'Ce', 'Pr', 'Nd',
  'Pm', 'Sm', 'Eu', 'Gd', 'Tb', 'Dy', 'Ho', 'Er', 'Tm', 'Yb', 'Lu', 'Hf', 'Ta', 'W', 'Re', 'Os', 'Ir', 'Pt', 'Au', 'Hg', 'Tl', 'Pb', 'Bi', 'Po', 'At', 'Rn', 'Fr', 'Ra', 'Ac', 'Th',
  'Pa', 'U', 'Np', 'Pu', 'Am', 'Cm', 'Bk', 'Cf', 'Es', 'Fm', 'Md', 'No', 'Lr', 'Rf', 'Db', 'Sg', 'Bh', 'Hs', 'Mt', 'Ds', 'Rg', 'Cn', 'Nh', 'Fl', 'Mc', 'Lv', 'Ts', 'Og'];

/**
 * Converts an atomic number to the 1/2-letter abbreviation of the corresponding element.
 *
 * @param {number=} input the atomic number
 * @return {string} the element abbreviation
 * @customfunction
 */
function NUM2EL(input) {
  if (input.map) {
    return input.map(NUM2EL);
  }
  return ELEMENTS[input - 1];
}

/**
 * Converts the 1/2-letter abbreviation of an element to its corresponding atomic number. This function is not case sensitive.
 *
 * @param {string} the 1/2-letter element abbreviation
 * @return {number} the atomic number
 * @customfunction
 */
function EL2NUM(input) {
  if (input.map) {
    return input.map(EL2NUM);
  }
  for (var i = 0; i < ELEMENTS.length; i++) {
    if (ELEMENTS[i].equalsIgnoreCase(input)) {
      return i + 1;
    }
  }
  return 'Not an element';
}

/**
 * Executes the custom function defined in "Snap Functions -> Define custom function".
 *
 * @customfunction
 */
function CUSTOM() {
  var customFunction = getCustomFunction();
  if (customFunction) {
    eval('var _ = ' + customFunction);
    return _.apply(null, arguments);
  }
  return "Not defined"
}

