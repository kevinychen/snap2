import classNames from "classnames";
import React from 'react';
import ReactDOM from 'react-dom';
import GithubCorner from 'react-github-corner';
import {
    BrowserRouter as Router,
    Redirect,
    Switch,
    Route,
    Link
} from 'react-router-dom';
import FindWords from './findwords';
import Parser from './parser';
import Solver from './solver';
import Wordsearch from './wordsearch';
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
            <Link to="/wordsearch"><span className="link">WORD SEARCH</span></Link>
            <br />
            Search for words in a grid (both straight lines and boggle style)
        </div>
        <div className="block">
            <Link to="/solver"><span className="link">HEAVY-DUTY ANAGRAM SOLVER</span></Link>
            <br />
            Solver that uses knowledge of English n-grams and word frequencies to complete and anagram long phrases and sentences (can be used for single words as well)
        </div>
        <div className="block">
            <Link to="/findwords"><span className="link">FIND WORDS</span></Link>
            <br />
            Find words that satisfy one or more various properties
        </div>
    </>;
}

function Header({ mode }) {
    return <div className="block">
        <Link to="/" className="inline"><img src="./wrench.png" alt="Home" /></Link>
        <Link to="/parser"><span className={classNames("inline link", {selected: mode === "parser"})}>GRID PARSER</span></Link>
        <Link to="/wordsearch"><span className={classNames("inline link", {selected: mode === "wordsearch"})}>WORD SEARCH</span></Link>
        <Link to="/solver"><span className={classNames("inline link", {selected: mode === "solver"})}>HEAVY-DUTY ANAGRAM SOLVER</span></Link>
        <Link to="/findwords"><span className={classNames("inline link", {selected: mode === "findwords"})}>FIND WORDS</span></Link>
    </div>;
}

function App() {
    return <Router>
        <GithubCorner href="https://github.com/kevinychen/snap2" />
        <Switch>
            <Route path="/parser">
                <Header mode="parser" />
                <Parser />
            </Route>
            <Route path="/wordsearch">
                <Header mode="wordsearch" />
                <Wordsearch />
            </Route>
            <Route path="/solver">
                <Header mode="solver" />
                <Solver />
            </Route>
            <Route path="/findwords">
                <Header mode="findwords" />
                <FindWords />
            </Route>
            <Route path="/index.html">
                <Redirect to="/" />
            </Route>
            <Route path="/document.html">
                <Redirect to="/parser" />
            </Route>
            <Route path="/wordsearch.html">
                <Redirect to="/wordsearch" />
            </Route>
            <Route path="/solver.html">
                <Redirect to="/solver" />
            </Route>
            <Route path="/findwords.html">
                <Redirect to="/findwords" />
            </Route>
            <Route path="/">
                <Index />
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
