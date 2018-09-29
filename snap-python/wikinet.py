"""
Python convenience script with the same implementations of "find" and "directFind" in Wikinet.java.
"""

import collections
import util

NUM_PARTITIONS = 65536

Article = collections.namedtuple('Article', 'title redirect summary')

def strip_from(s, mark):
    index = s.find(mark)
    if index != -1:
        s = s[:index]
    return s

def normalize(title):
    return filter(lambda c: c.isalpha(), strip_from(title.lower(), ' ('))

def java_string_hashcode(s):
    h = 0
    for c in s:
        h = (31 * h + ord(c)) & 0xFFFFFFFF
    return ((h + 0x80000000) & 0xFFFFFFFF) - 0x80000000

def from_tsv(tsv):
    (normalized_title, title, label, msg) = tsv.rstrip('\n').split('\t')
    if label == 'REDIRECT':
        return Article(title=title, redirect=msg, summary=None)
    else:
        return Article(title=title, redirect=None, summary=msg)

def to_tsv(article):
    if article.redirect != None:
        return '%s\t%s\t%s\t%s' % (normalize(article.title), article.title, 'REDIRECT', article.redirect)
    else:
        return '%s\t%s\t%s\t%s' % (normalize(article.title), article.title, 'SUMMARY', article.summary)

def direct_find(title, exact=False):
    normalized_title = normalize(title)
    hash = abs(java_string_hashcode(normalized_title)) % NUM_PARTITIONS
    prefix = normalized_title + '\t'
    articles = []
    with open(util.data_file('wikinet/partitions/%04x' % hash)) as fh:
        for line in fh:
            if line.startswith(prefix):
                article = from_tsv(line)
                if not exact or article.title == title:
                    articles.append(article)
    return articles

def find(title, exact=False):
    """ Returns a set of all articles with the given title (case insensitive).

    >>> find('wikipedia')
    set([Article(title='Wikipedia', summary='Wikipedia is a free, collaborative, multilingual Internet encyclopedia.\n')])
    """
    articles = []
    for article in direct_find(title, exact=exact):
        if article.redirect != None:
            redirect_articles = direct_find(strip_from(article.redirect, '#'), True)
            if len(redirect_articles) == 0:
                break
            article = redirect_articles[0]
        if article.summary != None:
            articles.append(article)
    return articles

