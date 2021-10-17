import classNames from "classnames";
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
import Solver from './solver';
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
        <div className="block">
            <Link to="/solver"><span className="link">HEAVY-DUTY ANAGRAM SOLVER</span></Link>
            <br />
            Solver that uses knowledge of English n-grams and word frequencies to complete and anagram long phrases and sentences (can be used for single words as well)
        </div>
    </>;
}

function Header({ mode }) {
    return <div className="block">
        <Link to="/" className="inline"><img src="./wrench.png" alt="Home" /></Link>
        <Link to="/parser"><span className={classNames("inline link", {selected: mode === "parser"})}>GRID PARSER</span></Link>
        <Link to="/solver"><span className={classNames("inline link", {selected: mode === "solver"})}>HEAVY-DUTY ANAGRAM SOLVER</span></Link>
    </div>;
}

function App() {
    return <Router>
        <Switch>
            <Route path="/parser">
                <Header mode="parser" />
                <Parser />
            </Route>
            <Route path="/solver">
                <Header mode="solver" />
                <Solver />
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
            <Route path="/solver.html">
                <Redirect to="/solver" />
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
