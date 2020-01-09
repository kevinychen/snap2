import { postJson } from "../fetch";
import { Popup } from "./popup";

export class ParseGridLinesPopup extends Popup {
    constructor(props) {
        super(props);
        this.state = {
            mode: "EXPLICIT",
            interpolate: false,
            approxGridSize: 12,
        };
    }

    customClass() {
        return "parse-grid-lines-popup";
    }

    renderContent() {
        const { mode, interpolate, approxGridSize } = this.state;
        return (
            <div>
                <div className="center">Detect grid lines</div>
                <div className="block">
                    <div className="inline">
                        <input
                            id="explicit-mode"
                            type="radio"
                            checked={mode === "EXPLICIT"}
                            onChange={() => this.setState({ mode: "EXPLICIT" })}
                        />
                        <label
                            htmlFor="explicit-mode"
                            title="Look for explicit lines in the image"
                        >
                            {"Explicit"}
                        </label>
                    </div>
                    <div className="inline">
                        <input
                            id="implicit-mode"
                            type="radio"
                            checked={mode === "IMPLICIT"}
                            onChange={() => this.setState({ mode: "IMPLICIT" })}
                        />
                        <label
                            htmlFor="implicit-mode"
                            title="Look for whitespace in the image as implicit lines"
                        >
                            {"Implicit"}
                        </label>
                    </div>
                </div>

                <div className="block">
                    <div className="inline">
                        <input
                            id="interpolate-setting"
                            type="checkbox"
                            checked={interpolate}
                            onChange={() => this.setState({ interpolate: !interpolate })}
                        />
                        <label
                            htmlFor="interpolate-setting"
                            title="Ensure that all grid lines are evenly spaced"
                        >
                            {"Interpolate"}
                        </label>
                    </div>
                </div>

                <div className="block">
                    <span className="inline">Approximate number of rows or columns:</span>
                    <input
                        className="inline"
                        type="text"
                        style={{ width: "40px" }}
                        value={approxGridSize}
                        onChange={e => this.setState({ approxGridSize: parseInt(e.target.value) })}
                    />
                </div>
            </div>
        );
    }

    submit = () => {
        const { document, page, rectangle, setGridLines } = this.props;
        const { mode, interpolate, approxGridSize } = this.state;
        const approxGridSquareSize = Math.round(Math.min(
            (rectangle.width + rectangle.height) / (2 * approxGridSize),
            rectangle.width / 6,
            rectangle.height / 6));
        postJson({
            path: `/documents/${document.id}/lines`,
            body: {
                section: { page, rectangle },
                findGridLinesMode: mode,
                interpolate,
                approxGridSquareSize,
            }
        }, gridLines => {
            setGridLines(gridLines);
            this.exit();
        });
    }
}
