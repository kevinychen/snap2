import * as classNames from "classnames";
import { get } from "../fetch";
import { DocumentImage } from "./documentImage";
import { FindGridLinesPopup } from "./findGridLinesPopup";
import { ParseContentPopup } from "./parseContentPopup";

export class Document extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            page: 0,
            mode: "select-rectangle",
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
        const { imageDataUrl, mode, page, rectangle, gridLines, gridPosition, grid, popupMode } = this.state;

        return (
            <div className="block">
                <div className="block">
                    <button
                        onClick={() => this.setState({ page: page - 1 })}
                        disabled={page == 0}
                    >
                        {"<"}
                    </button>
                    Page {page + 1}/{document.pages.length}
                    <button
                        className="inline"
                        onClick={() => this.setState({ page: page + 1 })}
                        disabled={page == document.pages.length - 1}
                    >
                        {">"}
                    </button>

                    <button
                        className={classNames("small-button", { "green": mode === "select-rectangle" })}
                        title="Select rectangular region"
                        onClick={() => this.setState({ mode: "select-rectangle" })}
                    >
                        {"⬚"}
                    </button>
                    <button
                        className={classNames("small-button", { "green": mode === "edit-grid-lines" })}
                        title="Add or remove grid lines"
                        onClick={() => this.setState({ mode: "edit-grid-lines" })}
                        disabled={rectangle === undefined}
                    >
                        {"╋"}
                    </button>
                    <button
                        className={classNames("small-button", { "green": mode === "select-blob" })}
                        title="Select an arbitrary shape"
                        onClick={() => this.setState({ mode: "select-blob" })}
                    >
                        {"⬯"}
                    </button>

                    <div className="inline popup-bar">
                        <button
                            className={classNames("small-button", { "green": popupMode === "find-grid-lines" })}
                            title="Find grid lines"
                            onClick={() => this.setState({ popupMode: popupMode === "find-grid-lines" ? undefined : "find-grid-lines" })}
                            disabled={rectangle === undefined}
                        >
                            {"▒"}
                        </button>
                        <button
                            className={classNames("small-button", { "green": popupMode === "parse-content" })}
                            title="Find grid lines"
                            onClick={() => this.setState({ popupMode: popupMode === "parse-content" ? undefined : "parse-content" })}
                            disabled={rectangle === undefined}
                        >
                            {"🄰"}
                        </button>

                        <FindGridLinesPopup
                            isVisible={popupMode === "find-grid-lines" && rectangle}
                            document={document}
                            page={page}
                            rectangle={rectangle}
                            setGridLines={gridLines => this.setState({ gridLines, grid: undefined, popupMode: undefined })}
                        />
                        <ParseContentPopup
                            isVisible={popupMode === "parse-content" && gridLines}
                            document={document}
                            page={page}
                            rectangle={rectangle}
                            gridLines={gridLines}
                            setGrid={({ gridPosition, grid }) => this.setState({ gridPosition, grid, popupMode: undefined })}
                        />
                    </div>
                </div>
                <DocumentImage
                    imageDataUrl={imageDataUrl}
                    mode={mode}
                    rectangle={rectangle}
                    gridLines={gridLines}
                    gridPosition={gridPosition}
                    grid={grid}
                    setRectangle={this.setRectangle}
                />
            </div>
        );
    }

    setRectangle = (rectangle) => {
        this.setState({
            rectangle,
            gridLines: {
                horizontalLines: [0, rectangle.height],
                verticalLines: [0, rectangle.width],
            },
            grid: undefined,
        });
    }
}
