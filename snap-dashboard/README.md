Hunt Dashboard
==============

The following [template](https://docs.google.com/spreadsheets/d/1RDu12f795VzK5beceMhsV4Q2kIRzxHWX06fK5-buv94) can be copied to create a lightweight hunt dashboard directly in Google Sheets. This allows you to see a list of puzzle URLs and spreadsheet URLs in one place, and allows you to organize them using all of Google Sheets functionality for free. Furthermore, the template supports the following features:

- Entering the URL of a puzzle will trigger a popup dialog to automatically generate a Google Sheet, autocomplete the "Title" column, and create a Slack channel with all members of your team.

![Generating a puzzle](../docs/dashboard-generate.gif)

- Entering the answer to a puzzle will trigger a popup dialog to automatically rename the corresponding Google Sheet and notify the Slack channel for the puzzle.

![Solving a puzzle](../docs/dashboard-solve.gif)

To create a dashboard:

1. Go to the [template](https://docs.google.com/spreadsheets/d/1RDu12f795VzK5beceMhsV4Q2kIRzxHWX06fK5-buv94) and in the File menu, click "Make a copy...". Choose an empty parent folder that can be shared with the Snap service user. The name of the sheet should be the name of the puzzle hunt.

1. In the "Tools" menu, click "Script editor" to open the "Hunt Dashboard script". If the script doesn't appear, you can copy the contents from [here](Dashboard.gs).

1. If you want to enable Slack integration, create a new Slack app [here](https://api.slack.com/apps). In the "OAuth & Permissions" section, add the following permission scopes to your app: `channels:write`, `users:read`, `users:read.email`, `team:read`, and `chat:write:bot`. Install the app to your workspace, and copy the "OAuth Access Token" to the `SLACK_TOKEN` field in the Hunt Dashboard script.

1. Enter email addresses of any other members that should be invited to each newly created Slack channel in the `SLACK_EMAIL_ADDRESSES` field. Don't include yourself (the person who created the Slack app); you will already be invited to every channel.

1. In the "Run" menu and "Run function" submenu of the Hunt Hashboard script, click "setup". This will share the entire parent folder with the Snap service user, and install the triggers to automatically show popups.

1. Enter a puzzle URL in the first column of the dashboard to begin!

