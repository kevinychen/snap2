function post(args, onSuccess, onError) {
    fetch('https://util.in/api' + args.path, {
        method: 'POST',
        body: args.body,
        headers: args.headers,
    }).then(response => {
        response.json().then(body => {
            if (response.ok) {
                if (onSuccess) {
                    onSuccess(body);
                }
            } else if (body.message) {
                (onError || alert)(body.message);
            } else {
                (onError || alert)('An unknown error occurred.');
            }
        });
    })
}

function postJson(args, onSuccess, onError) {
    post({
        path: args.path,
        body: JSON.stringify(args.body),
        headers: { 'Content-type': 'application/json' },
    }, onSuccess, onError);
}

class Snap extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            route: "upload", // TODO change to "home"
        };
    }

    render() {
        if (this.state.route === "home") {
            return (
                <div>
                    <input
                        className="block"
                        type="button"
                        onClick={() => this.setState({ route: "upload" })}
                        value="Upload from document..."
                    />
                </div>
            );
        } else {
            return (
                <div>
                    <input
                        className="block"
                        type="button"
                        onClick={() => this.setState({ route: "home" })}
                        value="< Home"
                    />
                    {this.renderPage()}
                </div>
            );
        }
    }

    renderPage() {
        if (this.state.route === "upload") {
            return <UploadPage />;
        }
    }
}

class UploadPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = { good: true };
    }

    componentDidMount() {
        this.checkGood();
        window.addEventListener("resize", () => this.checkGood());
    }

    render() {
        return this.state.good ? this.renderPage() : this.renderError();
    }

    checkGood() {
        this.setState({ good: (document.body.clientWidth >= 500) });
    }

    renderPage() {
        return (
            <div>
                <div>
                    <input
                        className="inline"
                        type="text"
                        placeholder="Enter URL of image/PDF..."
                        style={{ width: "300px" }}
                        onChange={e => this.setUrl(e.target.value)}
                    />
                    <span className="inline">or</span>
                    <input
                        className="inline"
                        type="file"
                        onChange={e => this.setFile(e.target.files[0])}
                    />
                </div>
                <div>
                    {JSON.stringify(this.state.document)}
                </div>
            </div>
        );
    }

    setUrl(url) {
        if (url) {
            postJson({ path: '/documents/url', body: { url } }, document => this.setState({ document }));
        }
    }

    setFile(file) {
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

ReactDOM.render(<Snap></Snap>, document.getElementById('app'));
