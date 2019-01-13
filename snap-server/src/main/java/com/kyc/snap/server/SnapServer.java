package com.kyc.snap.server;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.kyc.snap.cromulence.CromulenceSolver;
import com.kyc.snap.cromulence.LowLevelCromulenceSolver;
import com.kyc.snap.cromulence.NiceCromulenceSolver;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.store.FileStore;
import com.kyc.snap.words.DictionaryManager;
import com.kyc.snap.words.WordsearchSolver;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class SnapServer extends Application<SnapConfiguration> {

    public static void main(String[] args) throws Exception {
        new SnapServer().run("server", "config.yml");
    }

    @Override
    public void initialize(Bootstrap<SnapConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    }

    @Override
    public void run(SnapConfiguration configuration, Environment environment) throws Exception {
        GoogleAPIManager googleApi = new GoogleAPIManager();
        OpenCvManager openCv = new OpenCvManager();
        GridParser gridParser = new GridParser(openCv, googleApi);
        CrosswordParser crosswordParser = new CrosswordParser();
        DictionaryManager dictionary = new DictionaryManager();
        WordsearchSolver wordsearchSolver = new WordsearchSolver(dictionary);
        NiceCromulenceSolver cromulenceSolver = new NiceCromulenceSolver(new CromulenceSolver(new LowLevelCromulenceSolver(dictionary)));
        FileStore store = new FileStore();

        environment.jersey().setUrlPattern("/api/*");

        environment.jersey().register(new MultiPartFeature());
        environment.jersey().register(new ImageResource(configuration, googleApi, gridParser, crosswordParser));
        environment.jersey().register(new WordsResource(wordsearchSolver, crosswordParser, cromulenceSolver));
        environment.jersey().register(new FileResource(store));
        environment.jersey().register(new DocumentResource(store, googleApi, gridParser));
    }
}
