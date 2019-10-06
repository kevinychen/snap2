import { UploadPage } from "./upload/uploadPage";

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
                    <button
                        className="block"
                        onClick={() => this.setState({ route: "upload" })}
                    >
                        Upload from document...
                    </button>
                </div>
            );
        } else {
            return (
                <div>
                    <button
                        className="block"
                        onClick={() => this.setState({ route: "home" })}
                    >
                        {"< Home"}
                    </button>
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

ReactDOM.render(<Snap></Snap>, document.getElementById('app'));
