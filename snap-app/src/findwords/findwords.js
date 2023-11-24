import React from "react";
import { postJson } from "../fetch";

export default class FindWords extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            results: [],

            loading: false,
        };
    }

    render() {
        const { results, loading } = this.state;
        return <>
            <div className="block">
                All fields are optional.<br />
                has length at least <input id="find-min-length" type="number" /><br />
                has length at most <input id="find-max-length" type="number" /><br />
                has frequency at least <input id="find-min-freq" type="number" /> (10,000 is a good default)<br />
                matches regex <input id="find-regex" type="text" /><br />
                contains the subsequence <input id="find-contains-subseq" type="text" /><br />
                is a subsequence of <input id="find-contained-subseq" type="text" /><br />
                contains all letters of <input id="find-contains" type="text" /><br />
                has letters all contained in <input id="find-contained" type="text" /><br />

                <input
                    type="button"
                    value="Solve!"
                    onClick={this.solve}
                />
            </div>
            {loading ? <span className="loading" /> : undefined}
            <div className="block">
                <table>
                    <tbody>
                        {results.map(word => <tr key={word}><td>{word}</td></tr>)}
                    </tbody>
                </table>
            </div>
        </>
    }

    solve = () => {
        const minLength = document.getElementById('find-min-length').value;
        const maxLength = document.getElementById('find-max-length').value;
        const minFreq = document.getElementById('find-min-freq').value;
        const regex = document.getElementById('find-regex').value;
        const containsSubseq = document.getElementById('find-contains-subseq').value;
        const containedSubseq = document.getElementById('find-contained-subseq').value;
        const contains = document.getElementById('find-contains').value;
        const contained = document.getElementById('find-contained').value;
        this.setState({ loading: true })
        postJson({
            path: '/words/find', body: {
                minLength: minLength === '' ? undefined : parseInt(minLength),
                maxLength: maxLength === '' ? undefined : parseInt(maxLength),
                minFreq: minFreq === '' ? undefined : parseInt(minFreq),
                regex: regex === '' ? undefined : regex,
                containsSubseq: containsSubseq === '' ? undefined : containsSubseq,
                containedSubseq: containedSubseq === '' ? undefined : containedSubseq,
                contains: contains === '' ? undefined : contains,
                contained: contained === '' ? undefined : contained,
            }
        }, ({ words }) => this.setState({ results: words, loading: false }));
    }
}
