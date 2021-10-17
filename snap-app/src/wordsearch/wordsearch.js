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
            boggleMode: false,
            results: [],
            highlightedWord: undefined,
            highlightedPositions: [],

            loading: false,
        };
    }

    render() {
        const { editMode, boggleMode, results, highlightedWord, highlightedPositions, loading } = this.state;
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
                        <input type="radio" checked={!boggleMode} onChange={() => this.setState({ boggleMode: !boggleMode })} />Straight
                        <input type="radio" checked={boggleMode} onChange={() => this.setState({ boggleMode: !boggleMode })} />Boggle
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
                                onClick={() => this.setState({ editMode: true })}
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
                {results.map(({ word, positions }) => <input
                    className={classNames("link", { hovering: word === highlightedWord })}
                    value={word}
                    readOnly={true}
                    onMouseEnter={() => {
                        const highlightedPositions = {};
                        for (const { x, y } of positions) {
                            highlightedPositions[`${x}-${y}`] = true;
                        }
                        this.setState({ highlightedWord: word, highlightedPositions });
                    }}
                />)}
            </div>
        </div>;
    }

    getGrid() {
        return this.state.grid.trim().toUpperCase().split('\n').map(word => word.replace(/\s/g, ''));
    }

    solve = () => {
        const { boggle } = this.state;
        this.setState({ loading: true });
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
