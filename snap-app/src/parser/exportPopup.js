import { postJson } from "../fetch";
import { Popup } from "./popup";

export class ExportPopup extends Popup {
    constructor(props) {
        super(props);
        this.state = {
            destination: {},
            marker: 'A1',
        };
    }

    customClass() {
        return "export-popup";
    }

    renderContent() {
        const { blobs } = this.props;
        const { url } = this.state;
        return (
            <div>
                <div className="center">{"Export to Google Sheets"}</div>
                <div className="block">
                    First, add <code>sheets-creator@snap-187301.iam.gserviceaccount.com</code> as an Editor to your Google document (or parent folder).
                </div>
                <div className="block">
                    <input
                        className="inline"
                        style={{ width: "100%" }}
                        type="text"
                        placeholder={`Enter URL of Google ${blobs !== undefined ? 'Sheet/Slide' : 'Sheet'}...`}
                        value={url}
                        onChange={this.setUrl}
                    />
                </div>
                {this.maybeRenderMarkerInput()}
            </div>
        );
    }

    maybeRenderMarkerInput() {
        const { destination, marker } = this.state;
        if (destination.spreadsheetId === undefined) {
            return;
        }
        return <div className="block">
            <label
                htmlFor="export-marker"
            >
                {"Export at: "}
            </label>
            <input
                id="export-marker"
                type="text"
                value={marker}
                onChange={e => this.setState({ marker: e.target.value })}
            />
        </div>;
    }

    setUrl = e => {
        const url = e.target.value;
        const sheetMatch = url.match(new RegExp('https://docs\\.google\\.com/spreadsheets/d/([A-Za-z0-9_-]+).*(?:gid=([0-9]+))'));
        const slideMatch = url.match(new RegExp('https://docs\\.google\\.com/presentation/d/([A-Za-z0-9_-]+).*(?:slide=id\\.([A-Za-z0-9_-]+))'));
        if (sheetMatch) {
            this.setState({ destination: { spreadsheetId: sheetMatch[1], sheetId: sheetMatch[2] } });
        } else if (slideMatch) {
            this.setState({ destination: { presentationId: slideMatch[1], slideId: slideMatch[2] } });
        } else {
            this.setState({ destination: {} });
        }
    }

    submit = () => {
        const { document, page, rectangle, blobs, gridLines, grid, crossword, crosswordClues } = this.props;
        const { destination, marker } = this.state;
        const { spreadsheetId, sheetId, presentationId, slideId } = destination;
        if (spreadsheetId !== undefined) {
            const markerMatch = marker.match(new RegExp('([A-Z]+)([1-9][0-9]*)'));
            if (markerMatch) {
                const row = parseInt(markerMatch[2]) - 1;
                let col = 0;
                for (let i = 0; i < markerMatch[1].length; i++) {
                    col = col * 26 + markerMatch[1].charCodeAt(i) - 64;
                }
                col--;
                postJson({
                    path: `/documents/${document.id}/export/sheet/${spreadsheetId}/${sheetId}`, body: {
                        section: { page, rectangle },
                        marker: { row, col },
                        blobs,
                        gridLines,
                        grid,
                        crossword,
                        crosswordClues,
                    }
                }, this.exit);
            } else {
                alert('Invalid export cell');
                this.finish();
            }
        } else if (presentationId !== undefined) {
            postJson({
                path: `/documents/${document.id}/export/slide/${presentationId}/${slideId}`, body: {
                    section: { page, rectangle },
                    blobs,
                }
            }, this.exit);
        } else {
            alert('Invalid URL.');
            this.finish();
        }
    }
}
