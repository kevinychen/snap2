import * as classNames from "classnames";
import { get, postJson } from "../fetch";
import { DocumentImage } from "./documentImage";
import { ParseBlobsPopup } from "./parseBlobsPopup";
import { ParseGridLinesPopup } from "./parseGridLinesPopup";
import { ParseContentPopup } from "./parseContentPopup";
import { ParseCrosswordPopup } from "./parseCrosswordPopup";
import { DropdownMenu } from "./dropdownMenu";

export class Document extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            page: 0,
            imageDataUrl: undefined,
            navbarMode: undefined,
            mode: undefined,
            imageDimensions: { width: 0, height: 0 },
            rectangle: undefined,
            blobs: undefined,
            gridLines: undefined,
            gridPosition: undefined,
            grid: undefined,
            crossword: undefined,
            crosswordClues: undefined,
            popupMode: undefined,
            numExporting: 0,
            sharedWithServer: false,
        };
    }

    componentDidMount() {
        this.fetchImage(this.props.document.pages[this.state.page].compressedImageId);
    }

    componentDidUpdate(prevProps, prevState) {
        const { compressedImageId } = this.props.document.pages[this.state.page];
        const { compressedImageId: prevCompressedImageId } = prevProps.document.pages[prevState.page];
        if (compressedImageId !== prevCompressedImageId) {
            this.fetchImage(compressedImageId);
        }
    }

    fetchImage(compressedImageId) {
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

    render() {
        const { document } = this.props;
        const {
            navBarMode,
            page,
            imageDimensions,
            rectangle,
            gridLines,
            grid,
            crossword,
            popupMode,
            numExporting,
        } = this.state;

        return (
            <div className="block">
                <div className="block">
                    <button
                        className="inline"
                        onClick={() => this.setState({ page: page - 1 })}
                        disabled={page == 0}
                    >
                        {"<"}
                    </button>
                    <span className="inline">Page {page + 1}/{document.pages.length}</span>
                    <button
                        className="inline"
                        onClick={() => this.setState({ page: page + 1 })}
                        disabled={page == document.pages.length - 1}
                    >
                        {">"}
                    </button>

                    <DropdownMenu value={this.maybeBold("Select", navBarMode === "SELECT")}>
                        <div
                            className="clickable"
                            onClick={() => this.setState({ navBarMode: "SELECT", mode: "RECTANGLE" })}
                        >
                            Rectangular region
                        </div>
                        <div
                            className="clickable"
                            onClick={() => {
                                this.setState({ navBarMode: "SELECT" });
                                this.setRectangle({ x: 0, y: 0, width: imageDimensions.width, height: imageDimensions.height });
                            }}
                        >
                            All
                        </div>
                    </DropdownMenu>
                    <DropdownMenu value={this.maybeBold("Detect", navBarMode === "PARSE")}>
                        <div
                            className={classNames({"clickable": rectangle !== undefined})}
                            onClick={() => this.setState({ navBarMode: "PARSE", popupMode: "PARSE_BLOBS"})}
                        >
                            Blobs
                        </div>
                        <div
                            className={classNames({"clickable": rectangle !== undefined})}
                            onClick={() => this.setState({ navBarMode: "PARSE", popupMode: "PARSE_GRID_LINES"})}
                        >
                            Grid lines
                        </div>
                        <div
                            className={classNames({"clickable": gridLines !== undefined})}
                            onClick={() => this.setState({ navBarMode: "PARSE", popupMode: "PARSE_CONTENT"})}
                        >
                            Grid square content
                        </div>
                        <div
                            className={classNames({"clickable": grid !== undefined})}
                            onClick={() => {
                                this.setState({ navBarMode: "PARSE" });
                                postJson({
                                    path: `/words/findCrossword`,
                                    body: { grid },
                                }, response => {
                                    this.setCrossword(response.crossword);
                                    this.setState({ navBarMode: "PARSE", popupMode: "PARSE_CLUES"});
                                });
                            }}
                        >
                            Crossword
                        </div>
                    </DropdownMenu>
                    <DropdownMenu value={this.maybeBold("Edit", navBarMode === "EDIT")}>
                        <div
                            className={classNames({"clickable": gridLines !== undefined})}
                            onClick={() => this.setState({ navBarMode: "EDIT", mode: "GRID_LINES" })}
                        >
                            Grid lines
                        </div>
                        <div
                            className={classNames({"clickable": crossword !== undefined})}
                            onClick={() => this.setState({ navBarMode: "EDIT", mode: "CROSSWORD" })}
                        >
                            Crossword numbers
                        </div>
                    </DropdownMenu>

                    <button
                        className="export-button"
                        onClick={() => this.export()}
                    >
                        Export {this.currentType()} to cursor
                        {numExporting > 0 ? " (...)" : ""}
                    </button>

                    <ParseBlobsPopup
                        isVisible={popupMode === "PARSE_BLOBS"}
                        document={document}
                        {...this.state}
                        setBlobs={this.setBlobs}
                        exit={this.clearPopupMode}
                    />
                    <ParseGridLinesPopup
                        isVisible={popupMode === "PARSE_GRID_LINES"}
                        document={document}
                        {...this.state}
                        setGridLines={this.setGridLines}
                        exit={this.clearPopupMode}
                    />
                    <ParseContentPopup
                        isVisible={popupMode === "PARSE_CONTENT"}
                        document={document}
                        {...this.state}
                        setGrid={({ gridPosition, grid }) => this.setGrid(gridPosition, grid)}
                        exit={this.clearPopupMode}
                    />
                    <ParseCrosswordPopup
                        isVisible={popupMode === "PARSE_CLUES"}
                        document={document}
                        {...this.state}
                        setCrosswordClues={this.setCrosswordClues}
                        exit={this.clearPopupMode}
                    />
                </div>
                <DocumentImage
                    {...this.state}
                    setImageDimensions={this.setImageDimensions}
                    setRectangle={this.setRectangle}
                    setGridLines={this.setGridLines}
                    setCrossword={this.setCrossword}
                />
            </div>
        );
    }

    maybeBold(value, isBold) {
        if (isBold) {
            return <b>{value}</b>
        } else {
            return value;
        }
    }

    currentType() {
        const { blobs, gridLines, grid, crossword, crosswordClues } = this.state;
        if (gridLines && grid) {
            if (crossword && crosswordClues) {
                return "crossword";
            } else {
                return "grid";
            }
        } else if (blobs) {
            return "blobs";
        } else if (gridLines && gridLines.horizontalLines.length * gridLines.verticalLines.length > 1) {
            return "images";
        } else {
            return "image";
        }
    }

    setImageDimensions = imageDimensions => {
        this.setRectangle({ x: 0, y: 0, width: imageDimensions.width, height: imageDimensions.height });
        this.setState({ imageDimensions })
    }

    setRectangle = rectangle => {
        this.setState({ rectangle });
        this.setGridLines({
            horizontalLines: [0, rectangle.height],
            verticalLines: [0, rectangle.width],
        });
    }

    setBlobs = blobs => {
        if (blobs) {
            this.setGridLines(undefined);
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

    clearPopupMode = () => {
        this.setState({ popupMode: undefined });
    }

    export = () => {
        this.setState({ numExporting: this.state.numExporting + 1 });
        if (this.state.sharedWithServer) {
            this.exportHelper();
        } else {
            this.setState({ sharedWithServer: true });
            gs_shareWithServer(this.exportHelper);
        }
    }

    exportHelper = () => {
        const { document } = this.props;
        const { page, rectangle, blobs, gridLines, grid, crossword, crosswordClues } = this.state;
        gs_getActiveCell(({ spreadsheetId, sheetId, row, col }) => {
            postJson({
                path: `/documents/${document.id}/export/sheet/${spreadsheetId}/${sheetId}`,
                body: {
                    marker: { row, col },
                    section: { page, rectangle },
                    blobs,
                    gridLines,
                    grid,
                    crossword,
                    crosswordClues,
                },
            }, () => this.setState({ numExporting: this.state.numExporting - 1 }));
        });
    }
}
