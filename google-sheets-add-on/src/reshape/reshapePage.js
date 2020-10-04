import "./reshapePage.css";

export class ReshapePage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            startWidth: 1,
            startHeight: 1,
            endWidth: 1,
            endHeight: 1,
        };
    }

    render() {
        const { startWidth, startHeight, endWidth, endHeight } = this.state;
        return (
            <div className="reshape">
                <div className="block">
                    <h3>Reshape</h3>
                </div>
                <div className="block">
                    {"Start width x height: "}
                    <input
                        className="inline"
                        type="number"
                        value={startWidth}
                        onChange={e => this.setState({ startWidth: this.clip(e.target.value) })}
                    />
                    {" x"}
                    <input
                        className="inline"
                        type="number"
                        value={startHeight}
                        onChange={e => this.setState({ startHeight: this.clip(e.target.value) })}
                    />
                </div>
                <div className="block">
                    {"End width x height: "}
                    <input
                        className="inline"
                        type="number"
                        value={endWidth}
                        onChange={e => this.setState({ endWidth: this.clip(e.target.value) })}
                    />
                    {" x"}
                    <input
                        className="inline"
                        type="number"
                        value={endHeight}
                        onChange={e => this.setState({ endHeight: this.clip(e.target.value) })}
                    />
                </div>
                <div className="block">
                    <button
                        className="blue"
                        onClick={this.executeReshape}
                    >
                        {"Reshape"}
                    </button>
                </div>
            </div>
        );
    }

    clip = val => {
        val = parseInt(val);
        if (val <= 0) {
            return 1;
        }
        if (val > 999) {
            return 999;
        }
        return val;
    }

    executeReshape = () => {
        const { startWidth, startHeight, endWidth, endHeight } = this.state;
        gs_reshape(startWidth, startHeight, endWidth, endHeight);
    }
}
