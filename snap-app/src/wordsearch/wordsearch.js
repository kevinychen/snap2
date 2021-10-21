import classNames from "classnames";
import React from "react";
import { postJson } from "../fetch";
import "./wordsearch.css";

export default class Wordsearch extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            editMode: true,
            grid: '',
            boggle: false,
            results: [],
            highlightedIndex: undefined,
            highlightedPositions: [],

            loading: false,
        };
    }

    componentDidMount() {
        const hash = window.location.hash.substr(1);
        if (hash !== '') {
            this.setState({ ...JSON.parse(Buffer.from(hash, 'base64')) });
            setTimeout(this.solve, 100);
        }
    }

    render() {
        const { editMode, boggle, results, highlightedIndex, highlightedPositions, loading } = this.state;
        const grid = this.getGrid();
        return <div className="wordsearch">
            <div className="input">
                {editMode ? <>
                    <div>
                        {"Enter wordsearch grid (spaces are ignored): "}
                        <input
                            type="button"
                            value="Demo"
                            onClick={() => this.setState({
                                grid: 'OHQRECTANGLEP\nDVQIMHESQWRAK\nICAVULQNTHRKO\nOPXLQUOROAINN\n'
                                    + 'ZNPPAGIMLCHYO\nERURAABLHREDG\nPYETNUEZEFXSA\nAICGSLCDEHAPT\n'
                                    + 'ROLPOINYBEGHN\nTELGRIPHUIOEE\nSURCLATYCONRP\nGALYLMECRJAEK\nMECZHFDIMARYP'
                            })}
                        />
                    </div>
                    <textarea
                        onChange={e => this.setState({ grid: e.target.value })}
                        value={this.state.grid}
                    />
                    <div className="block">
                        <input type="radio" checked={!boggle} onChange={() => this.setState({ boggle: !boggle })} />Straight
                        <input type="radio" checked={boggle} onChange={() => this.setState({ boggle: !boggle })} />Boggle
                    </div>
                    <input
                        type="button"
                        value="Solve Wordsearch"
                        onClick={this.solve}
                    />
                    <br />
                    {loading ? <span className="loading" /> : undefined}
                </> : <>
                        <div className="block">
                            <span
                                className="button"
                                onClick={() => {
                                    this.setState({ editMode: true, results: [], highlightedIndex: undefined, highlightedPositions: undefined });
                                    window.location.hash = '';
                                }}
                            >
                                {"< Edit grid"}
                            </span>
                        </div>
                        <table className="block">
                            <tbody>
                                {grid.map((row, y) => <tr key={y}>
                                    {[...row].map((c, x) => <td
                                        key={x}
                                        className={highlightedPositions[`${x}-${y}`] ? 'hovering' : ''}
                                    >
                                        {c}
                                    </td>)}
                                </tr>)}
                            </tbody>
                        </table>
                </>}
            </div>
            <div className="output">
                {results.map(({ word, positions }, index) => <input
                    className={classNames("link", { hovering: index === highlightedIndex })}
                    value={word}
                    readOnly={true}
                    onMouseEnter={() => {
                        const highlightedPositions = {};
                        for (const { x, y } of positions) {
                            highlightedPositions[`${x}-${y}`] = true;
                        }
                        this.setState({ highlightedIndex: index, highlightedPositions });
                    }}
                />)}
            </div>
        </div>;
    }

    getGrid() {
        return this.state.grid.trim().toUpperCase().split('\n').map(word => word.replace(/\s/g, ''));
    }

    solve = () => {
        const { grid, boggle } = this.state;
        this.setState({ loading: true });
        window.location.hash = Buffer.from(JSON.stringify({ grid, boggle })).toString('base64');
        postJson({
            path: '/words/search', body: {
                grid: this.getGrid(),
                boggle,
            }
        }, ({ results }) => {
            this.setState({ editMode: false, results, highlightedPositions: [], loading: false });
        });
    }
}
