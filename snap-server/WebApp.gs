/**
 * Processes a POST request. The request body should contain a command that is one of the following:
 *
 * - insertImage
 * - removeAllImages
 */
function doPost(e) {
  const params = JSON.parse(e.postData.contents);

  if (params.command === "insertImage") {
    insertImage(params);
  } else if (params.command === "removeAllImages") {
    removeAllImages(params);
  }
}

/**
 * Add an image to a sheet. The params object should contain the following fields:
 *
 * spreadsheetId: ID of spreadsheet
 * sheetId: ID of sheet
 * url: url of image, e.g. "https://www.google.com/images/srpr/logo3w.png"
 * column: 1-indexed column, e.g. 1
 * row: 1-indexed row, e.g. 1
 * offsetX: x-offset from cell position, e.g. 0
 * offsetY: y-offset from cell position, e.g. 0
 * width: width of image in sheet, e.g. 100
 * height: height of image in sheet, e.g. 100
 */
function insertImage(params) {
  const sheet = getSheet(params.spreadsheetId, params.sheetId);
  const image = sheet.insertImage(params.url, params.column, params.row, params.offsetX, params.offsetY);
  image.setWidth(params.width);
  image.setHeight(params.height);
}

/**
 * Removes all overlay images from a sheet. The params object should contain the following fields:
 *
 * spreadsheetId: ID of spreadsheet
 * sheetId: ID of sheet
 */
function removeAllImages(params) {
  const sheet = getSheet(params.spreadsheetId, params.sheetId);
  sheet.getImages().map(image => image.remove());
}

function getSheet(spreadsheetId, sheetId) {
  for (const sheet of SpreadsheetApp.openById(spreadsheetId).getSheets()) {
    if (sheet.getSheetId() === sheetId) {
      return sheet;
    }
  }
}

