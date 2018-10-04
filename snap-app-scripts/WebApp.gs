/**
 * Deployed here:
 * https://script.google.com/home/projects/1pIMTaT1S2eJ2raU_fJHladPb9vrqyCyDXZybOZIxf2gAJwxoG7icMVUS
 */

SERVICE_USER = "sheets-creator@snap-187301.iam.gserviceaccount.com";

/**
 * Processes a POST request. The request body should contain a command that is one of the following:
 *
 * - insertImage
 */
function doPost(e) {
  var params = JSON.parse(e.postData.contents);
  
  if (Session.getActiveUser().getEmail() != SERVICE_USER) {
    console.error("Incorrect user");
    return;
  }
  
  if (params.command == "insertImage") {
    insertImage(params);
  }
}

/**
 * Add an image to a sheet that the user owns. The params object should contain the following fields:
 *
 * spreadsheetId: ID of spreadsheet, e.g. "1xNDWJXOekpBBV2hPseQwCRR8Qs4LcLOcSLDadVqDA0E"
 * sheetName: name of sheet, e.g. "Sheet1"
 * url: url of image, e.g. "https://www.google.com/images/srpr/logo3w.png"
 * column: 1-indexed column, e.g. 1
 * row: 1-indexed row, e.g. 1
 * offsetX: x-offset from cell position, e.g. 0
 * offsetY: y-offset from cell position, e.g. 0
 */
function insertImage(params) {
  SpreadsheetApp.openById(params.spreadsheetId).getSheetByName(params.sheetName).insertImage(params.url, params.column, params.row, params.offsetX, params.offsetY);
}

