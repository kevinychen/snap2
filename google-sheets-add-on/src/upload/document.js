import * as classNames from "classnames";
import { get, postJson } from "../fetch";
import { DocumentImage } from "./documentImage";
import { ParseGridLinesPopup } from "./parseGridLinesPopup";
import { ParseContentPopup } from "./parseContentPopup";
import { ParseCrosswordCluesPopup } from "./parseCrosswordCluesPopup";
import { ExportToSheetPopup } from "./exportToSheetPopup";
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
            gridLines: undefined,
            gridPosition: undefined,
            grid: undefined,
            crossword: undefined,
            crosswordClues: undefined,
            popupMode: undefined,
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
            popupMode } = this.state;

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
                    <DropdownMenu value={this.maybeBold("Parse", navBarMode === "PARSE")}>
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
                                }, response => this.setCrossword(response.crossword));
                            }}
                        >
                            Crossword
                        </div>
                        <div
                            className="clickable"
                            onClick={() => this.setState({ navBarMode: "PARSE", popupMode: "PARSE_CLUES"})}
                        >
                            Crossword clues
                        </div>
                    </DropdownMenu>
                    <DropdownMenu value={this.maybeBold("Export", navBarMode === "EXPORT")}>
                        <div
                            className={classNames({"clickable": rectangle !== undefined})}
                            onClick={() => this.setState({ navBarMode: "EXPORT", popupMode: "EXPORT_TO_SHEET" })}
                        >
                            To sheet
                        </div>
                    </DropdownMenu>

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
                    <ParseCrosswordCluesPopup
                        isVisible={popupMode === "PARSE_CLUES"}
                        document={document}
                        {...this.state}
                        setCrosswordClues={this.setCrosswordClues}
                        exit={this.clearPopupMode}
                    />
                    <ExportToSheetPopup
                        isVisible={popupMode === "EXPORT_TO_SHEET"}
                        document={document}
                        {...this.state}
                        exit={this.clearPopupMode}
                    />
                </div>
                <DocumentImage
                    {...this.state}
                    setImageDimensions={this.setImageDimensions}
                    setRectangle={this.setRectangle}
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

    setImageDimensions = imageDimensions => {
        this.setRectangle(undefined);
        this.setState({ imageDimensions })
    }

    setRectangle = rectangle => {
        if (rectangle === undefined) {
            this.setGridLines(undefined);
            this.setState({ rectangle });
        } else {
            this.setState({ rectangle });
            this.setGridLines({
                horizontalLines: [0, rectangle.height],
                verticalLines: [0, rectangle.width],
            });
        }
    }

    setGridLines = gridLines => {
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
}
