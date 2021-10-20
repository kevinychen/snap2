import React from "react";
import "./cluesArea.css";

export class CluesArea extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            localChanges: undefined,
        };
        this.timeout = undefined;
    }

    render() {
        const { crosswordClues, crosswordCluesInferred } = this.props;
        const { localChanges } = this.state;
        return <div className="clues-area">
            <h3>Enter crossword clues</h3>
            {crosswordCluesInferred
                ? <p>Clues automatically found! Make any fixes if needed here.</p>
                : <p>The format is flexible, so copying and pasting directly from the puzzle probably works.</p>}
            <textarea
                style={{ fontFamily: localChanges !== undefined ? "auto" : "monospace" }}
                value={localChanges !== undefined ? localChanges : this.format(crosswordClues)}
                placeholder={"Across\n1. Clue\n2. Clue\n3. Clue\nDown\n1. Clue\n4. Clue\n7. Clue\n"}
                onChange={e => {
                    this.setState({ localChanges: e.target.value });
                    if (this.timeout !== undefined) {
                        clearTimeout(this.timeout);
                    }
                    this.timeout = setTimeout(() => {
                        this.parse();
                    }, 2000);
                }}
                onBlur={() => {
                    if (this.timeout !== undefined) {
                        clearTimeout(this.timeout);
                    }
                    this.parse();
                }}
            />
        </div>;
    }

    format(crosswordClues) {
        if (crosswordClues === undefined) {
            return '';
        }
        let cluesText = '';
        for (const section of crosswordClues.sections) {
            cluesText += section.direction + '\n';
            for (const clue of section.clues) {
                cluesText += clue.clue + '\n';
            }
        }
        return cluesText;
    }

    parse = () => {
        const { parseCrosswordClues } = this.props;
        const { localChanges } = this.state;
        if (localChanges !== undefined) {
            parseCrosswordClues(localChanges, () => this.setState({ localChanges: undefined }));
        }
    }
}
