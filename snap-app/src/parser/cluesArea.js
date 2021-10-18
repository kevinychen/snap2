import React from "react";
import { postJson } from "../fetch";
import "./cluesArea.css";

export class CluesArea extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            hasLocalChanges: true,
            localChanges: "",
        };
        this.timeout = undefined;
    }

    render() {
        const { hasLocalChanges, localChanges } = this.state;
        return <div className="clues-area">
            <h3>Enter crossword clues</h3>
            <p>The format is flexible, so copying and pasting directly from the puzzle probably works.</p>
            <textarea
                style={{ fontFamily: hasLocalChanges ? "auto" : "monospace" }}
                value={localChanges}
                placeholder={"Across\n1. Clue\n2. Clue\n3. Clue\nDown\n1. Clue\n4. Clue\n7. Clue\n"}
                onChange={e => {
                    this.setState({ hasLocalChanges: true, localChanges: e.target.value });
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

    parse = () => {
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
        });
    }
}
