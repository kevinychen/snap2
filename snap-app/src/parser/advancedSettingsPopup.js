import { Popup } from "./popup";

export class AdvancedSettingsPopup extends Popup {
    constructor(props) {
        super(props);
        this.state = {};
    }

    customClass() {
        return "advanced-settings-popup";
    }

    renderContent() {
        const { setAdvancedSetting, findGridLinesMode, interpolateSetting } = this.props;
        return (
            <div>
                <div className="center">Advanced settings</div>
                <div className="block">
                    <div className="inline">
                        <input
                            id="explicit-mode"
                            type="radio"
                            checked={findGridLinesMode === "EXPLICIT"}
                            onChange={() => setAdvancedSetting("findGridLinesMode", "EXPLICIT")}
                        />
                        <label
                            htmlFor="explicit-mode"
                            title="Look for explicit lines in the image"
                        >
                            {"Explicit grid lines"}
                        </label>
                    </div>
                    <div className="inline">
                        <input
                            id="implicit-mode"
                            type="radio"
                            checked={findGridLinesMode === "IMPLICIT"}
                            onChange={() => setAdvancedSetting("findGridLinesMode", "IMPLICIT")}
                        />
                        <label
                            htmlFor="implicit-mode"
                            title="Look for whitespace in the image as implicit lines"
                        >
                            {"Implicit grid lines"}
                        </label>
                    </div>
                </div>
                <div className="block">
                    <div className="inline">
                        <input
                            id="interpolate-setting"
                            type="checkbox"
                            checked={interpolateSetting}
                            onChange={() => setAdvancedSetting("interpolateSetting", !interpolateSetting)}
                        />
                        <label
                            htmlFor="interpolate-setting"
                        >
                            {"Ensure grid is evenly spaced"}
                        </label>
                    </div>
                </div>
            </div>
        );
    }

    renderSubmitSection() {
        return (
            <div className="submit-section">
                <button onClick={this.exit}>Done</button>
            </div>
        );
    }

    submit = () => this.exit();
}
