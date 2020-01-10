import * as classNames from "classnames";
import { postJson } from "../fetch";
import { Popup } from "./popup";

export class ParseContentPopup extends Popup {
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
        };
    }

    customClass() {
        return "parse-content-popup";
    }

    renderContent() {
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
            <div>
                <div className="center">Detect content</div>
                <div className="block">
                    <div className="inline">
                        <input
                            id="colors-setting"
                            type="checkbox"
                            checked={findColors}
                            onChange={() => this.setState({ findColors: !findColors })}
                        />
                        <label
                            htmlFor="colors-setting"
                            title="Parse the color of each grid square"
                        >
                            {"Colors"}
                        </label>
                    </div>
                    <div className="inline">
                        <input
                            id="borders-setting"
                            type="checkbox"
                            checked={findBorders}
                            onChange={() => this.setState({ findBorders: !findBorders })}
                        />
                        <label
                            htmlFor="borders-setting"
                            title="Parse borders between grid squares"
                        >
                            {"Borders"}
                        </label>
                    </div>
                </div>

                <div className="block">
                    <div className="inline">
                        <input
                            id="no-text-mode"
                            type="radio"
                            checked={findTextMode === "NONE"}
                            onChange={() => this.setState({ findTextMode: "NONE" })}
                        />
                        <label htmlFor="no-text-mode">
                            {"No text"}
                        </label>
                    </div>
                    <div className="inline">
                        <input
                            id="native-text-mode"
                            type="radio"
                            checked={findTextMode === "USE_NATIVE"}
                            onChange={() => this.setState({ findTextMode: "USE_NATIVE" })}
                        />
                        <label htmlFor="native-text-mode">
                            {"Stored text"}
                        </label>
                    </div>
                    <div className="inline">
                        <input
                            id="ocr-text-mode"
                            type="radio"
                            checked={findTextMode === "USE_OCR"}
                            onChange={() => this.setState({ findTextMode: "USE_OCR" })}
                        />
                        <label htmlFor="ocr-text-mode">
                            {"Infer text with OCR"}
                        </label>
                    </div>
                </div>

                <div className={classNames("block", { "grayed-out": findTextMode !== "USE_OCR" })}>
                    <div className="block">
                        <div className="inline">
                            <input
                                id="ocr-single-character-setting"
                                type="checkbox"
                                checked={ocrSingleCharacter}
                                onChange={() => this.setState({ ocrSingleCharacter: !ocrSingleCharacter })}
                            />
                            <label htmlFor="ocr-single-character-setting">
                                {"Single character per square"}
                            </label>
                        </div>
                    </div>
                    <div className="block">
                        <div>Allowed characters</div>
                        <div>
                            <div className="inline">
                                <input
                                    id="ocr-no-allowed-characters-mode"
                                    type="radio"
                                    checked={ocrAllowedCharactersMode === "NONE"}
                                    onChange={() => this.setState({ ocrAllowedCharactersMode: "NONE" })}
                                />
                                <label htmlFor="ocr-no-allowed-characters-mode">
                                    {"All"}
                                </label>
                            </div>
                            <div className="inline">
                                <input
                                    id="ocr-digits-allowed-characters-mode"
                                    type="radio"
                                    checked={ocrAllowedCharactersMode === "DIGITS"}
                                    onChange={() => this.setState({ ocrAllowedCharactersMode: "DIGITS" })}
                                />
                                <label htmlFor="ocr-digits-allowed-characters-mode">
                                    {"123"}
                                </label>
                            </div>
                            <div className="inline">
                                <input
                                    id="ocr-uppercase-allowed-characters-mode"
                                    type="radio"
                                    checked={ocrAllowedCharactersMode === "UPPERCASE"}
                                    onChange={() => this.setState({ ocrAllowedCharactersMode: "UPPERCASE" })}
                                />
                                <label htmlFor="ocr-uppercase-allowed-characters-mode">
                                    {"ABC"}
                                </label>
                            </div>
                            <div className="inline">
                                <input
                                    id="ocr-custom-allowed-characters-mode"
                                    type="radio"
                                    checked={ocrAllowedCharactersMode === "CUSTOM"}
                                    onChange={() => this.setState({ ocrAllowedCharactersMode: "CUSTOM" })}
                                />
                                <label htmlFor="ocr-custom-allowed-characters-mode">
                                    {"Custom:"}
                                </label>
                            </div>
                            <input
                                className={classNames("inline", { "grayed-out": ocrAllowedCharactersMode !== "CUSTOM" })}
                                type="text"
                                style={{ width: "120px" }}
                                value={ocrAllowedCharacters}
                                onChange={e => this.setState({ ocrAllowedCharacters: e.target.value })}
                            />
                        </div>
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
            </div>
        );
    }

    submit = () => {
        const { document, page, rectangle, gridLines, setGrid } = this.props;
        const {
            findColors,
            findBorders,
            findTextMode,
            ocrSingleCharacter,
            ocrFullness,
            ocrConfidenceThreshold,
        } = this.state;
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
            setGrid(response);
            this.exit();
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
