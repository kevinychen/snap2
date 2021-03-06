import "./uploadPage.css";
import { post, postJson } from "../fetch";
import { Document } from "./document";

export class UploadPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            good: true,
            url: '',
            loadingDocument: false,
            document: undefined,
        };
    }

    componentDidMount() {
        this.checkGood();
        window.addEventListener("resize", this.checkGood);
        window.addEventListener("paste", this.pasteImage);
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.checkGood);
        window.removeEventListener("paste", this.pasteImage);
    }

    render() {
        return this.state.good ? this.renderPage() : this.renderError();
    }

    checkGood = () => {
        this.setState({ good: (document.body.clientWidth >= 500) });
    }

    pasteImage = e => {
        const item = (e.clipboardData || e.originalEvent.clipboardData).items[0];
        if (item.kind === 'file') {
            this.setFile(item.getAsFile());
        }
    }

    renderPage() {
        const { url, loadingDocument, document } = this.state;
        return (
            <div className="block">
                <div className="block">
                    <input
                        className="inline"
                        type="text"
                        placeholder="Enter URL of image/PDF/HTML..."
                        style={{ width: "300px" }}
                        value={url}
                        onKeyUp={this.setUrl}
                        onChange={this.setUrl}
                    />
                    <span className="inline">or</span>
                    <input
                        className="inline"
                        type="file"
                        onChange={e => this.setFile(e.target.files[0])}
                    />
                    {loadingDocument ? <span className="loading"></span> : undefined}
                </div>
                {document ? <Document document={document} /> : undefined}
            </div>
        );
    }

    setUrl = e => {
        const url = e.target.value;
        if (url && url !== this.state.url) {
            this.setState({ url, loadingDocument: true });
            postJson({ path: '/documents/url', body: { url } }, document => this.setState({ loadingDocument: false, document }));
        }
    }

    setFile = file => {
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
