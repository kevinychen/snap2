import "./documentImage.css";

export class DocumentImage extends React.Component {

    constructor(props) {
        super(props);
    }

    componentDidUpdate() {
        const { imageDimensions, rectangle, gridLines, gridPosition, grid, crossword } = this.props;

        const ctx = this.canvas.getContext('2d');
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        if (rectangle) {
            ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
            ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
            ctx.clearRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            ctx.strokeStyle = 'red';
            ctx.lineWidth = 4;
            ctx.strokeRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }

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

        if (grid) {
            for (var i = 0; i < grid.numRows; i++) {
                for (var j = 0; j < grid.numCols; j++) {
                    const row = gridPosition.rows[i];
                    const col = gridPosition.cols[j];
                    const square = grid.squares[i][j];

                    ctx.beginPath();
                    ctx.rect(
                        rectangle.x + col.startX + col.width / 3,
                        rectangle.y + row.startY + row.height / 3,
                        col.width / 3,
                        row.height / 3);
                    ctx.fillStyle = this.rgbToStyle(square.rgb);
                    ctx.fill();
                    ctx.strokeStyle = 'red';
                    ctx.lineWidth = 2;
                    ctx.stroke();

                    ctx.strokeStyle = 'blue';
                    ctx.strokeText(square.text,
                        rectangle.x + col.startX + col.width / 3,
                        rectangle.y + row.startY + 2 * row.height / 3);

                    ctx.fillStyle = this.rgbToStyle(square.topBorder.rgb);
                    ctx.fillRect(
                        rectangle.x + col.startX + col.width / 3,
                        rectangle.y + row.startY + row.height / 3 - 1,
                        col.width / 3,
                        -square.topBorder.width);
                    ctx.fillStyle = this.rgbToStyle(square.rightBorder.rgb);
                    ctx.fillRect(
                        rectangle.x + col.startX + col.width * 2 / 3 + 1,
                        rectangle.y + row.startY + row.height / 3,
                        square.rightBorder.width,
                        row.height / 3);
                    ctx.fillStyle = this.rgbToStyle(square.bottomBorder.rgb);
                    ctx.fillRect(
                        rectangle.x + col.startX + col.width / 3,
                        rectangle.y + row.startY + row.height * 2 / 3 + 1,
                        col.width / 3,
                        square.bottomBorder.width);
                    ctx.fillStyle = this.rgbToStyle(square.leftBorder.rgb);
                    ctx.fillRect(
                        rectangle.x + col.startX + col.width / 3 - 1,
                        rectangle.y + row.startY + row.height / 3,
                        -square.leftBorder.width,
                        row.height / 3);
                }
            }
        }

        if (crossword) {
            ctx.font = "18px Helvetica";
            ctx.strokeStyle = 'blue';
            const widthRatio = this.canvas.width / imageDimensions.width;
            const heightRatio = this.canvas.height / imageDimensions.height;
            for (var entry of crossword.entries) {
                const row = gridPosition.rows[entry.startRow];
                const col = gridPosition.cols[entry.startCol];
                ctx.strokeText(entry.clueNumber,
                    widthRatio * (col.startX + col.width / 3),
                    heightRatio * (row.startY + row.height * 2 / 3));
            }
        }
    }

    rgbToStyle(rgb) {
        return 'rgb(' + ((rgb >> 16) & 0xff) + ',' + ((rgb >> 8) & 0xff) + ',' + ((rgb >> 0) & 0xff) + ')';
    }

    drawPoint(ctx, x, y) {
        ctx.beginPath();
        ctx.arc(x, y, 8, 0, 2 * Math.PI, true);
        ctx.fill();
    }

    render() {
        const { imageDataUrl, imageDimensions, setImageDimensions } = this.props;
        return (
            <div className="block image-container">
                <img
                    ref={image => this.image = image}
                    className="image"
                    src={imageDataUrl}
                    alt=""
                    onLoad={() => setImageDimensions({ width: this.image.naturalWidth, height: this.image.naturalHeight })}
                />
                <canvas
                    ref={this.canvasRef}
                    className="overlay-canvas"
                    width={imageDimensions.width}
                    height={imageDimensions.height}
                />
            </div>
        )
    }

    canvasRef = canvas => {
        if (this.canvas) {
            this.canvas.removeEventListener('mousedown', this.mouseDown);
            this.canvas.removeEventListener('mousemove', this.mouseMove);
            this.canvas.removeEventListener('mouseup', this.mouseUp);
        }
        this.canvas = canvas;
        if (this.canvas) {
            this.canvas.addEventListener('mousedown', this.mouseDown);
            this.canvas.addEventListener('mousemove', this.mouseMove);
            this.canvas.addEventListener('mouseup', this.mouseUp);
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
        this.mouseDownLoc = this.mouseEndLoc = undefined;
    }

    updateMouseLoc = e => {
        const { navBarMode, mode, setRectangle } = this.props;
        const xRatio = this.canvas.scrollWidth / this.canvas.width;
        const yRatio = this.canvas.scrollHeight / this.canvas.height;
        this.mouseEndLoc = { x: e.offsetX / xRatio, y: e.offsetY / yRatio };
        if (this.mouseDownLoc === undefined) {
            this.mouseDownLoc = this.mouseEndLoc;
        }
        if (navBarMode === "SELECT" && mode === "RECTANGLE") {
            setRectangle({
                x: Math.min(this.mouseDownLoc.x, this.mouseEndLoc.x),
                y: Math.min(this.mouseDownLoc.y, this.mouseEndLoc.y),
                width: Math.abs(this.mouseDownLoc.x - this.mouseEndLoc.x),
                height: Math.abs(this.mouseDownLoc.y - this.mouseEndLoc.y),
            });
        }
    };
}
