import * as classNames from "classnames";
import { postJson } from "../fetch";

export class FindGridLinesPopup extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            mode: "EXPLICIT",
            interpolate: false,
            approxGridSquareSize: 32,
            awaitingServer: false,
        };
    }

    render() {
        const { mode, interpolate, approxGridSquareSize } = this.state;
        return (
            <div
                className={classNames( "popup", {"hide": !this.props.isVisible })}
                style={{ width: "250px", height: "175px" }}
            >
                <div className="center">Find grid lines</div>
                <div className="block">
                    <button
                        className={classNames({ "green": mode === "EXPLICIT" })}
                        title="Look for explicit lines in the image"
                        onClick={() => this.setState({ mode: "EXPLICIT" })}
                    >
                        {"Explicit"}
                    </button>
                    <button
                        className={classNames({ "green": mode === "IMPLICIT" })}
                        title="Look for whitespace in the image as implicit lines"
                        onClick={() => this.setState({ mode: "IMPLICIT" })}
                    >
                        {"Implicit"}
                    </button>
                    <button
                        className={classNames({ "green": interpolate })}
                        title="Ensure that all grid lines are evenly spaced"
                        onClick={() => this.setState({ interpolate: !interpolate })}
                    >
                        {"Interpolate"}
                    </button>
                </div>

                <div className="block">
                    <span className="inline">Approximate grid size (in pixels):</span>
                    <input
                        className="inline"
                        type="text"
                        style={{ width: "40px" }}
                        value={approxGridSquareSize}
                        onChange={e => this.setState({ approxGridSquareSize: parseInt(e.target.value) })}
                    />
                </div>

                <div className="submit-section">
                    <button onClick={this.cancel}>Cancel</button>
                    {this.renderSubmitButton()}
                </div>
            </div>
        );
    }

    renderSubmitButton() {
        if (this.state.awaitingServer) {
            return (
                <button disabled={true}>
                    Calculating...
                </button>
            );
        } else {
            return (
                <button className="blue" onClick={this.findGridLines}>
                    Submit
                </button>
            );
        }
    }

    cancel = () => {
        const { cancel } = this.props;
        this.setState({ awaitingServer: false });
        cancel();
    }

    findGridLines = () => {
        const { document, page, rectangle, setGridLines } = this.props;
        const { mode, interpolate, approxGridSquareSize } = this.state;
        this.setState({ awaitingServer: true });
        postJson({
            path: `/documents/${document.id}/lines`,
            body: {
                section: { page, rectangle },
                findGridLinesMode: mode,
                interpolate,
                approxGridSquareSize,
            }
        }, gridLines => {
            this.setState({ awaitingServer: false });
            setGridLines(gridLines);
        });
    }
}
