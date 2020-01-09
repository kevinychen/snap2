import { postJson } from "../fetch";
import { Popup } from "./popup";

export class ParseCrosswordPopup extends Popup {
    constructor(props) {
        super(props);
        this.state = {
            hasLocalChanges: true,
            localChanges: "",
        };
    }

    customClass() {
        return "parse-crossword-popup";
    }

    renderContent() {
        const { hasLocalChanges, localChanges } = this.state;
        return (
            <div>
                <div className="block">
                    <div className="center">Enter crossword clues</div>
                    <textarea
                        style={{ fontFamily: hasLocalChanges ? "auto" : "monospace" }}
                        value={localChanges}
                        onChange={e => this.setState({ hasLocalChanges: true, localChanges: e.target.value })}
                    />
                </div>
            </div>
        );
    }

    renderSubmitSection() {
        return (
            <div className="submit-section">
                {this.renderSubmitButton("Parse")}
                <button onClick={this.exit}>Done</button>
            </div>
        );
    }

    submit = () => {
        const { setCrosswordClues } = this.props;
        const { localChanges } = this.state;
        postJson({
            path: `/words/parseCrosswordClues`,
            body: { unparsedClues: localChanges },
        }, response => {
            setCrosswordClues(response.clues);

            var cluesText = '';
            for (var section of response.clues.sections) {
                cluesText += section.direction + '\n';
                for (var clue of section.clues) {
                    cluesText += clue.clue + '\n';
                }
            }
            this.setState({ hasLocalChanges: false, localChanges: cluesText });
            this.finish();
        });
    }
}
