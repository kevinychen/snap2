package com.kyc.snap.server;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.solver.EnglishModel;
import com.kyc.snap.solver.PregexSolver;
import com.kyc.snap.store.FileStore;
import com.kyc.snap.words.EnglishDictionary;
import com.kyc.snap.words.WordSearchSolver;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.ws.rs.container.ContainerResponseFilter;

public class SnapServer extends Application<Configuration> {

    public static void main(String[] args) throws Exception {
        new SnapServer().run("server");
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new SinglePageAppAssetsBundle("/assets", "/", "index.html"));
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
        EnglishDictionary dictionary = new EnglishDictionary();
        WordSearchSolver wordSearchSolver = new WordSearchSolver();
        PregexSolver pregexSolver = new PregexSolver(new EnglishModel(dictionary));
        FileStore store = new FileStore();

        environment.jersey().setUrlPattern("/api/*");

        environment.jersey().register(new MultiPartFeature());
        environment.jersey().register(new WordsResource(wordSearchSolver, crosswordParser, pregexSolver, dictionary));
        environment.jersey().register(new FileResource(store));
        environment.jersey().register(new DocumentResource(store, googleApi, gridParser, crosswordParser));
    }

    static class SinglePageAppAssetsBundle extends AssetsBundle {

        SinglePageAppAssetsBundle(String resourcePath, String uriPath, String indexFile) {
            super(resourcePath, uriPath, indexFile);
        }

        @Override
        protected AssetServlet createServlet() {
            return new SinglePageAppAssetsServlet(getResourcePath(), getUriPath(), getIndexFile(), StandardCharsets.UTF_8);
        }
    }

    static class SinglePageAppAssetsServlet extends AssetServlet {

        SinglePageAppAssetsServlet(String resourcePath, String uriPath, String indexFile, Charset defaultCharset) {
            super(resourcePath, uriPath, indexFile, defaultCharset);
        }

        @Override
        protected URL getResourceUrl(String absoluteRequestedResourcePath) {
            // Redirect any frontend managed routes to root
            if (absoluteRequestedResourcePath.matches("assets/[^.]+(\\.html)?")) {
                return super.getResourceUrl("assets/index.html");
            }
            else
                return super.getResourceUrl(absoluteRequestedResourcePath);
        }
    }
}
