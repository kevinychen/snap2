import { postJson } from "../fetch";
import { Popup } from "./popup";

export class ParseGridLinesPopup extends Popup {
    constructor(props) {
        super(props);
        this.state = {
            mode: "EXPLICIT",
            interpolate: false,
            approxGridSquareSize: 32,
        };
    }

    customClass() {
        return "parse-grid-lines-popup";
    }

    renderContent() {
        const { mode, interpolate, approxGridSquareSize } = this.state;
        return (
            <div>
                <div className="center">Find grid lines</div>
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
                    <span className="inline">Approximate square size (in pixels):</span>
                    <input
                        className="inline"
                        type="text"
                        style={{ width: "40px" }}
                        value={approxGridSquareSize}
                        onChange={e => this.setState({ approxGridSquareSize: parseInt(e.target.value) })}
                    />
                </div>
            </div>
        );
    }

    submit = () => {
        const { document, page, rectangle, setGridLines } = this.props;
        const { mode, interpolate, approxGridSquareSize } = this.state;
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
