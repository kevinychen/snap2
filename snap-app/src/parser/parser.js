import classNames from "classnames";
import React from "react";
import { get, post, postJson } from "../fetch";
import { AdvancedSettingsPopup } from "./advancedSettingsPopup";
import { DocumentImage } from "./documentImage";
import { Output } from "./output";
import { ExportPopup } from "./exportPopup";
import { ParseCrosswordPopup } from "./parseCrosswordPopup";
import "./parser.css";

export default class Parser extends React.Component {
    
    constructor(props) {
        super(props);
        this.state = {
            url: '',
            document: undefined,

            page: 0,
            imageDataUrl: undefined,
            mode: undefined,
            popupMode: undefined,
            imageDimensions: { width: 0, height: 0 },
            selectedAll: false,
            rectangle: undefined,
            blobs: undefined,
            gridLines: undefined,
            gridPosition: undefined,
            grid: undefined,
            crossword: undefined,
            crosswordClues: undefined,

            findGridLinesMode: "EXPLICIT",

            loadingDocument: false,
            loadingGrid: false,
            loadingClipboard: false,
        };
    }

    componentDidMount() {
        window.addEventListener("paste", this.pasteImage);
    }

    componentWillUnmount() {
        window.removeEventListener("paste", this.pasteImage);
    }

    componentDidUpdate(_, prevState) {
        const { document, page } = this.state;
        if (document !== undefined ) {
            if (document !== prevState.document || page !== prevState.page) {
                const { compressedImageId } = document.pages[page];
                get({ path: `/files/${compressedImageId}` }, response => {
                    response.blob().then(blob => {
                        const reader = new FileReader()
                        reader.onloadend = () => {
                            this.setState({ imageDataUrl: reader.result });
                        };
                        reader.readAsDataURL(blob);
                    });
                });
            }
        }
    }

    render() {
        const { url, document, popupMode, blobs, grid, crossword, loadingGrid, loadingClipboard } = this.state;
        return <div className="parser">
            <div className="input">
                <div className="block">
                    <input
                        className="inline"
                        style={{ width: "300px" }}
                        type="text"
                        placeholder="Enter URL of image/PDF/HTML..."
                        value={url}
                        onKeyUp={this.setUrl}
                        onChange={this.setUrl}
                    />
                    <span className="inline">or</span>
                    <input
                        className="inline"
                        type="file"
                        onChange={e => this.setFile(e.target.files[0])}
                    />
                </div>
                {this.maybeRenderToolbar()}
                {this.maybeRenderDocument()}
            </div>
            <div className="output">
                <div className="block">
                    <div
                        className={classNames({ hidden: document === undefined }, "big button")}
                        onClick={() => {
                            if (!this.state.loadingGrid) {
                                this.setState({ loadingGrid: true });
                                this.findCrossword(() => this.setState({ loadingGrid: false }));
                            }
                        }}
                    >
                        {loadingGrid ? <span className="loading" /> : "Parse crossword"}
                    </div>
                    <div
                        className={classNames({ hidden: document === undefined }, "big button")}
                        onClick={() => {
                            if (!this.state.loadingGrid) {
                                this.setState({ loadingGrid: true });
                                this.findGrid(() => this.setState({ loadingGrid: false }));
                            }
                        }}
                    >
                        {loadingGrid ? <span className="loading" /> : "Parse grid"}
                    </div>
                    <div
                        className={classNames({ hidden: document === undefined }, "big button")}
                        onClick={() => {
                            if (!this.state.loadingGrid) {
                                this.setState({ loadingGrid: true });
                                this.findBlobs(() => this.setState({ loadingGrid: false }));
                            }
                        }}
                    >
                        {loadingGrid ? <span className="loading" /> : "Parse blobs"}
                    </div>
                </div>
                <div id="html-grid" className="block">
                    <Output grid={grid} crossword={crossword} />
                </div>
                <div className="block">
                    <div
                        className={classNames({ hidden: grid === undefined }, "big button")}
                        onClick={this.copyGridToClipboard}
                    >
                        {loadingClipboard ? "Copied!" : "Copy to clipboard"}
                    </div>
                    <div
                        className={classNames({ hidden: blobs === undefined && grid === undefined }, "big button")}
                        onClick={this.copyGridToSheet}
                    >
                        {"Export to Sheets"}
                    </div>
                </div>
            </div>
            <AdvancedSettingsPopup
                {...this.state}
                isVisible={popupMode === "ADVANCED_SETTINGS"}
                setAdvancedSetting={(key, value) => this.setState({ [key]: value })}
                exit={() => this.setState({ popupMode: undefined })}
            />
            <ParseCrosswordPopup
                isVisible={popupMode === "PARSE_CLUES"}
                setCrosswordClues={this.setCrosswordClues}
                exit={() => this.setState({ popupMode: undefined })}
            />
            <ExportPopup
                {...this.state}
                isVisible={popupMode === "EXPORT"}
                exit={() => this.setState({ popupMode: undefined })}
            />
        </div>;
    }

    maybeRenderToolbar() {
        const { document, mode, imageDimensions, selectedAll } = this.state;
        if (document === undefined) {
            return;
        }
        return <div className="block">
            <button
                className="inline"
                onClick={() => {
                    this.setRectangle({ x: 0, y : 0, width: imageDimensions.width, height: imageDimensions.height });
                    this.setState({ mode: undefined, selectedAll: true });
                }}
            >
                {"Reset"}
            </button>
            <button
                className="inline"
                onClick={() => this.setState({ popupMode: "ADVANCED_SETTINGS" })}
            >
                {"Advanced..."}
            </button>
            {this.maybeRenderNav()}
            <div className="toolbar_options">
                {selectedAll ? <span className="inline">Entire image selected.</span> : undefined}
                <div className="inline">
                    <span
                        className={classNames({ selected: mode === "SELECT_REGION" }, "radio")}
                        onClick={() => this.setState({ mode: "SELECT_REGION" })}
                    >
                        {"Select region"}
                    </span>
                    <span
                        className={classNames({ selected: mode === "EDIT_GRID_LINES" }, "radio")}
                        onClick={() => this.setState({ mode: "EDIT_GRID_LINES" })}
                    >
                        {"Edit grid lines"}
                    </span>
                </div>
            </div>
        </div>;
    }

    maybeRenderNav() {
        const { document, page } = this.state;
        if(document.pages.length === 1) {
            return;
        }
        return <>
            <button
                className="inline"
                onClick={() => this.setState({ page: page - 1 })}
                disabled={page === 0}
            >
                {"<"}
            </button>
            <span className="inline">Page {page + 1}/{document.pages.length}</span>
            <button
                className="inline"
                onClick={() => this.setState({ page: page + 1 })}
                disabled={page === document.pages.length - 1}
            >
                {">"}
            </button>
        </>;
    }

    maybeRenderDocument() {
        const { loadingDocument, document } = this.state;
        if (loadingDocument) {
            return <span className="loading"></span>;
        }
        if (document === undefined) {
            return "or paste an image from the clipboard."
        }
        return <DocumentImage
            {...this.state}
            setImageDimensions={this.setImageDimensions}
            setRectangle={this.setRectangle}
            setGridLines={this.setGridLines}
            setCrossword={this.setCrossword}
        />
    }

    pasteImage = e => {
        const item = (e.clipboardData || e.originalEvent.clipboardData).items[0];
        if (item.kind === 'file') {
            this.setFile(item.getAsFile());
        }
    }

    setUrl = e => {
        const url = e.target.value;
        if (url && url !== this.state.url) {
            this.setState({ url, loadingDocument: true });
            postJson({ path: '/documents/url', body: { url } }, this.setDocument);
        }
    }

    setFile = file => {
        if (file === undefined) {
            return;
        }
        const formData = new FormData();
        if (file.type === "application/pdf") {
            formData.append('pdf', file);
            this.setState({ url: '', loadingDocument: true });
            post({ path: '/documents/pdf', body: formData }, this.setDocument);
        } else if (file.type.startsWith("image/")) {
            formData.append('image', file);
            this.setState({ url: '', loadingDocument: true });
            post({ path: '/documents/image', body: formData }, this.setDocument);
        }
    }

    setDocument = document => {
        this.setState({ loadingDocument: false, document, page: 0 });
    }

    setImageDimensions = imageDimensions => {
        this.setRectangle({ x: 0, y: 0, width: imageDimensions.width, height: imageDimensions.height });
        this.setState({ imageDimensions, selectedAll: true })
    }

    setRectangle = rectangle => {
        this.setState({ rectangle, selectedAll: false });
        this.setGridLines({
            horizontalLines: [0, rectangle.height],
            verticalLines: [0, rectangle.width],
        });
    }

    setBlobs = blobs => {
        const { rectangle } = this.state;
        if (blobs) {
            this.setGridLines({
                horizontalLines: [0, rectangle.height],
                verticalLines: [0, rectangle.width],
            });
        }
        this.setState({ blobs });
    }

    setGridLines = gridLines => {
        if (gridLines) {
            this.setBlobs(undefined);
        }
        this.setGrid(undefined, undefined);
        this.setState({ gridLines });
    }

    setGrid = (gridPosition, grid) => {
        this.setCrossword(undefined);
        this.setState({ gridPosition, grid });
    }

    setCrossword = crossword => {
        this.setState({ crossword });
    }

    setCrosswordClues = crosswordClues => {
        this.setState({ crosswordClues });
    }

    findGridLines = callback => {
        const { document, page, rectangle, gridLines, findGridLinesMode } = this.state;
        if (gridLines.horizontalLines.length > 2 || gridLines.verticalLines.length > 2) {
            callback(gridLines);
            return;
        }
        postJson({
            path: `/documents/${document.id}/lines`,
            body: {
                section: { page, rectangle },
                findGridLinesMode,
                interpolate: true,
            }
        }, gridLines => {
            this.setGridLines(gridLines);
            callback(gridLines);
        });
    }

    findGrid = callback => {
        const { document, page, rectangle, gridPosition, grid } = this.state;
        if (grid !== undefined) {
            callback(gridPosition, grid);
            return;
        }
        this.findGridLines(gridLines => {
            postJson({
                path: `/documents/${document.id}/grid`,
                body: {
                    section: { page, rectangle },
                    gridLines,
                }
            }, ({ gridPosition, grid }) => {
                this.setGrid(gridPosition, grid);
                callback(gridPosition, grid);
            });
        });
    }

    findCrossword = callback => {
        const { crossword } = this.state;
        if (crossword !== undefined) {
            return;
        }
        this.findGrid((_, grid) => {
            postJson({
                path: `/words/findCrossword`,
                body: { grid },
            }, response => {
                this.setCrossword(response.crossword);
                this.setState({ navBarMode: "PARSE", popupMode: "PARSE_CLUES" });
                callback();
            });
        });
    }

    findBlobs = callback => {
        const { document, page, rectangle, blobs } = this.state;
        if (blobs !== undefined) {
            return;
        }
        postJson({
            path: `/documents/${document.id}/blobs`,
            body: {
                section: { page, rectangle },
                exact: false,
            },
        }, blobs => {
            this.setBlobs(blobs);
            callback();
        });
    }

    copyGridToClipboard = () => {
        window.getSelection().removeAllRanges();
        const range = document.createRange();
        range.selectNode(document.getElementById('html-grid'));
        window.getSelection().addRange(range);
        document.execCommand("copy");
        window.getSelection().removeAllRanges();
        this.setState({ loadingClipboard: true });
        setTimeout(() => this.setState({ loadingClipboard: false }), 3000);
    }

    copyGridToSheet = () => {
        this.setState({ popupMode: "EXPORT" });
    }
}
