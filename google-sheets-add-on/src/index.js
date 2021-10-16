import "./style.css"
import { UploadPage } from "./upload/uploadPage";
import { CustomFunctionPage } from "./customFunction/customFunctionPage";
import { FindWordsPage } from "./findWords/findWordsPage";
import { ReshapePage } from "./reshape/reshapePage";

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
                            onClick={() => this.setState({ route: "reshape" })}
                        >
                            {"Reshape values in a range"}
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
        } else if (route === "custom-function") {
            return <CustomFunctionPage />
        } else if (route === "find-words") {
            return <FindWordsPage />
        } else if (route === "reshape") {
            return <ReshapePage />
        }
    }
}

ReactDOM.render(<Snap></Snap>, document.getElementById('app'));
