import classNames from "classnames";
import React from "react";
import { get, post, postJson } from "../fetch";
import { DocumentImage } from "./documentImage";
import "./parser.css";

export default class Parser extends React.Component {
    
    constructor(props) {
        super(props);
        this.state = {
            url: '',
            loadingDocument: false,
            document: undefined,

            page: 0,
            imageDataUrl: undefined,
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
        const { url } = this.state;
        return <div className="parser">
            <div className="input">
                <div className="block">
                    <input
                        className="inline"
                        type="text"
                        placeholder="Enter URL of image/PDF/HTML..."
                        style={{ width: "300px" }}
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
            </div>
        </div>;
    }

    maybeRenderToolbar() {
        const { document, mode, imageDimensions, gridLines } = this.state;
        if (document === undefined) {
            return undefined;
        }
        return <div className="block">
            <button
                className="inline"
                onClick={() => this.setRectangle({ x: 0, y: 0, width: imageDimensions.width, height: imageDimensions.height })}
            >
                {"Reset"}
            </button>
            {this.maybeRenderNav()}
            <div className="toolbar_options">
                <span
                    className={classNames({ "selected": mode === "SELECT_REGION" }, "clickable inline")}
                    onClick={() => this.setState({ mode: "SELECT_REGION" })}
                >
                    {"Select region"}
                </span>
                <span
                    className={classNames({ "clickable": gridLines !== undefined, "selected": mode === "EDIT_GRID_LINES" }, "inline")}
                    onClick={() => this.setState({ mode: "EDIT_GRID_LINES" })}
                >
                    {"Edit grid lines"}
                </span>
            </div>
        </div>;
    }

    maybeRenderNav() {
        const { document, page } = this.state;
        if(document.pages.length === 1) {
            return undefined;
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
}
