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
            wordbank: '',
            boggle: false,
            results: [],
            highlightedIndex: undefined,
            highlightedPositions: {},
            highlightedWords: {},
            highlightingAllWordbank: false,
            highlightingDifferentColors: false,

            loading: false,
        };
    }

    componentDidMount() {
        const hash = window.location.hash.substr(1);
        if (hash !== '') {
            this.setState({ ...JSON.parse(Buffer.from(hash, 'base64')) }, this.solve);
        }
    }

    render() {
        const {
            editMode,
            boggle,
            results,
            highlightedIndex,
            highlightedPositions,
            highlightedWords,
            highlightingAllWordbank,
            highlightingDifferentColors,
            loading
        } = this.state;
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
                    <textarea className="grid"
                        onChange={e => this.setState({ grid: e.target.value })}
                        value={this.state.grid}
                    />
                     <div>
                        {"Optional wordbank (split by tab and/or newline; non-alphanumeric chars ignored): "}
                        <input
                            type="button"
                            value="Demo"
                            onClick={() => this.setState({
                                grid: 'MENIMDLORAIREIMMUSKD\nWALUIGIGSOPAWTAADALH\nTERRNAEEWATOELRETTEC\n'
                                    + 'ROIRODRIBYSFORIPOMEA\nANDILETTOPEACROFEINE\nCDRTPAMYWOCLHGTUEAHB\n'
                                    + 'EANILASORSACLASATOCP\nWDFLSERYORNDORSSPEAH\nAYBOWSACTTOLVDENEOCB\n'
                                    + 'PSRESSFFLEDAEKPIPOOO\nYSIADSEIOTESLSDGPIPK\nCROGDTONHFSONBNITOIU\n'
                                    + 'ASTLAFSIOSETROMURGTC\nLIPEOSQOOBOTKEPLDAMR\nESEDSTUORRYYEFSDVIGI\n'
                                    + 'RINRERAIDTERERNWVSYC\nTOADAIOCOKTGDSASOCNI\nHOSIUCTMNCKMOULTEBRE\n'
                                    + 'RILLSEEOUSDLKINGBOOH\nDRREOTDTMALENRITAINS',
                                wordbank: [
                                    "Luigi Circuit", "Mario Circuit", "Daisy Circuit", "Dry Dry Ruins",
                                    "Moo Moo Meadows", "Coconut Mall", "Koopa Cape", "Moonview Highway",
                                    "Mushroom Gorge", "DK Summit", "Maple Treeway", "Bowser's Castle",
                                    "Toad's Factory", "Wario's Gold Mine", "Grumble Volcano", "Rainbow Road",
                                    "Peach Beach", "Sherbet Land", "Desert Hills", "Mario Circuit", "Yoshi Falls",
                                    "Shy Guy Beach", "Bowser Castle", "Peach Gardens", "Ghost Valley",
                                    "Delfino Square", "DK's Jungle Parkway", "DK Mountain", "Mario Raceway",
                                    "Waluigi Stadium", "Mario Circuit", "Bowser's Castle"
                                ].join('\t'),
                                boggle: true
                            })}
                        />
                    </div>
                     <textarea className="wordbank"
                        onChange={e => this.setState({ wordbank: e.target.value })}
                        value={this.state.wordbank}
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
                            </span><br/><br/>
                            {results.find(result => result.inWordbank) && <>
                            <input
                                type="checkbox"
                                checked={highlightingAllWordbank}
                                onChange={() => this.setState({ highlightingAllWordbank: !highlightingAllWordbank }, this.onHighlightWordbankChange)}
                            />
                            {"Highlight all wordbank"}
                            {highlightingAllWordbank && <>
                            <input
                                type="checkbox"
                                checked={highlightingDifferentColors}
                                onChange={() => this.setState({ highlightingDifferentColors: !highlightingDifferentColors }, this.onHighlightWordbankChange)}
                            />
                            {"Highlight different colors"}
                            </>}
                            </>
                            }
                        </div>
                        <table className="block">
                            <tbody>
                                {grid.map((row, y) => <tr key={y}>
                                    {[...row].map((c, x) => <td
                                        key={x}
                                        className={classNames({
                                            hovering: highlightedPositions[`${x}-${y}`] !== undefined,
                                            "in-wordbank": highlightedPositions[`${x}-${y}`]?.inWordbank
                                        })}
                                        style={{ backgroundColor: highlightedPositions[`${x}-${y}`]?.color }}
                                    >
                                        {c}
                                    </td>)}
                                </tr>)}
                            </tbody>
                        </table>
                        {highlightingAllWordbank && <>
                            <p>Unused letters: </p>
                            <textarea className="unused"
                                readOnly={true}
                                value={grid.reduce((unused, row, y) =>
                                    unused + [...row].reduce((s, c, x) =>
                                        s + (highlightedPositions[`${x}-${y}`] === undefined ? c : '')
                                    , '')
                                , '')}>
                            </textarea>
                        </>
                        }
                </>}
            </div>
            <div className="output">
                {results.map(({ word, positions, inWordbank }, index) => <input
                    key={index}
                    className={classNames("link", { hovering: index === highlightedIndex, "in-wordbank": inWordbank })}
                    style={{ backgroundColor: highlightedWords[index]?.color }}
                    value={word}
                    readOnly={true}
                    onMouseEnter={() => {
                        if (!highlightingAllWordbank) {
                            const highlightedPositions = {};
                            const highlightedWords = {};
                            for (const { x, y } of positions) {
                                highlightedPositions[`${x}-${y}`] = { inWordbank };
                            }
                            this.setState({ highlightedIndex: index, highlightedPositions, highlightedWords });
                        }
                    }}
                />)}
            </div>
        </div>;
    }

    hashLocation() {
        const { grid, wordbank, boggle, highlightingAllWordbank, highlightingDifferentColors } = this.state;
        window.location.hash = Buffer.from(JSON.stringify({ grid, wordbank, boggle, highlightingAllWordbank, highlightingDifferentColors })).toString('base64');
    }

    hashStringToColor(str) {
        var hash = 0;
        for (let i = 0; i < str.length; i++) {
            hash = str.charCodeAt(i) + ((hash << 5) - hash);
        }
        let colour = '#';
        for (let i = 0; i < 3; i++) {
            const value = (hash >> (i * 8)) & 0xFF;
            colour += ('00' + value.toString(16)).substr(-2);
        }
        colour += 'aa';
        return colour;
    }

    onHighlightWordbankChange() {
        const { highlightingAllWordbank, highlightingDifferentColors, results } = this.state;
        const highlightedWords = {};
        const highlightedPositions = {};
        if (highlightingAllWordbank) {
            results.filter(result => result.inWordbank).forEach(
                ({ positions, word }, index
            ) => {
                const color = highlightingDifferentColors ? this.hashStringToColor(word) : '#f9ce70';
                highlightedWords[index] = { color };
                positions.forEach(({ x, y }) => {
                    const key = `${x}-${y}`;
                    if (!highlightedPositions.hasOwnProperty(key)) {
                        highlightedPositions[key] = { color };
                    }
                });
            });
        }
        this.setState({ highlightedPositions, highlightedWords, highlightedIndex: undefined });
        this.hashLocation();
    }

    getGrid() {
        return this.state.grid.trim().toUpperCase().split('\n').map(word => word.replace(/\s/g, ''));
    }

    getWordbank() {
        return this.state.wordbank.trim().toUpperCase().split(/\n|\t/).map(word => word.replace(/[^A-Z0-9]/g, '')).filter(word => word.length > 1);
    }

    solve = () => {
        const { boggle } = this.state;
        this.hashLocation();
        this.setState({ loading: true });
        postJson({
            path: '/words/search',
            body: {
                grid: this.getGrid(),
                wordbank: this.getWordbank(),
                boggle,
            }
        }, ({ results }) => {
            this.setState({ editMode: false, results, highlightedPositions: [], loading: false }, this.onHighlightWordbankChange);
            if (!results.find(result => result.inWordbank)) {
                this.setState({ highlightingAllWordbank: false });
            }
        });
    }
}
