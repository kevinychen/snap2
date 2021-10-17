import "./output.css";

export function Output({ grid, crossword }) {
    if (grid === undefined) {
        return null;
    }
    const { squares } = grid;

    const crosswordNums = {};
    if (crossword) {
        for (const { startRow, startCol, clueNumber } of crossword.entries) {
            crosswordNums[`${startRow}-${startCol}`] = clueNumber;
        }
    }

    return <table className='parser-output'>
        <tbody>
            {squares.map((row, i) => <tr key={i}>
                {row.map(({ rgb, text, rightBorder, bottomBorder }, j) => {
                    return <td
                        key={j}
                        style={{
                            backgroundColor: '#' + rgb.toString(16).padStart(6, '0'),
                            borderRight: `${borderToHtml(rightBorder)}px solid black`,
                            borderBottom: `${borderToHtml(bottomBorder)}px solid black`,
                        }}
                    >
                        {crosswordNums[`${i}-${j}`] || text}
                    </td>;
                })}
            </tr>)}
        </tbody>
    </table>
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
