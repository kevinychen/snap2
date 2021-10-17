export function Output({ grid }) {
    if (grid === undefined) {
        return null;
    }
    const { squares } = grid;
    return <table style={{ border: "1px solid black", borderSpacing: 0 }}>
        <tbody>
            {squares.map((row, i) => <tr key={i}>
                {row.map(({ rgb, text, topBorder, rightBorder, bottomBorder, leftBorder }, j) => {
                    return <td
                        key={j}
                        style={{
                            width: 20,
                            height: 20,
                            backgroundColor: '#' + rgb.toString(16).padStart(6, '0'),
                            borderColor: 'black',
                            borderTop: `${borderToHtml(topBorder)}px solid black`,
                            borderRight: `${borderToHtml(rightBorder)}px solid black`,
                            borderBottom: `${borderToHtml(bottomBorder)}px solid black`,
                            borderLeft: `${borderToHtml(leftBorder)}px solid black`,
                        }}
                    >
                        {text}
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
