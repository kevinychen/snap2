import * as classNames from "classnames";
import { get } from "../fetch";
import { DocumentImage } from "./documentImage";
import { FindGridLinesPopup } from "./findGridLinesPopup";
import { ParseContentPopup } from "./parseContentPopup";
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
            popupMode: undefined,
        };
    }

    componentDidMount() {
        this.fetchImage(this.props.document.pages[this.state.page].imageId);
    }

    componentDidUpdate(prevProps, prevState) {
        const { imageId } = this.props.document.pages[this.state.page];
        const { imageId: prevImageId } = prevProps.document.pages[prevState.page];
        if (imageId !== prevImageId) {
            this.fetchImage(imageId);
        }
    }

    fetchImage(imageId) {
        get({ path: `/files/${imageId}` }, response => {
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
                    </DropdownMenu>

                    <FindGridLinesPopup
                        isVisible={popupMode === "PARSE_GRID_LINES"}
                        document={document}
                        {...this.state}
                        cancel={this.clearPopupMode}
                        setGridLines={gridLines => {
                            this.clearPopupMode();
                            this.setGridLines(gridLines);
                        }}
                    />
                    <ParseContentPopup
                        isVisible={popupMode === "PARSE_CONTENT"}
                        document={document}
                        {...this.state}
                        cancel={this.clearPopupMode}
                        setGrid={({ gridPosition, grid }) => {
                            this.clearPopupMode();
                            this.setGrid(gridPosition, grid);
                        }}
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
        this.setState({ imageDimensions })
        this.setRectangle(undefined);
    }

    setRectangle = rectangle => {
        this.setState({ rectangle });
        this.setGridLines(rectangle === undefined ? undefined : {
            horizontalLines: [0, rectangle.height],
            verticalLines: [0, rectangle.width],
        });
    }

    setGridLines = gridLines => {
        this.setState({ gridLines });
        this.setGrid(undefined, undefined);
    }

    setGrid = (gridPosition, grid) => {
        this.setState({ gridPosition, grid });
    }

    clearPopupMode = () => {
        this.setState({ popupMode: undefined });
    }
}
