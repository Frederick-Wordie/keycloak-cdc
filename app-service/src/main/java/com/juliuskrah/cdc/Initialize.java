package com.juliuskrah.cdc;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.runtime.event.annotation.EventListener;
import io.reactiverse.reactivex.pgclient.PgPool;

/**
 * Initializes the database
 * 
 * @author Julius Krah
 */
@Singleton
public class Initialize {
    private final PgPool client;
    private static final Logger log = LoggerFactory.getLogger(Initialize.class);

    public Initialize(PgPool client) {
        this.client = client;
    }

    /**
     * Creates the database tables on application start
     */
    @EventListener
    public void onStartUp(StartupEvent event) {
        log.info("Initializing the database");
        ResourceLoader loader = new ResourceResolver().getLoader(ClassPathResourceLoader.class).get();
        Optional<URL> url = loader.getResource("classpath:schema.sql");
        if (url.isPresent()) {
            try {
                var sql = new String(Files.readAllBytes(Paths.get(url.get().toURI())));
                log.debug("SQL: {}", sql);
                client.query(sql, ar -> {
                    if(ar.succeeded()) {
                        var rows = ar.result();
                        log.info("{} rows updated", rows.rowCount());
                    } else {
                        log.error("Could not execute statement", ar.cause());
                    }
                });
            } catch (IOException | URISyntaxException e) {
               log.error("unable to read file", e);
            }
        }    
    }
}