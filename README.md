Snap
====

Snap provides tooling for puzzle hunts, such as parsing of grids and crosswords and exporting them to Google Sheets. Try Snap at http://167.99.173.63:8080/.

Development
-----------

Install OpenCV (Snap uses libraries for OpenCV 3.4.0). On Ubuntu:

    sudo apt-get update
    sudo apt-get install libopencv-dev

Add a `google-api-credentials.json` file with Snap's [service account credentials](https://cloud.google.com/docs/authentication/production#providing_credentials_to_your_application).

Update the `config.yml` configuration file with the socket address that the Snap server will run at.

Download data files:

    cd snap-server
    ./gradlew downloadFiles

Run the Snap server (requires JDK 8+):

    cd snap-server
    ./gradlew run

Visit the app at `http://localhost:8080`.

Snap server endpoints can also be used through Google Sheet custom functions. In a Google Sheet, click "Tools" and "Script editor", and then copy the contents of the files under `snap-app-scripts`. The menu items and custom functions will be available in the Google Sheet after refreshing the page.

Wikinet
-------

The Wikinet generator stores summaries for all Wikipedia articles and indexes them for quick lookup. To generate the Wikinet:

    cd snap-server
    ./gradlew generateWikinet

Generating the Wikinet involves downloading about 50 files with a total of about 20GB of data from the internet, so may take many hours.

Python
------

To use the Python library, add the following to your `.bashrc`:

    export PYTHONPATH=/path/to/snap2/snap-python

You can then access Snap Python functions anywhere:

    $ python
    >>> from wikinet import find
    >>> wikinet.find('Ajaigarh')
    set([Article(title='Ajaigarh', redirect=None, summary='Ajaigarh or Adjygurh is a town and a nagar panchayat in the Panna District of Madhya Pradesh state in central India.\n')])

