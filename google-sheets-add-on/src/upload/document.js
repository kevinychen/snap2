import * as classNames from "classnames";
import { get } from "../fetch";
import { DocumentImage } from "./documentImage";
import { FindGridLinesPopup } from "./findGridLinesPopup";

export class Document extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            page: 0,
            mode: "select-rectangle",
            rectangle: undefined,
            gridLines: undefined,
            showFindGridLines: false,
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
        const { imageDataUrl, mode, page, rectangle, gridLines, showFindGridLines } = this.state;

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
                            className={classNames("small-button", { "green": showFindGridLines })}
                            title="Find grid lines"
                            onClick={() => this.setState({ showFindGridLines: !showFindGridLines })}
                            disabled={rectangle === undefined}
                        >
                            {"▒"}
                        </button>

                        {this.maybeRenderFindGridLines()}
                    </div>
                </div>
                <DocumentImage
                    imageDataUrl={imageDataUrl}
                    mode={mode}
                    rectangle={rectangle}
                    gridLines={gridLines}
                    setRectangle={this.setRectangle}
                />
            </div>
        );
    }

    setRectangle = (rectangle) => {
        if (rectangle) {
            this.setState({
                rectangle,
                gridLines: {
                    horizontalLines: [0, rectangle.height],
                    verticalLines: [0, rectangle.width],
                }
            });
        } else {
            this.setState({ rectangle, gridLines: undefined });
        }
    }

    maybeRenderFindGridLines() {
        const { document } = this.props;
        const { page, rectangle, showFindGridLines } = this.state;
        if (showFindGridLines && rectangle) {
            return (
                <FindGridLinesPopup
                    document={document}
                    page={page}
                    rectangle={rectangle}
                    setGridLines={gridLines => this.setState({ gridLines, showFindGridLines: false })}
                />
            );
        }
    }
}
