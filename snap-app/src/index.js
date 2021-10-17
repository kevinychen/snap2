import React from 'react';
import ReactDOM from 'react-dom';
import {
    BrowserRouter as Router,
    Redirect,
    Switch,
    Route,
    Link
} from 'react-router-dom';
import Parser from './parser';
import './index.css';

function Index() {
    return <>
        <div className="title">
            <Link to="/" className="inline"><img src="./wrench.png" alt="Home" /></Link>
            util.in
        </div>
        <div className="block">
            <Link to="/parser"><span className="link">GRID PARSER</span></Link>
            <br />
            Parse grids, text, and crosswords in PDFs and images to Google Sheets
        </div>
    </>;
}

function Header() {
    return <div className="block">
        <Link to="/" className="inline"><img src="./wrench.png" alt="Home" /></Link>
        <Link to="/parser"><span className="inline link">GRID PARSER</span></Link>
    </div>;
}

function App() {
    return <Router>
        <Switch>
            <Route path="/parser">
                <Header />
                <Parser />
            </Route>
            <Route path="/">
                <Index />
            </Route>
            <Route path="/index.html">
                <Redirect to="/" />
            </Route>
            <Route path="/document.html">
                <Redirect to="/parser" />
            </Route>
        </Switch>
    </Router>;
}

ReactDOM.render(
    <React.StrictMode>
        <App />
    </React.StrictMode>,
    document.getElementById('root')
);
