## Development

### One-time setup

To develop on and run the Snap server, you need JDK 11+. All commands should be run in this directory (snap-server).

- Update the [gradle.properties](gradle.properties) configuration file with your platform, one of `linux-x86_64`, `macosx-x86_64`, or `windows-x86_64`.

- Download required data files by running `./gradlew downloadFiles`.

#### Setup Google API service account

Running the crossword tool or grid parser requires a Google API service account.

- Visit the [Google Cloud Platform](https://console.cloud.google.com/home) and create a project. In the "IAM & admin" tab, select "Service accounts" and click "Create service account". You can use "sheets-creator" as the name. Note the service account ID; this is the email address that you must share your Google Sheets with to use Snap.

- Download a credentials file for the service account. Click "Create key" and download the JSON file. Name it `google-api-credentials.json` and save it in this directory.

- Allow the service user to use Google Drive APIs. In the navigation menu of the Google Cloud console page, select "APIs & Services" and click "Enable APIs and services". Enable both the Google Drive API and Google Sheets API.

- Adding overlay images is not well-supported in the Google Sheets API. The current workaround is to create a new [App Script](http://script.google.com). Give your service account access to the script's container Google Sheet, then copy the contents of [WebApp.gs](WebApp.gs) into the script. Then, in the "Publish" menu, click "Deploy as web app" and select "Execute the app as:" yourself instead of the user accessing. A popup dialog will display the URL of the published web app; it looks like `https://script.google.com/macros/s/.../exec`. Copy the URL into the `googleServerScriptUrl` field in the [gradle.properties](gradle.properties) file. Also update the `draftSpreadsheetId` field with the spreadsheet ID of the script container Google Sheet. Finally, in the "Run" menu, run any function and click through the Google popup to allow the script to be run.

#### Install Google Chrome binary

This is needed for running the crossword tool or grid parser with HTML URLs (it is not needed for images or PDFs).

- Steps for Ubuntu can be found [here](https://blog.softhints.com/ubuntu-16-04-server-install-headless-google-chrome/).

### Starting the server

Run `./gradlew run`.

Visit the app at http://localhost:8080.

