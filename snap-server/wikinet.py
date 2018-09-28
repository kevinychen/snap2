"""
Python convenience script with the same implementations of "find" and "directFind" in Wikinet.java.

To run the Python console with the functions in this file preloaded:
$ PYTHONSTARTUP=wikinet.py python
"""

from collections import namedtuple

NUM_PARTITIONS = 65536

Article = namedtuple('Article', 'title redirect summary')

def java_string_hashcode(s):
    h = 0
    for c in s:
        h = (31 * h + ord(c)) & 0xFFFFFFFF
    return ((h + 0x80000000) & 0xFFFFFFFF) - 0x80000000

def startswith_ignore_case(str, prefix):
    return str.upper().startswith(prefix.upper())

def from_tsv(tsv):
    (title, label, msg) = tsv.split('\t')
    if label == 'REDIRECT':
        return Article(title=title, redirect=msg, summary=None)
    else:
        return Article(title=title, redirect=None, summary=msg)

def to_tsv(article):
    if article.redirect != None:
        return '%s\t%s\t%s' % (article.title, 'REDIRECT', article.redirect)
    else:
        return '%s\t%s\t%s' % (article.title, 'SUMMARY', article.summary)

def direct_find(title, exact):
    hash = abs(java_string_hashcode(title.upper())) % NUM_PARTITIONS
    prefix = title + '\t'
    articles = set()
    for line in open('./data/wikinet/partitions/%04x' % hash):
        if (line.startswith(prefix) if exact else startswith_ignore_case(line, prefix)):
            articles.add(from_tsv(line))
    return articles

def find(title):
    articles = set()
    for article in direct_find(title, False):
        if article.redirect != None:
            articles.update(direct_find(article.redirect, True))
        else:
            articles.add(article)
    return articles

