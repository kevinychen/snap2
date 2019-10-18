import "./customFunctionPage.css";

export class CustomFunctionPage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            customFunction: "function(arg) {\n  \n}",
        };
    }

    componentDidMount() {
        gs_getCustomFunction(customFunction => this.setState({ customFunction }));
    }

    render() {
        const { customFunction } = this.state;
        return (
            <div>
                <div className="block">
                    <h3>Define custom function =CUSTOM(...)</h3>
                </div>
                <div className="block">
                    <textarea
                        value={customFunction}
                        onChange={e => this.setState({ customFunction: e.target.value })}
                    />
                </div>
                <div className="block">
                    <button
                        className="blue"
                        onClick={this.save}
                    >
                        Save
                    </button>
                </div>
            </div>
        );
    }

    save = () => {
        gs_saveCustomFunction(this.state.customFunction);
    }
}
