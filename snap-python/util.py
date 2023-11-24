import csv
import os
import re
import subprocess

# OPERATING SYSTEM

def data_file(path):
    return os.path.join(os.path.dirname(__file__), '../snap-server/data', path)

def call(*commands):
    """ Runs the command in the console.

    >>> call('echo', 'hello')
    'hello\n'
    """
    p = subprocess.Popen(commands, stdout=subprocess.PIPE)
    return p.communicate()[0]

# DATA FILES

def words():
    """ Returns a generator of all English words in lowercase.

    >>> words().next()
    'the'
    """
    with open(data_file('count_1w.txt')) as fh:
        tsv = csv.reader(fh, delimiter = '\t')
        for row in tsv:
            yield row[0]

def wiki():
    """ Returns a generator of all Wikipedia article titles with lowercase letters and digits separated by spaces.
    """
    with open(data_file('wikipedia-titles')) as fh:
        for line in fh:
            yield re.sub('[^a-z]+', ' ', line.lower()).strip()

# STRING MANIPULATION

def anagram(s, dict=words):
    """ Returns a list of all English anagrams of the given string.

    >>> anagram('aaagmnr')
    ['anagram', 'mangara']
    """
    s = s.lower()
    matches = []
    for word in dict():
        if len(word) == len(s) and sorted(word) == sorted(s):
            matches.append(word)
    return matches

def caesar(s):
    for i in range(26):
        shift = ''
        for c in s:
            if c >= 'A' and c <= 'Z':
                shift += chr((ord(c) - 65 + i) % 26 + 65)
            elif c >= 'a' and c <= 'z':
                shift += chr((ord(c) - 97 + i) % 26 + 97)
            else:
                shift += c
        print(shift)

def match(pattern, dict=words):
    """ Returns a list of all English words that match the given pattern.

    >>> match('m*t*h')
    ['match', 'mitch', 'mutch', 'mutoh', 'matth']
    >>> match('lmp ab t * h')
    ['match', 'patch', 'latch', 'latah', 'matth']
    """
    pattern = pattern.lower().split()
    if len(pattern) == 1:
        pattern = pattern[0]
    def fit(s):
        for c, cSet in zip(s, pattern):
            if cSet != '*' and c not in cSet:
                return False
        return True
    matches = list()
    for word in dict():
        if len(word) == len(pattern) and fit(word.lower()):
            matches.append(word)
    return matches

def subseq(letters, subseq, ordered=True):
    """ Returns whether the letters contains the given subsequence.

    >>> subseq('snake', 'sake')
    True
    >>> subseq('snake', 'sea')
    False
    >>> subseq('snake', 'sea', ordered=False)
    True
    """
    for c in subseq:
        if c not in letters:
            return False
        index = letters.index(c)
        if ordered:
            letters = letters[index+1:]
        else:
            letters = letters[:index] + letters[index+1:]
    return True

# NUMBER THEORY

def find_factor(n):
    """ Returns a positive integer >= 2 that divides n.

    >>> find_factor(45)
    3
    """
    if n % 2 == 0:
        return 2
    d = 3
    while d * d <= n:
        if n % d == 0:
            return d
        d += 2
    return n

def is_prime(n):
    return find_factor(n) == n

def factor(n):
    """ Returns a list of all factors of n.

    >>> factor(45)
    [3, 3, 5]
    """
    l = []
    while n > 1:
        d = find_factor(n)
        l.append(d)
        n //= d
    return l

def gcd(a, b):
    return a if b == 0 else gcd(b, a % b)

def nCr(a, b):
    if b < 0:
        return 0
    n = 1
    for i in range(b):
        n *= a - i
        n //= i + 1
    return n
