
export class AutoMatchPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            referenceRange: "",
            matchRange: "",
        };
    }

    render() {
        const { referenceRange } = this.state;
        return (
            <div>
                <div className="block">
                    <h3>Auto match</h3>
                </div>
                <div className="block">
                    <div className="block">
                        Select the range of the reference column and click "Select".
                        All values in the reference column must be distinct.
                    </div>
                    <div className="block">
                        <button
                            className="blue"
                            onClick={this.setReferenceRange}
                        >
                            Select
                        </button>
                        <input
                            readOnly={true}
                            value={referenceRange}
                        />
                    </div>
                </div>
                {this.maybeRenderMatchRange()}
            </div>
        );
    }

    maybeRenderMatchRange() {
        const { referenceRange, matchRange } = this.state;
        if (referenceRange) {
            return (
                <div className="block">
                    <div className="block">
                        Select the range of the columns to reorder and click "Select".
                        Snap will reorder these columns such that the first value in each row matches the value in the reference column.
                    </div>
                    <div className="block">
                        <button
                            className="blue"
                            onClick={this.setMatchRange}
                        >
                            Select
                        </button>
                        <input
                            readOnly={true}
                            value={matchRange}
                        />
                    </div>
                </div>
            );
        }
    }

    setReferenceRange = () => {
        gs_getSelectedRangeA1Notation(range => this.setState({ referenceRange: range }));
    }

    setMatchRange = () => {
        const { referenceRange } = this.state;
        gs_getSelectedRangeA1Notation(range => {
            this.setState({ matchRange: range });
            gs_autoMatch(referenceRange, range);
        });
    }
}
