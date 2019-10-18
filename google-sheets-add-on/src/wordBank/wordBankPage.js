
export class WordBankPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            wordBankRange: "",
            usedWordsRange: "",
        };
    }

    render() {
        const { wordBankRange } = this.state;
        return (
            <div>
                <div className="block">
                    <h3>Highlight used words</h3>
                </div>
                <div className="block">
                    <div className="block">
                        Select the range of the word bank and click "Select".
                    </div>
                    <div className="block">
                        <button
                            className="blue"
                            onClick={this.setWordBankRange}
                        >
                            Select
                        </button>
                        <input
                            readOnly={true}
                            value={wordBankRange}
                        />
                    </div>
                </div>
                {this.maybeRenderUsedWordsRange()}
            </div>
        );
    }

    maybeRenderUsedWordsRange() {
        const { wordBankRange, usedWordsRange } = this.state;
        if (wordBankRange) {
            return (
                <div class="block">
                    <div class="block">
                        Select the range of used words and click "Select".
                        Snap will automatically highlight all words in the word bank that are present here.
                    </div>
                    <div class="block">
                        <button
                            className="blue"
                            onClick={this.setUsedWordsRange}
                        >
                            Select
                        </button>
                        <input
                            readonly={true}
                            value={usedWordsRange}
                        />
                    </div>
                </div>
            );
        }
    }

    setWordBankRange = () => {
        gs_getSelectedRangeA1Notation(range => this.setState({ wordBankRange: range }));
    }

    setUsedWordsRange = () => {
        gs_getSelectedRangeA1Notation(range => {
            this.setState({ usedWordsRange: range });
            gs_highlightUsed(this.state.wordBankRange, this.state.usedWordsRange);
        });
    }
}
