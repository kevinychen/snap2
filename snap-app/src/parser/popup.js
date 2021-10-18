import * as classNames from "classnames";
import React from "react";
import "./popup.css"

export class Popup extends React.Component {

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
                {this.renderSubmitButton("Submit")}
            </div>
        );
    }

    renderSubmitButton(text) {
        if (this.state.awaitingServer) {
            return (
                <button disabled={true}>
                    Please wait...
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
                    {text}
                </button>
            );
        }
    }

    exit = () => {
        const { exit } = this.props;
        this.finish();
        exit();
    }

    finish = () => this.setState({ awaitingServer: false });
}
