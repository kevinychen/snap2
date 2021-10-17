package com.kyc.snap.server;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.kyc.snap.cromulence.CromulenceSolver;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.store.FileStore;
import com.kyc.snap.wikinet.Wikinet;
import com.kyc.snap.words.DictionaryManager;
import com.kyc.snap.words.WordsearchSolver;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.ws.rs.container.ContainerResponseFilter;

public class SnapServer extends Application<Configuration> {

    public static void main(String[] args) throws Exception {
        new SnapServer().run("server");
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
    }

    @Override
    public void run(Configuration configuration, Environment environment) {
        // Allow CORS - this server doesn't store any personal information
        environment.jersey().register(
            (ContainerResponseFilter) (requestContext, responseContext) -> {
                responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
                responseContext.getHeaders().add("Access-Control-Allow-Headers", "*");
            });

        GoogleAPIManager googleApi = new GoogleAPIManager();
        OpenCvManager openCv = new OpenCvManager();
        GridParser gridParser = new GridParser(openCv);
        CrosswordParser crosswordParser = new CrosswordParser();
        DictionaryManager dictionary = new DictionaryManager();
        WordsearchSolver wordsearchSolver = new WordsearchSolver(dictionary);
        CromulenceSolver cromulenceSolver = new CromulenceSolver(dictionary);
        FileStore store = new FileStore();
        Wikinet wikinet = new Wikinet();

        environment.jersey().setUrlPattern("/api/*");

        environment.jersey().register(new MultiPartFeature());
        environment.jersey().register(new WordsResource(wordsearchSolver, crosswordParser, cromulenceSolver, dictionary, wikinet));
        environment.jersey().register(new FileResource(store));
        environment.jersey().register(new DocumentResource(store, googleApi, gridParser));
    }
}
