Development
===========

- Install Clasp and login:

        npm install @google/clasp -g
        clasp login

- (Optional) If you do not have access to this project, you can create a new one with the same contents.

        clasp create [scriptTitle]

- To develop this add-on:

        yarn install
        yarn start

- To push to the container Google Sheet:

        yarn build
        clasp push

- To deploy as an add-on

        clasp deploy -d <description>

If this is the first deploy, follow the instructions [here](https://docs.google.com/document/d/15o4s7MSfcuyudLsUKjmFaS-_k-ftDLdv_eRDQaNp_vs/edit?usp=sharing). Otherwise, navigate to [GSuite Marketplace](https://console.cloud.google.com/apis/api/appsmarket-component.googleapis.com/googleapps_sdk), select your project, go to the "Configuration" tab, and change the version number to the newly deployed version. Your change will only sync after around 24 hours.

To install the add-on, go to the "Publish" tab in GSuite Marketplace, click the App URL that looks like https://gsuite.google.com/marketplace/app/xxxxx, and click "INSTALL".

See https://developers.google.com/apps-script/guides/clasp for details on Clasp.

