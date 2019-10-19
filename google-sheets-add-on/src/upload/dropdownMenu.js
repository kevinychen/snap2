import "./dropdownMenu.css"
import * as classNames from "classnames";

export class DropdownMenu extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            isHovering: false,
        }
    }

    render() {
        const { value, children } = this.props;
        const { isHovering } = this.state;
        return (
            <div
                className={classNames("inline", "dropdown-menu-container", { "isHovering": isHovering })}
                onMouseMove={() => this.setState({ isHovering: true })}
                onClick={() => this.setState({ isHovering: false })}
            >
                <span className="dropdown-menu">
                    {value}
                    <div className="dropdown-menu-items-container">
                        {React.Children.map(children, child => <div className="dropdown-menu-item">{child}</div>)}
                    </div>
                </span>
            </div>
        )
    }
}
