import * as classNames from "classnames";
import { postJson } from "../fetch";

export class ParseContentPopup extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            findColors: true,
            findBorders: false,
            findTextMode: "NONE",
            ocrAllowedCharactersMode: "NONE",
            ocrAllowedCharacters: "",
            ocrSingleCharacter: false,
            ocrFullness: 0.8,
            ocrConfidenceThreshold: 50,
            awaitingServer: false,
        };
    }

    render() {
        const {
            findColors,
            findBorders,
            findTextMode,
            ocrAllowedCharactersMode,
            ocrAllowedCharacters,
            ocrSingleCharacter,
            ocrFullness,
            ocrConfidenceThreshold,
        } = this.state;

        return (
            <div
                className={classNames( "popup", {"hide": !this.props.isVisible })}
                style={{ width: "350px", height: "400px" }}
            >
                <div className="block">
                    <button
                        className={classNames({ "green": findColors })}
                        title="Parse the color of each grid square"
                        onClick={() => this.setState({ findColors: !findColors })}
                    >
                        {"Colors"}
                    </button>
                    <button
                        className={classNames({ "green": findBorders })}
                        title="Parse borders between grid squares"
                        onClick={() => this.setState({ findBorders: !findBorders })}
                    >
                        {"Borders"}
                    </button>
                </div>

                <div className="block">
                    <button
                        className={classNames({ "green": findTextMode === "NONE" })}
                        onClick={() => this.setState({ findTextMode: "NONE" })}
                    >
                        {"No text"}
                    </button>
                    <button
                        className={classNames({ "green": findTextMode === "USE_NATIVE" })}
                        title="Text is present in the document (e.g. textual PDFs)"
                        onClick={() => this.setState({ findTextMode: "USE_NATIVE" })}
                    >
                        {"Stored text"}
                    </button>
                    <button
                        className={classNames({ "green": findTextMode === "USE_OCR" })}
                        onClick={() => this.setState({ findTextMode: "USE_OCR" })}
                    >
                        {"Infer text with OCR"}
                    </button>
                </div>

                <div className={classNames("block", { "grayed-out": findTextMode !== "USE_OCR" })}>
                    <div className="block">
                        <button
                            className={classNames({ "green": ocrSingleCharacter })}
                            onClick={() => this.setState({ ocrSingleCharacter: !ocrSingleCharacter })}
                        >
                            {"Single character per square"}
                        </button>
                    </div>
                    <div className="block">
                        <div>Allowed characters</div>
                        <button
                            className={classNames("small-button", { "green": ocrAllowedCharactersMode === "NONE" })}
                            onClick={() => this.setState({ ocrAllowedCharactersMode: "NONE" })}
                        >
                            {"All"}
                        </button>
                        <button
                            className={classNames("small-button", { "green": ocrAllowedCharactersMode === "DIGITS" })}
                            onClick={() => this.setState({ ocrAllowedCharactersMode: "DIGITS" })}
                        >
                            {"123"}
                        </button>
                        <button
                            className={classNames("small-button", { "green": ocrAllowedCharactersMode === "UPPERCASE" })}
                            onClick={() => this.setState({ ocrAllowedCharactersMode: "UPPERCASE" })}
                        >
                            {"ABC"}
                        </button>
                        <button
                            className={classNames({ "green": ocrAllowedCharactersMode === "CUSTOM" })}
                            onClick={() => this.setState({ ocrAllowedCharactersMode: "CUSTOM" })}
                        >
                            {"Custom"}
                        </button>
                        <input
                            className={classNames("inline", { "grayed-out": ocrAllowedCharactersMode !== "CUSTOM" })}
                            type="text"
                            style={{ width: "120px" }}
                            value={ocrAllowedCharacters}
                            onChange={e => this.setState({ ocrAllowedCharacters: e.target.value })}
                        />
                    </div>
                    <div className="block">
                        <input
                            className="inline"
                            type="text"
                            style={{ width: "40px" }}
                            value={ocrFullness}
                            onChange={e => this.setState({ ocrFullness: parseFloat(e.target.value) })}
                        />
                        <span className="inline">0 ≤ Fullness ≤ 1</span>
                        <div>
                            (this value should estimate the percentage of each square's width and height not covered by borders/embellishment, and greatly improves character recognition)
                        </div>
                    </div>
                    <div className="block">
                        <input
                            className="inline"
                            type="text"
                            style={{ width: "40px" }}
                            value={ocrConfidenceThreshold}
                            onChange={e => this.setState({ ocrConfidenceThreshold: parseInt(e.target.value) })}
                        />
                        <span className="inline">0 ≤ Confidence threshold ≤ 100</span>
                        <div>
                            (how much confidence is required for a character to be recognized; the higher, the stricter the character recognition)
                        </div>
                    </div>
                </div>

                {this.renderSubmitButton()}
            </div>
        );
    }

    renderSubmitButton() {
        if (this.state.awaitingServer) {
            return (
                <button
                    className="submit-button"
                    disabled={true}
                >
                    {"Calculating..."}
                </button>
            );
        } else {
            return (
                <button
                    className={classNames("submit-button", "blue")}
                    onClick={this.parseContent}
                >
                    {"Parse content"}
                </button>
            );
        }
    }

    parseContent = () => {
        const { document, page, rectangle, gridLines, setGrid } = this.props;
        const {
            findColors,
            findBorders,
            findTextMode,
            ocrSingleCharacter,
            ocrFullness,
            ocrConfidenceThreshold,
        } = this.state;
        this.setState({ awaitingServer: true });
        postJson({
            path: `/documents/${document.id}/grid`,
            body: {
                section: { page, rectangle },
                gridLines,
                findColors,
                findBorders,
                findTextMode,
                ocrOptions: {
                    allowedCharacters: this.getOcrAllowedCharacters(),
                    singleCharacter: ocrSingleCharacter,
                    fullness: ocrFullness,
                    confidenceThreshold: ocrConfidenceThreshold,
                },
            }
        }, response => {
            this.setState({ awaitingServer: false });
            setGrid(response);
        });
    }

    getOcrAllowedCharacters() {
        const { ocrAllowedCharactersMode, ocrAllowedCharacters } = this.state;
        if (ocrAllowedCharactersMode === "NONE") {
            return undefined;
        } else if (ocrAllowedCharactersMode === "DIGITS") {
            return "0123456789";
        } else if (ocrAllowedCharactersMode === "UPPERCASE") {
            return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        } else if (ocrAllowedCharactersMode === "CUSTOM") {
            return ocrAllowedCharacters;
        }
    }
}
