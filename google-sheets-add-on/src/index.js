import "./style.css"
import { UploadPage } from "./upload/uploadPage";
import { WordBankPage } from "./wordBank/wordBankPage";
import { CustomFunctionPage } from "./customFunction/customFunctionPage";
import { AutoMatchPage } from "./autoMatch/autoMatchPage";
import { FindWordsPage } from "./findWords/findWordsPage";

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
                            {"Upload crosswords, grids, and images"}
                        </button>
                    </div>
                    <div className="block">
                        <button
                            onClick={() => this.setState({ route: "find-words" })}
                        >
                            {"Heavy-duty message solver"}
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
                            onClick={() => this.setState({ route: "auto-match" })}
                        >
                            {"Reorder column(s) to match another column"}
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
        } else if (route === "auto-match") {
            return <AutoMatchPage />
        } else if (route === "find-words") {
            return <FindWordsPage />
        }
    }
}

ReactDOM.render(<Snap></Snap>, document.getElementById('app'));
