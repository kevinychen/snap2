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

- To publish:

        yarn build
        clasp push
        clasp version [description]
        clasp deploy [version] [description]

See https://developers.google.com/apps-script/guides/clasp for details.

