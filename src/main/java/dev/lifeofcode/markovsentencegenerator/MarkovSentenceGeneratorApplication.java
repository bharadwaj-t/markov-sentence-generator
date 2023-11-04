package dev.lifeofcode.markovsentencegenerator;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class MarkovSentenceGeneratorApplication implements CommandLineRunner {

	private final MarkovTableVerticle markovTableVerticle;

	public static void main(String[] args) {
		SpringApplication.run(MarkovSentenceGeneratorApplication.class, args);
	}

	public MarkovSentenceGeneratorApplication(MarkovTableVerticle markovTableVerticle) {
		this.markovTableVerticle = markovTableVerticle;
	}

	@Override
	public void run(String... args) throws Exception {
		var vertx = Vertx.vertx();
		vertx.deployVerticle(markovTableVerticle).onFailure(fail -> log.error("failed", fail));
	}
}
