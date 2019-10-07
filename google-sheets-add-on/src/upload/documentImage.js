
export class DocumentImage extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            imageWidth: 0,
            imageHeight: 0,
        };
    }

    componentDidUpdate() {
        const { rectangle, gridLines } = this.props;

        const ctx = this.canvas.getContext('2d');
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        if (rectangle) {
            ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
            ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
            ctx.clearRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            ctx.strokeStyle = 'red';
            ctx.lineWidth = 4;
            ctx.strokeRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);

            if (gridLines) {
                ctx.strokeStyle = 'red';
                ctx.fillStyle = 'red';
                ctx.lineWidth = 2;
                for (var row of gridLines.horizontalLines) {
                    ctx.beginPath();
                    ctx.moveTo(rectangle.x, rectangle.y + row);
                    ctx.lineTo(rectangle.x + rectangle.width, rectangle.y + row);
                    ctx.stroke();
                    this.drawPoint(ctx, rectangle.x, rectangle.y + row);
                    this.drawPoint(ctx, rectangle.x + rectangle.width, rectangle.y + row);
                }
                for (var col of gridLines.verticalLines) {
                    ctx.beginPath();
                    ctx.moveTo(rectangle.x + col, rectangle.y);
                    ctx.lineTo(rectangle.x + col, rectangle.y + rectangle.height);
                    ctx.stroke();
                    this.drawPoint(ctx, rectangle.x + col, rectangle.y);
                    this.drawPoint(ctx, rectangle.x + col, rectangle.y + rectangle.height);
                }
            }
        }
    }

    drawPoint(ctx, x, y) {
        ctx.beginPath();
        ctx.arc(x, y, 8, 0, 2 * Math.PI, true);
        ctx.fill();
    }

    render() {
        const { imageDataUrl } = this.props;
        const { imageWidth, imageHeight } = this.state;
        return (
            <div className="block image-container">
                <img
                    ref={image => this.image = image}
                    className="image"
                    src={imageDataUrl}
                    alt=""
                    onLoad={() => this.setState({ imageWidth: this.image.naturalWidth, imageHeight: this.image.naturalHeight })}
                />
                <canvas
                    ref={this.canvasRef}
                    className="overlay-canvas"
                    width={imageWidth}
                    height={imageHeight}
                />
            </div>
        )
    }

    canvasRef = canvas => {
        this.canvas = canvas;
        if (canvas) {
            this.canvas.addEventListener('mousedown', this.mouseDown);
            this.canvas.addEventListener('mousemove', this.mouseMove);
            this.canvas.addEventListener('mouseup', this.mouseUp);
        } else {
            this.canvas.removeEventListener('mousedown', this.mouseDown);
            this.canvas.removeEventListener('mousemove', this.mouseMove);
            this.canvas.removeEventListener('mouseup', this.mouseUp);
        }
    }

    mouseDown = e => {
        this.updateMouseLoc(e);
    }

    mouseMove = e => {
        if (this.mouseDownLoc) {
            this.updateMouseLoc(e);
        }
    }

    mouseUp = e => {
        this.updateMouseLoc(e);

        const { mode } = this.props;
        if (mode === "select-rectangle") {
            ;
        } else if (mode === "edit-grid-lines") {
            // TODO
        } else if (mode === "select-blob") {
            // TODO
        }
        this.mouseDownLoc = this.mouseEndLoc = undefined;
    }

    updateMouseLoc = e => {
        const { mode, setRectangle } = this.props;
        const xRatio = this.canvas.scrollWidth / this.canvas.width;
        const yRatio = this.canvas.scrollHeight / this.canvas.height;
        this.mouseEndLoc = { x: e.offsetX / xRatio, y: e.offsetY / yRatio };
        if (this.mouseDownLoc === undefined) {
            this.mouseDownLoc = this.mouseEndLoc;
        }
        if (mode === "select-rectangle") {
            setRectangle({
                x: Math.min(this.mouseDownLoc.x, this.mouseEndLoc.x),
                y: Math.min(this.mouseDownLoc.y, this.mouseEndLoc.y),
                width: Math.abs(this.mouseDownLoc.x - this.mouseEndLoc.x),
                height: Math.abs(this.mouseDownLoc.y - this.mouseEndLoc.y),
            });
        } else if (mode === "edit-grid-lines") {
            // TODO
        } else if (mode === "select-blob") {
            // TODO
        }
    };
}
