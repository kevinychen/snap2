import { getJson, post, postJson } from "../fetch";
import { Document } from "./document";

export class UploadPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            good: true,
            document: undefined,
        };
    }

    componentDidMount() {
        this.checkGood();
        window.addEventListener("resize", this.checkGood);

        // TODO temporary for testing
        const documentId = "73453f56-0590-44ce-93be-47fc06f4b014";
        getJson({ path: `/documents/${documentId}` }, document => this.setState({ document }));
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.checkGood);
    }

    render() {
        return this.state.good ? this.renderPage() : this.renderError();
    }

    checkGood = () => {
        this.setState({ good: (document.body.clientWidth >= 500) });
    }

    renderPage() {
        const { document } = this.state;
        return (
            <div className="block">
                <div className="block">
                    <input
                        className="inline"
                        type="text"
                        placeholder="Enter URL of image/PDF..."
                        style={{ width: "300px" }}
                        onChange={this.setUrl}
                    />
                    <span className="inline">or</span>
                    <input
                        className="inline"
                        type="file"
                        onChange={this.setFile}
                    />
                </div>
                {document ? <Document document={document} /> : undefined}
            </div>
        );
    }

    setUrl = e => {
        const url = e.target.value;
        if (url) {
            postJson({ path: '/documents/url', body: { url } }, document => this.setState({ document }));
        }
    }

    setFile = e => {
        const file = e.target.files[0];
        if (file) {
            const formData = new FormData();
            if (file.type === "application/pdf") {
                formData.append('pdf', file);
                post({ path: '/documents/pdf', body: formData }, document => this.setState({ document }));
            } else if (file.type.startsWith("image/")) {
                formData.append('image', file);
                post({ path: '/documents/image', body: formData }, document => this.setState({ document }));
            }
        }
    }

    renderError() {
        const errorText = "$('.script-application-sidebar').style.width = '700px'";
        window.focus();
        navigator.clipboard.writeText(errorText);
        return (
            <div className="block">
                <p><b>The sidebar must be wider to use this tool.</b></p>
                <ul>
                    <li>Open developer tools with Ctrl+Shift+J (Cmd+Option+J on Mac)</li>
                    <li>Paste the following (already copied to clipboard):</li>
                </ul>
                <code id="widen-sidebar-command">{errorText}</code>
            </div>
        );
    }
}
