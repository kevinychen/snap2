import React from "react";
import { postJson } from "../fetch";
import "./wordsearch.css";

export default class Wordsearch extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            editMode: true,
            loading: false,
            loadingClipboard: false,

            /* inputs */
            grid: '',
            wordBank: '',
            boggle: false,
            fuzzy: false,

            /* output state */
            results: [],
            hitLimit: false,
            selectedIds: [],
            hoveringId: undefined,
            isAdd: undefined, /* if not undefined, whether currently adding or removing selected indices */
        };
    }

    componentDidMount() {
        const hash = window.location.hash.substr(1);
        if (hash !== '') {
            this.setState({ ...JSON.parse(atob(hash)) }, this.solve);
        }
    }

    render() {
        const { editMode, results, selectedIds, hoveringId } = this.state;

        const gridSquareColors = {};
        const wordColors = {};
        for (const selectedId of selectedIds) {
            const{ path, word } = results.find(result => result.id === selectedId)
            let hash = 0;
            for (let i = 0; i < word.length; i++) {
                hash = word.charCodeAt(i) + ((hash << 5) - hash);
            }
            const color = `hsl(${hash % 359}, 50%, 85%)`;

            for (const { x, y } of path) {
                if (gridSquareColors[`${x}-${y}`] === undefined) {
                    gridSquareColors[`${x}-${y}`] = color;
                }
            }
            wordColors[selectedId] = color;
        }
        if (hoveringId !== undefined) {
            const { path } = results.find(result => result.id === hoveringId)
            path.forEach(({ x, y }) => gridSquareColors[`${x}-${y}`] = '#f9ce70');
            wordColors[hoveringId] = '#f9ce70';
        }

        return <div className="wordsearch">
            <div className="input">
                {editMode ? this.renderEditingGrid() : this.renderSolvedGrid(gridSquareColors)}
            </div>
            {!editMode && this.renderWordList(wordColors)}
        </div>;
    }

    renderEditingGrid() {
        const { loading, boggle, fuzzy } = this.state;

        return <>
            <div>
                {"Enter word search grid (whitespace ignored): "}
                <input
                    type="button"
                    value="Demo"
                    onClick={() => this.setState({
                        grid:
                            "OHQRECTANGLEP\nDVQIMHESQWRAK\nICAVULQNTHRKO\nOPXLQUOROAINN\n" +
                            "ZNPPAGIMLCHYO\nERURAABLHREDG\nPYETNUEZEFXSA\nAICGSLCDEHAPT\n" +
                            "ROLPOINYBEGHN\nTELGRIPHUIOEE\nSURCLATYCONRP\nGALYLMECRJAEK\nMECZHFDIMARYP",
                        wordBank: "",
                        boggle: false,
                        fuzzy: false,
                    })}
                />
            </div>
            <textarea
                className="grid"
                onChange={e => this.setState({ grid: e.target.value })}
                value={this.state.grid}
            />
            <div>
                {"Optional word bank (split by tab or newline; non-alphanumeric characters ignored): "}
                <input
                    type="button"
                    value="Demo"
                    onClick={() => this.setState({
                        grid:
                            "MENIMDLORAIREIMMUSKD\nWALUIGIGSOPAWTAADALH\nTERRNAEEWATOELRETTEC\n" +
                            "ROIRODRIBYSFORIPOMEA\nANDILETTOPEACROFEINE\nCDRTPAMYWOCLHGTUEAHB\n" +
                            "EANILASORSACLASATOCP\nWDFLSERYORNDORSSPEAH\nAYBOWSACTTOLVDENEOCB\n" +
                            "PSRESSFFLEDAEKPIPOOO\nYSIADSEIOTESLSDGPIPK\nCROGDTONHFSONBNITOIU\n" +
                            "ASTLAFSIOSETROMURGTC\nLIPEOSQOOBOTKEPLDAMR\nESEDSTUORRYYEFSDVIGI\n" +
                            "RINRERAIDTERERNWVSYC\nTOADAIOCOKTGDSASOCNI\nHOSIUCTMNCKMOULTEBRE\n" +
                            "RILLSEEOUSDLKINGBOOH\nDRREOTDTMALENRITAINS",
                        wordBank:
                            "Luigi Circuit\nMario Circuit\nDaisy Circuit\nDry Dry Ruins\n" +
                            "Moo Moo Meadows\nCoconut Mall\nKoopa Cape\nMoonview Highway\n" +
                            "Mushroom Gorge\nDK Summit\nMaple Treeway\nBowser's Castle\n" +
                            "Toad's Factory\nWario's Gold Mine\nGrumble Volcano\nRainbow Road\n" +
                            "Peach Beach\nSherbet Land\nDesert Hills\nMario Circuit\n" +
                            "Yoshi Falls\nShy Guy Beach\nBowser Castle\nPeach Gardens\n" +
                            "Ghost Valley\nDelfino Square\nDK's Jungle Parkway\nDK Mountain\n" +
                            "Mario Raceway\nWaluigi Stadium\nMario Circuit\nBowser's Castle",
                        boggle: true,
                        fuzzy: false,
                    })}
                />
            </div>
            <textarea
                className="word-bank"
                onChange={e => this.setState({ wordBank: e.target.value })}
                value={this.state.wordBank}
            />
            <div className="block">
                <label>
                    <input type="radio" checked={!boggle} onChange={() => this.setState({ boggle: false })} />
                    {"Straight"}
                </label>
                <label>
                    <input type="radio" checked={boggle} onChange={() => this.setState({ boggle: true })} />
                    {"Boggle"}
                </label>
                <br />
                <label>
                    <input type="radio" checked={!fuzzy} onChange={() => this.setState({ fuzzy: false })} />
                    {"Exact match"}
                </label>
                <label>
                    <input type="radio" checked={fuzzy} onChange={() => this.setState({ fuzzy: true })} />
                    {"Fuzzy match"}
                </label>
            </div>
            <input type="button" value="Solve word search" onClick={this.solve} />
            <br />
            {loading ? <span className="loading" /> : undefined}
        </>;
    }

    renderSolvedGrid(gridSquareColors) {
        const grid = this.getGrid();

        return <>
            <div className="block">
                <span
                    className="button"
                    onClick={() => {
                        this.setState({ editMode: true, selectedIds: [] });
                        window.location.hash = "";
                    }}
                >
                    {"< Edit grid"}
                </span>
            </div>
            <div id="solved-grid">
                <table className="block">
                    <tbody>
                        {grid.map((row, y) => <tr key={y}>
                            {[...row].map((c, x) => <td key={x} style={{ backgroundColor: gridSquareColors[`${x}-${y}`] }}>
                                {c}
                            </td>)}
                        </tr>)}
                    </tbody>
                </table>
            </div>
            <p>Unused letters:</p>
            <textarea
                className="unused-letters"
                readOnly={true}
                value={grid
                    .flatMap((row, y) => [...row].filter((_, x) => gridSquareColors[`${x}-${y}`] === undefined))
                    .join("")}
            />
        </>;
    }

    renderWordList(wordColors) {
        const { results, loadingClipboard, hitLimit, selectedIds, isAdd } = this.state;

        return <div className="output">
            <div>
                {"Click or drag over words to select them in the grid. "}
                {selectedIds.length === 0
                    ? <input
                        type="button"
                        value="Highlight all"
                        onClick={() => this.setState({ selectedIds: results.map(result => result.id) })}
                    />
                    : <input
                        type="button"
                        value="Clear all"
                        onClick={() => this.setState({ selectedIds: [] })}
                    />}
                <input
                    type="button"
                    value={loadingClipboard ? "Copied!" : "Copy results to clipboard"}
                    onClick={this.copyResultsToClipboard}
                />
            </div>
            <div>
                {"Sort by: "}
                <input
                    type="button"
                    value="Overall score"
                    onClick={() => this.setState({
                        results: results.sort((result1, result2) => result1.id - result2.id),
                    })}
                />
                <input
                    type="button"
                    value="Length"
                    onClick={() => this.setState({
                        results: results.sort((result1, result2) => result2.word.length - result1.word.length),
                    })}
                />
                {results.some(result => result.levenshteinDistance > 0) && <input
                    type="button"
                    value="Exact matches first"
                    onClick={() => this.setState({
                        results: results.sort((result1, result2) => result1.levenshteinDistance - result2.levenshteinDistance),
                    })}
                />}
            </div>
            {hitLimit && <div>{"Computation limit reached. Not all results are shown."}</div>}
            <div id="word-list">
                {results.map(({ id, word, levenshteinDistance }) => <div
                    key={id}
                    className="link"
                    style={{ backgroundColor: wordColors[id] || '#f1f1f1' }}

                    // On mouse click, select the word if it's currently unselected, or unselect it otherwise.
                    // When dragging, select or unselect the passed words based on whether the initial click was an add or remove.
                    onMouseDown={() => {
                        const isAdd = !selectedIds.includes(id);
                        this.setState(prevState => ({
                            selectedIds: isAdd
                                ? [...prevState.selectedIds, id]
                                : prevState.selectedIds.filter(selectedId => selectedId !== id),
                            isAdd,
                        }));
                    }}
                    onMouseUp={() => this.setState({ isAdd: undefined })}
                    onMouseEnter={e => {
                        this.setState(prevState => ({ ...prevState, hoveringId: id }))
                        if (isAdd !== undefined && e.buttons === 1) {
                            this.setState(prevState => ({
                                selectedIds: isAdd
                                    ? [...prevState.selectedIds, id]
                                    : prevState.selectedIds.filter(i => i !== id),
                            }));
                        }
                    }}
                    onMouseLeave={() => this.setState(prevState => ({ ...prevState, hoveringId: undefined }))}
                >{word}{levenshteinDistance > 0 ? '*' : ''}</div>)}
            </div>
        </div>
    }

    getGrid() {
        return this.state.grid.toUpperCase().split('\n').map(line => line.replace(/\s/g, '')).filter(line => line !== '');
    }

    getWordBank() {
        return this.state.wordBank.toUpperCase().split(/\n|\t/).map(line => line.replace(/[^A-Z0-9]/g, '')).filter(line => line !== '');
    }

    solve = () => {
        const { grid, wordBank, boggle, fuzzy } = this.state;

        window.location.hash = btoa(JSON.stringify({ grid, wordBank, boggle, fuzzy }));

        this.setState({ loading: true });
        postJson({
            path: '/words/search',
            body: {
                grid: this.getGrid(),
                wordBank: this.getWordBank(),
                boggle,
                fuzzy,
            }
        }, ({ results, hitLimit }) => this.setState({
            editMode: false,
            results: results.map((result, i) => ({ ...result, id: i })),
            hitLimit,
            hoveringId: undefined,
            loading: false,
        }));
    }

    copyResultsToClipboard = () => {
        const html = document.getElementById("solved-grid").innerHTML + document.getElementById("word-list").innerHTML;
        const content = new Blob([html], { type: 'text/html' });
        const data = [new window.ClipboardItem({ [content.type]: content })];
        this.setState({ loadingClipboard: true });
        navigator.clipboard.write(data).then(() => {
            setTimeout(() => this.setState({ loadingClipboard: false }), 3000);
        });
    }
}
