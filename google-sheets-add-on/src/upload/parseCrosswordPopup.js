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
                    <p>Copy the clues below. The format is flexible, e.g. extraneous spaces, newlines, periods, etc. are OK.</p>
                    <textarea
                        style={{ fontFamily: hasLocalChanges ? "auto" : "monospace" }}
                        value={localChanges}
                        placeholder={"Across\n1. Clue\n2. Clue\n3. Clue\nDown\n1. Clue\n4. Clue\n7. Clue\n"}
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
