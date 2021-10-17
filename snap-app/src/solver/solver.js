import React from "react";
import { postJson } from "../fetch";
import "./solver.css";

export default class Solver extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            query: '',
            wordLengths: '',
            canRearrange: false,

            loading: false,
            results: [],
        };
    }

    componentDidMount() {
        window.addEventListener("keyup", this.handleKey);
    }

    componentWillUnmount() {
        window.removeEventListener("keyup", this.handleKey);
    }


    render() {
        const { query, wordLengths, canRearrange, loading, results } = this.state;
        return <>
            <div className="block">
                Examples:
                <ul>
                    <li>
                        <span className="inline">
                            A string with some unknown letters: (<span class="parsed">.H.S.nEH..so.E..KNeWN.E.tE.S</span>)
                        </span>
                        <input
                            type="button"
                            value="Demo"
                            onClick={() => this.setState({
                                query: '.H.S.nEH..so.E..KNeWN.E.tE.S',
                                wordLengths: '',
                                canRearrange: false,
                            })}
                        />
                    </li>
                    <li>
                        <span className="inline">
                            A collection of letters to anagram: (<span class="parsed">AADDDEGILNNOORRRRUU</span>)
                        </span>
                        <input
                            type="button"
                            value="Demo"
                            onClick={() => this.setState({
                                query: 'AADDDEGILNNOORRRRUU',
                                wordLengths: '',
                                canRearrange: true,
                            })}
                        />
                    </li>
                    <li>
                        <span className="inline">
                            A space/comma separated list of character sequences to rearrange (<span class="parsed">CHA DEB ERO GRA HES ITY LFO MPI ONC REV TIT TOF TOT UDE WEA WIL E</span>)
                        </span>
                        <input
                            type="button"
                            value="Demo"
                            onClick={() => this.setState({
                                query: 'CHA DEB ERO GRA HES ITY LFO MPI ONC REV TIT TOF TOT UDE WEA WIL E',
                                wordLengths: '8 4 4 7 3 1 4 2 9 2 5',
                                canRearrange: true,
                            })}
                        />
                    </li>
                </ul>
                Capital letters denote letters you're sure about, lowercase letters denote about 80% confidence, and periods ('.') are wildcards.
                <br />
                You can also use{" "}
                <span class="parsed">()</span> for groups,{" "}
                <span class="parsed">[]</span> for one of several possible letters,{" "}
                <span class="parsed">{"<>"}</span> for (nested) anagrams,{" "}
                <span class="parsed">""</span> to forbid spaces,{" "}
                and <span class="parsed">{"{}, ?, +"}</span> as in regular expressions.
            </div>
            <div class="block">
                <div>
                    {"Letters/parts: "}
                    <input
                        style={{ width: 500 }}
                        value={query}
                        onChange={e => this.setState({ query: e.target.value })}
                    />
                </div>
                <div>
                    {"Space/comma separated word lengths (can be left blank): "}
                    <input
                        style={{ width: 217 }}
                        value={wordLengths}
                        onChange={e => this.setState({ wordLengths: e.target.value })}
                    />
                </div>
                <div>
                    {"Should rearrange letters/parts: "}
                    <input
                        type="checkbox"
                        checked={canRearrange}
                        onChange={() => this.setState({ canRearrange: !canRearrange })}
                    />
                </div>
                <input type="button" value="Solve!" onClick={this.solve} />
            </div>
            {loading ? <span className="loading" /> : undefined}
            <div class="block">
                <table>
                    <tbody>
                        {results.length > 0 ? <tr>
                            <td>Words</td>
                            <td>Score</td>
                        </tr> : undefined}
                        {results.map(({ words, score }) => <tr>
                            <td>{words.join(' ')}</td>
                            <td>{(score + 100).toFixed(2)}</td>
                        </tr>)}
                    </tbody>
                </table>
            </div>
        </>;
    }

    handleKey = e => {
        if (e.key !== 'Enter') {
            return;
        }
        this.solve();
        e.preventDefault();
    }

    solve = () => {
        const { query, wordLengths, canRearrange } = this.state;
        this.setState({ loading: true });
        postJson({
            path: '/words/cromulence', body: {
                parts: query.split(/[ ,]+/),
                wordLengths: wordLengths.length > 0 ? wordLengths.split(/[^0-9]+/) : undefined,
                canRearrange,
            }
        }, ({ results }) => this.setState({ results, loading: false }));
    }
}
