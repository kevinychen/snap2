import classNames from "classnames";
import React from 'react';
import ReactDOM from "react-dom/client";
import GithubCorner from 'react-github-corner';
import {
    BrowserRouter as Router,
    Navigate,
    Routes,
    Route,
    Link,
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
            Parse grids and crosswords in images and PDFs
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
        <Routes>
            <Route path="/parser" element={<><Header mode="parser" /><Parser /></>} />
            <Route path="/wordsearch" element={<><Header mode="wordsearch" /><Wordsearch /></>} i/>
            <Route path="/solver" element={<><Header mode="solver" /><Solver /></>}/>
            <Route path="/findwords" element={<><Header mode="findwords" /><FindWords /></>} />
            <Route path="/index.html" element={<Navigate to="/" />} />
            <Route path="/document.html" element={<Navigate to="/parser" />} />
            <Route path="/wordsearch.html" element={<Navigate to="/wordsearch" />} />
            <Route path="/solver.html" element={<Navigate to="/solver" />} />
            <Route path="/findwords.html" element={<Navigate to="/findwords" />} />
            <Route path="/" element={<Index />} />
        </Routes>
    </Router>;
}

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);
