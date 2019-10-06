import { get } from "../fetch";

export class Document extends React.Component {
    constructor(props) {
        super(props);
        this.state = { page: 0 };
        this.imageRef = React.createRef();
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
        const { imageDataUrl, imageHeight, imageWidth, page } = this.state;

        return (
            <div>
                <div>
                    <button
                        className="inline"
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
                </div>
                <div className="image-container">
                    <img
                        className="image"
                        src={imageDataUrl}
                        alt=""
                        onLoad={this.setImageSize}
                    />
                    <canvas
                        ref={this.imageRef}
                        className="overlay-canvas"
                        width={imageWidth}
                        height={imageHeight}
                    />
                </div>
            </div>
        );
    }

    setImageSize = () => {
        this.setState({
            imageWidth: this.imageRef.naturalWidth,
            imageHeight: this.imageRef.naturalHeight,
        })
    }
}
