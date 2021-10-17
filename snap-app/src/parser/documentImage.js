import React from "react";
import "./documentImage.css";

export class DocumentImage extends React.Component {

    static EDIT_GRID_LINE_BUFFER = 5;

    constructor(props) {
        super(props);
        this.state = {
            editGridLinesDirection: "COL",
            editGridLinesHoveredOver: undefined,
            editCrosswordHoveredOverGridRow: undefined,
            editCrosswordHoveredOverGridCol: undefined,
        };
    }

    componentDidUpdate() {
        const { imageDimensions, rectangle, blobs, gridLines, gridPosition, crossword } = this.props;
        const { editGridLinesHoveredOver } = this.state;

        const ctx = this.canvas.getContext('2d');
        ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        if (rectangle) {
            ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
            ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
            ctx.clearRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            ctx.strokeStyle = 'black';
            ctx.lineWidth = 4;
            ctx.strokeRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }

        if (blobs) {
            for (let i = 0; i < blobs.length; i++) {
                const color = 'hsl(' + (360 * i / blobs.length) + ', 100%, 50%)';
                ctx.strokeStyle = color;
                ctx.fillStyle = color;
                for (const p of blobs[i].fencePoints) {
                    ctx.fillRect(rectangle.x + p.x, rectangle.y + p.y, 2, 2);
                }
            }
        }

        if (rectangle && gridLines) {
            ctx.strokeStyle = 'red';
            ctx.fillStyle = 'red';
            ctx.lineWidth = Math.ceil((rectangle.width + rectangle.height) / 1000);
            for (const row of gridLines.horizontalLines) {
                ctx.beginPath();
                ctx.moveTo(rectangle.x, rectangle.y + row);
                ctx.lineTo(rectangle.x + rectangle.width, rectangle.y + row);
                ctx.stroke();
            }
            for (const col of gridLines.verticalLines) {
                ctx.beginPath();
                ctx.moveTo(rectangle.x + col, rectangle.y);
                ctx.lineTo(rectangle.x + col, rectangle.y + rectangle.height);
                ctx.stroke();
            }
            if (editGridLinesHoveredOver) {
                ctx.strokeStyle = 'green';
                ctx.fillStyle = 'green';
                ctx.lineWidth = 2;
                const { type, value } = editGridLinesHoveredOver;
                if (type === "ROW") {
                    ctx.beginPath();
                    ctx.moveTo(rectangle.x, rectangle.y + value);
                    ctx.lineTo(rectangle.x + rectangle.width, rectangle.y + value);
                    ctx.stroke();
                } else {
                    ctx.beginPath();
                    ctx.moveTo(rectangle.x + value, rectangle.y);
                    ctx.lineTo(rectangle.x + value, rectangle.y + rectangle.height);
                    ctx.stroke();
                }
            }
        }

        if (crossword) {
            ctx.font = "18px Helvetica";
            ctx.strokeStyle = 'blue';
            const widthRatio = this.canvas.width / imageDimensions.width;
            const heightRatio = this.canvas.height / imageDimensions.height;
            for (const entry of crossword.entries) {
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

    render() {
        const { imageDataUrl, imageDimensions, setImageDimensions } = this.props;
        return (
            <div
                className="block image-container"
                style={{ cursor: this.getCursor() }}
                onContextMenu={e => e.preventDefault()}
            >
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

    getCursor() {
        const { mode } = this.props;
        const { editGridLinesDirection, editGridLinesHoveredOver } = this.state;
        if (mode === "SELECT_REGION") {
            return "crosshair";
        } else if (mode === "EDIT_GRID_LINES") {
            if (editGridLinesHoveredOver) {
                return "no-drop";
            }
            return editGridLinesDirection === "ROW" ? "ew-resize" : "ns-resize";
        }
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
        const { mode } = this.props;
        const { editGridLinesDirection } = this.state;
        this.updateMouseWhileClicked(e);
        if (mode === "EDIT_GRID_LINES") {
            if (e.button === 2 || e.ctrlKey || e.altKey || e.metaKey) {
                this.setState({ editGridLinesDirection: editGridLinesDirection === "ROW" ? "COL" : "ROW" });
            } else {
                this.updateGridLine(e);
            }
        } else if (mode === "EDIT_GRID_CROSSWORD") {
            this.updateCrossword(e);
        }
    };

    mouseMove = e => {
        if (this.mouseDownLoc) {
            this.updateMouseWhileClicked(e);
        } else {
            this.updateMouse(e);
        }
    };

    mouseUp = e => {
        this.updateMouseWhileClicked(e);
        this.mouseDownLoc = this.mouseEndLoc = undefined;
    };

    updateMouseWhileClicked = e => {
        if (!this.canvas) {
            return;
        }
        const { mode, setRectangle } = this.props;
        const xRatio = this.canvas.scrollWidth / this.canvas.width;
        const yRatio = this.canvas.scrollHeight / this.canvas.height;
        this.mouseEndLoc = { x: e.offsetX / xRatio, y: e.offsetY / yRatio };
        if (this.mouseDownLoc === undefined) {
            this.mouseDownLoc = this.mouseEndLoc;
        }
        if (mode === "SELECT_REGION") {
            setRectangle({
                x: Math.min(this.mouseDownLoc.x, this.mouseEndLoc.x),
                y: Math.min(this.mouseDownLoc.y, this.mouseEndLoc.y),
                width: Math.max(Math.abs(this.mouseDownLoc.x - this.mouseEndLoc.x), 1),
                height: Math.max(Math.abs(this.mouseDownLoc.y - this.mouseEndLoc.y), 1),
            });
        }
        this.updateMouse(e);
    };

    updateMouse = e => {
        const { mode } = this.props;
        if (mode === "EDIT_GRID_LINES") {
            this.setState({ editGridLinesHoveredOver: this.findHoveredOverGridLine(e) });
        } else if (mode === "EDIT_GRID_CROSSWORD") {
            this.setState({
                editCrosswordHoveredOverGridRow: this.findHoveredOverGridRow(e),
                editCrosswordHoveredOverGridCol: this.findHoveredOverGridCol(e),
            });
        }
    };

    updateGridLine = e => {
        if (!this.canvas) {
            return;
        }
        const { rectangle, gridLines, setGridLines } = this.props;
        const { editGridLinesDirection, editGridLinesHoveredOver } = this.state;
        const xRatio = this.canvas.scrollWidth / this.canvas.width;
        const yRatio = this.canvas.scrollHeight / this.canvas.height;
        const copiedGridLines = {
            horizontalLines: [...gridLines.horizontalLines],
            verticalLines: [...gridLines.verticalLines],
        };
        if (editGridLinesHoveredOver) {
            const { type, value } = editGridLinesHoveredOver;
            const ref = type === "ROW" ? copiedGridLines.horizontalLines : copiedGridLines.verticalLines;
            ref.splice(ref.indexOf(value), 1);
        } else {
            if (editGridLinesDirection === "ROW") {
                copiedGridLines.horizontalLines.push(e.offsetY / yRatio - rectangle.y);
            } else {
                copiedGridLines.verticalLines.push(e.offsetX / xRatio - rectangle.x);
            }
        }
        setGridLines(copiedGridLines);
    };

    updateCrossword = e => {
        const { crossword, setCrossword } = this.props;
        const { editCrosswordHoveredOverGridRow, editCrosswordHoveredOverGridCol } = this.state;
        const newEntries = [];
        for (const entry of crossword.entries) {
            if (entry.startRow > editCrosswordHoveredOverGridRow) {
                newEntries.push({ ...entry, clueNumber: entry.clueNumber - 1 });
            } else if (entry.startRow < editCrosswordHoveredOverGridRow) {
                newEntries.push(entry);
            } else if (entry.startCol > editCrosswordHoveredOverGridCol) {
                newEntries.push({ ...entry, clueNumber: entry.clueNumber - 1 });
            } else if (entry.startCol < editCrosswordHoveredOverGridCol) {
                newEntries.push(entry);
            }
        }
        setCrossword({ ...crossword, entries: newEntries });
    };

    findHoveredOverGridLine = e => {
        if (!this.canvas) {
            return;
        }
        const { rectangle, gridLines } = this.props;
        const xRatio = this.canvas.scrollWidth / this.canvas.width;
        const yRatio = this.canvas.scrollHeight / this.canvas.height;
        for (const row of gridLines.horizontalLines) {
            if (Math.abs(e.offsetY - yRatio * (rectangle.y + row)) < DocumentImage.EDIT_GRID_LINE_BUFFER) {
                return { type: "ROW", value: row };
            }
        }
        for (const col of gridLines.verticalLines) {
            if (Math.abs(e.offsetX - xRatio * (rectangle.x + col)) < DocumentImage.EDIT_GRID_LINE_BUFFER) {
                return { type: "COL", value: col };
            }
        }
    };

    findHoveredOverGridRow = e => {
        if (!this.canvas) {
            return;
        }
        const { gridPosition } = this.props;
        const yRatio = this.canvas.scrollHeight / this.canvas.height;
        for (const [i, row] of gridPosition.rows.entries()) {
            if (e.offsetY / yRatio >= row.startY && e.offsetY / yRatio < row.startY + row.height) {
                return i;
            }
        }
    };

    findHoveredOverGridCol = e => {
        if (!this.canvas) {
            return;
        }
        const { gridPosition } = this.props;
        const xRatio = this.canvas.scrollWidth / this.canvas.width;
        for (const [i, col] of gridPosition.cols.entries()) {
            if (e.offsetX / xRatio >= col.startX && e.offsetX / xRatio < col.startX + col.width) {
                return i;
            }
        }
    };
}