import "./popup.css"
import * as classNames from "classnames";

export class Popup extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div
                className={classNames("popup", this.customClass(), { "hide": !this.props.isVisible })}
            >
                {this.renderContent()}
                {this.renderSubmitSection()}
            </div>
        );
    }

    renderSubmitSection() {
        return (
            <div className="submit-section">
                <button onClick={this.exit}>Cancel</button>
                {this.renderSubmitButton()}
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
                <button
                    className="blue"
                    onClick={() => {
                        this.setState({ awaitingServer: true });
                        this.submit();
                    }}
                >
                    Submit
                </button>
            );
        }
    }

    exit = () => {
        const { exit } = this.props;
        this.setState({ awaitingServer: false });
        exit();
    }
}
