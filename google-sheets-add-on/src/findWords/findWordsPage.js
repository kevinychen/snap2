import { postJson } from "../fetch";

export class FindWordsPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            letterBankRange: "",
            letterBankValues: undefined,
            wordLengths: "",
            isAnagramMode: false,
            words: undefined,
        };
    }

    render() {
        const { wordLengths, isAnagramMode } = this.state;
        return (
            <div>
                <div className="block">
                    <h3>Find words</h3>
                </div>
                <div className="block">
                    <div className="block">
                        Select the range of letters to use and click "Select".
                    </div>
                    <div className="block">
                        <input
                            className="inline"
                            type="text"
                            value={wordLengths}
                            onChange={e => this.setState({ wordLengths: e.target.value })}
                        />
                        <span className="inline">
                            {"Word lengths (leave empty if unknown)"}
                        </span>
                    </div>
                    <div className="block">
                        <input
                            id="anagram-mode"
                            type="checkbox"
                            checked={isAnagramMode}
                            onChange={() => this.setState({ isAnagramMode: !isAnagramMode, words: undefined })}
                        />
                        <label
                            htmlFor="anagram-mode"
                        >
                            {"Anagram mode (use all letters in a cell, but in any order)"}
                        </label>
                    </div>
                    <div className="block">
                        <button
                            className="blue"
                            onClick={this.findWords}
                        >
                            Select
                        </button>
                    </div>
                </div>
                {this.maybeRenderWordsToPaste()}
            </div>
        );
    }

    maybeRenderWordsToPaste() {
        const { words } = this.state;
        if (words === undefined) {
            return undefined;
        }
        return (
            <div className="block">
                <h3>Results</h3>
                {words ? words.map((word, index) => this.renderPasteButton(word, index)) : "No words found"}
            </div>
        );
    }

    renderPasteButton(word, index) {
        return (
            <div className="block" key={"word-" + index}>
                <span className="inline">{word}</span>
                <button className="inline" onClick={() => this.pasteWord(word, false)}>
                    {"Paste"}
                </button>
                {this.maybeRenderCutAndPasteButton(word)}
            </div>
        );
    }

    maybeRenderCutAndPasteButton(word) {
        const { letterBankValues } = this.state;
        if (letterBankValues) {
            return (
                <button className="inline" onClick={() => this.pasteWord(word, true)}>
                    {"Cut & Paste"}
                </button>
            );
        }
    }

    findWords = () => {
        const { wordLengths, isAnagramMode } = this.state;
        gs_getSelectedRange(({ rangeA1, values }) => {
            this.setState({ letterBankRange: rangeA1, letterBankValues: values });
            var parts = [];
            for(var row of values) {
            for (var val of row) {
                val = val.replace(/[^A-Za-z]/g, '');
                if (val === '') {
                    parts.push('.');
                } else if (isAnagramMode) {
                    parts.push(val);
                } else {
                    parts.push('[' + val + ']');
                }
            }
        }
        postJson({
            path: '/words/cromulence',
            body: {
                parts,
                canRearrange: isAnagramMode,
                wordLengths: wordLengths.length > 0 ? wordLengths.trim().split(/[^0-9]+/) : undefined,
            },
        }, response => {
            this.setState({ words: response.results.map(result => result.words.join(' ')) })
        });
    });
    }

    pasteWord = (word, removeLetters) => {
        // Paste the word in the currently selected range. If the range is multiple cells, the letters will be divided up roughly equally among the cells.
        // (In particular, if the number of cells is equal to the number of letters in the word, then one letter will be pasted in each cell.)
        // (The word may have spaces, but they will be removed if the current range contains more than one cell.)
        //
        // If letterBankRange is defined, then the letters in the word will also be removed from that range.

        const { letterBankRange, letterBankValues, isAnagramMode } = this.state;
        var wordWithoutSpaces = word.replace(/ /g, '');
        gs_getSelectedRange(({ rangeA1, values }) => {
            var rangeSize = values.length * values[0].length;
            if (rangeSize == 1) {
                values[0][0] = word;
            } else {
                for (var i = 0; i < values.length; i++) {
                    for (var j = 0; j < values[0].length; j++) {
                        var index = i * values[0].length + j;
                        values[i][j] = wordWithoutSpaces.substring(
                            Math.floor(index * wordWithoutSpaces.length / rangeSize),
                            Math.floor((index + 1) * wordWithoutSpaces.length / rangeSize));
                    }
                }
            }
            gs_setValues(rangeA1, values);
        });
        if (removeLetters) {
            if (isAnagramMode) {
                while (true) {
                    var found = false;
                    for (var i = 0; i < letterBankValues.length; i++) {
                        for (var j = 0; j < letterBankValues[0].length; j++) {
                            var val = letterBankValues[i][j].toUpperCase();
                            if (val !== '' && wordWithoutSpaces.startsWith(val)) {
                                found = true;
                                wordWithoutSpaces = wordWithoutSpaces.substring(val.length);
                                letterBankValues[i][j] = '';
                            }
                        }
                    }
                    if (!found) {
                        break;
                    }
                }
            } else {
                for (var i = 0; i < letterBankValues.length; i++) {
                    for (var j = 0; j < letterBankValues[0].length; j++) {
                        var index = i * letterBankValues[0].length + j;
                        letterBankValues[i][j] = letterBankValues[i][j].replace(new RegExp(wordWithoutSpaces[index], 'i'), '');
                    }
                }
            }
            gs_setValues(letterBankRange, letterBankValues);
            this.setState({ letterBankValues: undefined });
        }
    }
}
