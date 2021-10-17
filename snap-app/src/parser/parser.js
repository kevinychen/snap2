import React from "react";
import { post, postJson } from "../fetch";
import { Document } from "./document";
import "./parser.css";

export default class Parser extends React.Component {
    
    constructor(props) {
        super(props);
        this.state = {
            url: '',
            loadingDocument: false,
            document: undefined,
        };
    }

    componentDidMount() {
        window.addEventListener("paste", this.pasteImage);
    }

    componentWillUnmount() {
        window.removeEventListener("paste", this.pasteImage);
    }

    render() {
        const { url, loadingDocument, document } = this.state;
        return <div className="parser">
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
            {document ? <Document document={document} /> : "or paste an image from clipboard."}
        </div>;
    }

    pasteImage = e => {
        const item = (e.clipboardData || e.originalEvent.clipboardData).items[0];
        if (item.kind === 'file') {
            this.setFile(item.getAsFile());
        }
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
}
