import "./style.css"
import { UploadPage } from "./upload/uploadPage";
import { WordBankPage } from "./wordBank/wordBankPage";
import { CustomFunctionPage } from "./customFunction/customFunctionPage";

class Snap extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            route: "home"
        };
    }

    render() {
        if (this.state.route === "home") {
            return (
                <div className="main">
                    <div className="block">
                        <button
                            onClick={() => this.setState({ route: "upload" })}
                        >
                            {"Upload from document"}
                        </button>
                    </div>
                    <div className="block">
                        <button
                            onClick={() => this.setState({ route: "word-bank" })}
                        >
                            {"Highlight used words in a word bank"}
                        </button>
                    </div>
                    <div className="block">
                        <button
                            onClick={() => this.setState({ route: "custom-function" })}
                        >
                            {"Quick define custom function"}
                        </button>
                    </div>
                </div>
            );
        } else {
            return (
                <div className="main">
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
        const { route } = this.state;
        if (route === "upload") {
            return <UploadPage />;
        } else if (route === "word-bank") {
            return <WordBankPage />
        } else if (route === "custom-function") {
            return <CustomFunctionPage />
        }
    }
}

ReactDOM.render(<Snap></Snap>, document.getElementById('app'));