Snap Server
===========

To develop on and run the Snap server, you need JDK 11+. All commands should be run in this directory (snap-server).

- Snap requires a Google API service user in order to edit Google Drive documents. To create a new service user, visit the [Google Cloud Platform](https://console.cloud.google.com/home) and create a project. In the "IAM & admin" tab, select "Service accounts" and click "Create service account". You can use "sheets-creator" as the name. Note the service account ID; this is the email address that you must share your Google Sheets with to use Snap.

- Download a credentials file for the service account. Click "Create key" and download the JSON file. Name it `google-api-credentials.json` and save it in this directory.

- Allow the service user to use Google Drive APIs. In the navigation menu of the Google Cloud console page, select "APIs & Services" and click "Enable APIs and services". Enable both the Google Drive API and Google Sheets API.

- Adding images is currently only supported in Apps Script and not in the Google Sheets API. The current workaround is to create a new [script](http://script.google.com) and copy the contents of [WebApp.gs](WebApp.gs), but with the value of `SERVICE_USER` at the top renamed as appropriate. Then, in the "Publish" menu, click "Deploy as web app" and select "Execute the app as:" yourself instead of the user accessing. A popup dialog will display the URL of the published web app; it looks like `https://script.google.com/macros/s/.../exec`. Copy the URL into the `googleServerScriptUrl` field in the [server.properties](server.properties) file. Finally, in the "Run" menu, run any function and click through the Google popup to allow the script to be run.

- Update the [gradle.properties](gradle.properties) configuration file with your platform, one of `linux-x86_64`, `macosx-x86_64`, or `windows-x86_64`.

- Download required data files by running `./gradlew downloadFiles`.

- Uploading HTML requires installing the Google Chrome binary. Steps for Ubuntu can be found [here](https://blog.softhints.com/ubuntu-16-04-server-install-headless-google-chrome/).

- Start the Snap server by running `./gradlew run`.

- Visit the app at `http://localhost:8080`, or whatever address your server is hosted at.

You can develop on Snap by running `./gradlew eclipse` and then importing "Existing Projects into Workspace" in Eclipse. Files are in the standard Java project layout, with the entry point at [SnapServer.java](src/main/java/com/kyc/snap/server/SnapServer.java) and web assets under [assets](src/main/resources/assets).

