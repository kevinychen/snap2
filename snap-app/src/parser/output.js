import "./output.css";

export function Output({ grid, crosswordFormulas }) {
    if (grid === undefined) {
        return null;
    }
    const { squares } = grid;

    let numRows = squares.length, numCols = 0;

    for (const row of squares) {
        numCols = Math.max(numCols, row.length);
    }

    const formulasByPoint = {};
    if (crosswordFormulas) {
        for (const { row, col, formula, value, clueNumber } of crosswordFormulas) {
            formulasByPoint[`${row}-${col}`] = { formula, value, clueNumber };
            numRows = Math.max(numRows, row + 1);
            numCols = Math.max(numCols, col + 1);
        }
    }

    const tableRows = [];
    for (let row = 0; row < numRows; row++) {
        const rowCells = [];
        for (let col = 0; col < numCols; col++) {
            const cellProps = { key: col };
            let value = undefined;
            const square = squares[row] && squares[row][col];
            if (square !== undefined) {
                const { rgb, text, rightBorder, bottomBorder } = square;
                cellProps.style = {
                    backgroundColor: '#' + rgb.toString(16).padStart(6, '0'),
                    borderRight: `${borderToHtml(rightBorder)}px solid black`,
                    borderBottom: `${borderToHtml(bottomBorder)}px solid black`,
                    width: 20,
                    height: 20,
                };
                value = text;
            }
            const formula = formulasByPoint[`${row}-${col}`];
            if (formula !== undefined) {
                if (formula.formula) {
                    cellProps['data-sheets-formula'] = formula.value;
                } else {
                    cellProps['data-sheets-value'] = `{\"1\":2,\"2\":${JSON.stringify(formula.value)}}`;
                }
                if (formula.clueNumber) {
                    value = formula.clueNumber;
                }
            }
            rowCells.push(<td
                {...cellProps}
            >
                {value}
            </td>);
        }
        tableRows.push(<tr>{rowCells}</tr>);
    }
    return <google-sheets-html-origin>
        <table className='parser-output'>
            <tbody>{tableRows}</tbody>
        </table>
    </google-sheets-html-origin>;
}

function borderToHtml(border) {
    switch (border.style) {
        case 'NONE': return 0;
        case 'THIN': return 1;
        case 'MEDIUM': return 2;
        case 'THICK': return 3;
        default: throw Error();
    }
}
