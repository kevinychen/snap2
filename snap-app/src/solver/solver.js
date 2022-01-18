import React from "react";
import { withRouter } from "react-router-dom";
import { postJson } from "../fetch";
import "./solver.css";

class Solver extends React.Component {

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
        const searchParams = new URLSearchParams(this.props.location.search);
        if (searchParams.get("r") === '1') {
            this.setState({
                canRearrange: true
            });
        }
        if (searchParams.get("wl")) {
            const wl = decodeURI(searchParams.get("wl"));
            const wordLengths = wl.split(/[^0-9]+/);
            if (wordLengths.length > 0) {
                this.setState({
                    wordLengths: wordLengths.join(" ").trim()
                });
            }
        }
        if (searchParams.get("q")) {
            this.setState({
                query: decodeURI(searchParams.get("q"))
            }, this.solve);
        }
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
                            A string with some unknown letters: (<span className="parsed">.H.S.nEH..so.E..KNeWN.E.tE.S</span>)
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
                            A collection of letters to anagram: (<span className="parsed">AADDDEGILNNOORRRRUU</span>)
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
                            A space/comma separated list of character sequences to rearrange (<span className="parsed">CHA DEB ERO GRA HES ITY LFO MPI ONC REV TIT TOF TOT UDE WEA WIL E</span>)
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
                <span className="parsed">(ABC)</span> for ABC in that order,{" "}
                <span className="parsed">[ABC]</span> for A, B, or C,{" "}
                <span className="parsed">{"<ABC>"}</span> for ABC in any order (anagram),{" "}
                <span className="parsed">"ABC"</span> to forbid spaces,{" "}
                and <span className="parsed">{"{}, ?, +"}</span> as in regular expressions.
            </div>
            <div className="block">
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
            <div className="block">
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

    setSearchParams = () => {
        const { query, wordLengths, canRearrange } = this.state;
        const searchParams = new URLSearchParams({
            q: encodeURI(query)
        });
        if (canRearrange) {
            searchParams.set("r", "1");
        }
        if (wordLengths.length > 0) {
            const wl = wordLengths.split(/[^0-9]+/).join(" ").trim();
            if (wl.length > 0) {
                searchParams.set("wl", encodeURI(wl));
            }
        }
        this.props.history.push({
            pathname: '/solver',
            search: `?${searchParams.toString()}`
        });
    }

    solve = () => {
        const { query, wordLengths, canRearrange } = this.state;
        this.setState({ loading: true });
        postJson({
            path: '/words/cromulence', body: {
                parts: query.split(/[ ,]+/),
                wordLengths: wordLengths.length > 0 ? wordLengths.trim().split(/[^0-9]+/) : undefined,
                canRearrange,
            }
        }, ({ results }) => this.setState({ results, loading: false }));
        this.setSearchParams();
    }
}

export default withRouter(Solver);
