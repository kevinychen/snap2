Python
======

To use Snap's Python functions, add the following to your `.bashrc`:

    export PYTHONPATH=/path/to/snap2/snap-python
    alias snap='PYTHONSTARTUP=/path/to/snap2/snap-python/bootstrap.py python'

You can then access the functions anywhere:

    $ python
    >>> from util import anagram
    >>> anagram('aaagmnr')
    ['anagram', 'mangara']

Or, start a Python console with the functions already boostrapped:

    $ snap
    >>> anagram('aaagmnr')
    ['anagram', 'mangara']

