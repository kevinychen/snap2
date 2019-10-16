import * as classNames from "classnames";
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
        const { document, page, rectangle, gridPosition, grid } = this.props;
        gs_getActiveCell(({ spreadsheetId, sheetId, row, col }) => {
            postJson({
                path: `/documents/${document.id}/export/sheet/${spreadsheetId}/${sheetId}`,
                body: {
                    marker: { row, col },
                    section: { page, rectangle },
                    gridPosition,
                    grid,
                },
            }, this.exit);
        });
    }
}
