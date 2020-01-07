import { postJson } from "../fetch";
import { Popup } from "./popup";

export class ExportToSheetPopup extends Popup {
    constructor(props) {
        super(props);
        this.state = {
        };
    }

    customClass() {
        return "export-to-sheet-popup";
    }

    renderContent() {
        return (
            <div>
                <div className="center">Export to sheet</div>
                <div className="block">
                    <span className="inline">
                        Move your cursor to where you want to export, then click "Submit".
                    </span>
                </div>
            </div>
        );
    }

    submit = () => {
        const { document, page, rectangle, blobs, gridPosition, grid, crossword, crosswordClues } = this.props;
        gs_shareWithServer(() => {
            gs_getActiveCell(({ spreadsheetId, sheetId, row, col }) => {
                postJson({
                    path: `/documents/${document.id}/export/sheet/${spreadsheetId}/${sheetId}`,
                    body: {
                        marker: { row, col },
                        section: { page, rectangle },
                        blobs,
                        gridPosition,
                        grid,
                        crossword,
                        crosswordClues,
                    },
                }, this.exit);
            });
        });
    }
}
