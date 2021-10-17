import React from "react";
import "./dropdownMenu.css"

export class ExportButton extends React.Component {
    
    constructor(props) {
        super(props);
        this.state = {
            numExporting: 0,
            tempDisabled: false,
        };
    }

    render() {
        const { numExporting, tempDisabled } = this.state;
        return (
            <button
                className="export-button"
                onClick={() => this.export()}
                disabled={tempDisabled}
            >
                Export {this.currentType()} at cursor
                        {numExporting > 0 ? ` (${numExporting} in progress)` : ""}
            </button>
        )
    }

    currentType() {
        const { blobs, gridLines, grid, crossword, crosswordClues } = this.props;
        if (gridLines && grid) {
            if (crossword && crosswordClues) {
                return "crossword";
            } else {
                return "grid";
            }
        } else if (blobs) {
            return "blobs";
        } else if (gridLines && gridLines.horizontalLines.length * gridLines.verticalLines.length > 4) {
            return "images";
        } else {
            return "image";
        }
    }

    export = () => {
        const { numExporting } = this.state;
        this.setState({ numExporting: numExporting + 1, tempDisabled: true });
        setTimeout(() => this.setState({ tempDisabled: false }), 1500);
        // TODO
    }
}
