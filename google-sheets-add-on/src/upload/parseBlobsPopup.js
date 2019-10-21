import * as classNames from "classnames";
import { postJson } from "../fetch";
import { Popup } from "./popup";

export class ParseBlobsPopup extends Popup {
    constructor(props) {
        super(props);
        this.state = {
            minBlobSize: 6,
        };
    }

    customClass() {
        return "parse-blobs-popup";
    }

    renderContent() {
        const { minBlobSize } = this.state;
        return (
            <div>
                <div className="center">Parse blobs</div>
                <div className="block">
                    <label
                        htmlFor="minBlobSize"
                        className="inline"
                    >
                        Minimum blob size (pixels):
                    </label>
                    <input
                        id="minBlobSize"
                        className="inline"
                        type="text"
                        style={{ width: "40px" }}
                        value={minBlobSize}
                        onChange={e => this.setState({ minBlobSize: parseInt(e.target.value) })}
                    />
                </div>
            </div>
        );
    }

    submit = () => {
        const { document, page, rectangle, setBlobs } = this.props;
        const { minBlobSize } = this.state;
        postJson({
            path: `/documents/${document.id}/blobs`,
            body: {
                section: { page, rectangle },
                minBlobSize,
            },
        }, blobs => {
            setBlobs(blobs);
            this.exit();
        });
    }
}
