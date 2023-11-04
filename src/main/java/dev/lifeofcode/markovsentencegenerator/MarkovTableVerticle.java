package dev.lifeofcode.markovsentencegenerator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class MarkovTableVerticle extends AbstractVerticle {
    @Getter
    private final Map<String, List<String>> prefixSuffixMap = new HashMap<>();
    private final List<String> keyList = new ArrayList<>();
    private String fileData;
    private final String dataFile;
    private final int prefixLength;
    private final int wordLength;

    public MarkovTableVerticle(@Value("${data.file.loc.cp}") String dataFile,
                               @Value("${prefix.length}") int prefixLength,
                               @Value("${word.length}") int wordLength) {
        this.dataFile = dataFile;
        this.prefixLength = prefixLength;
        this.wordLength = wordLength;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var buffer = Buffer.buffer();
        var fs = vertx.fileSystem();
        var opts = new OpenOptions();
        opts.setCreate(false);

        Future<AsyncFile> file = fs.open( new ClassPathResource(dataFile).getPath(), opts)
                .onFailure(failed -> log.error("Error", failed));

        Future<Long> fileSize = file.compose(AsyncFile::size)
                .onFailure(fail -> log.error("failed to get size", fail));

        Future.all(file, fileSize)
                .onFailure(fail -> {
                    log.error("file read or file size calc failed", fail);
                    startPromise.fail(fail);
                })
                .compose(success -> {
                    AsyncFile fileRead = success.resultAt(0);
                    Long sizeRead = success.resultAt(1);
                    return fileRead.read(buffer, 0, 0, sizeRead.intValue());
                })
                .onSuccess(bufferSuccess -> {
                    this.fileData = bufferSuccess.toString(StandardCharsets.UTF_8);
                    generatePrefixSuffixMap(prefixLength);
                    var response = markovText(prefixLength, wordLength);
                    log.info("{}", response);
                    startPromise.complete();
                });

    }

    public void generatePrefixSuffixMap(int prefixLength) {
        List<String> data = Arrays.stream(fileData.split("\\s+"))
                .map(word -> word.replaceAll("[^A-Za-z0-9]", ""))
                .toList();

        // [hi, my, name, is, alice]
        for(int start = 0; start < data.size(); start++) {
            int prefixWordIndex = start;
            int prefixDistance = start + prefixLength;

            var prefix = new StringBuilder();

            if(prefixDistance > data.size()) {
                break;
            }

            for(; prefixWordIndex < prefixDistance; prefixWordIndex++) {
                prefix.append(data.get(prefixWordIndex));
                prefix.append(" ");
            }

            if(!prefixSuffixMap.containsKey(prefix.toString().trim())) {
                prefixSuffixMap.put(prefix.toString().trim(), new LinkedList<>());
                keyList.add(prefix.toString().trim());
            }

            if(prefixWordIndex >= data.size()) {
                prefixSuffixMap.get(prefix.toString().trim()).add("");
            } else {
                prefixSuffixMap.get(prefix.toString().trim()).add(data.get(prefixDistance));
            }
        }
    }

    public String markovText(int prefixLength, int wordLength) {
        var random = new Random();
        var randomIndex = random.nextInt(keyList.size());

        var key = keyList.get(randomIndex);
        List<String> valueList = prefixSuffixMap.get(key);
        var value = valueList.get(random.nextInt(valueList.size()));

        var markovStringList = new ArrayList<>(Arrays.stream(key.split("\\s+")).toList());
        markovStringList.add(value);


        for (int i = markovStringList.size(); i < wordLength; i++) {
            var newPrefixStartIndex = markovStringList.size() - prefixLength;
            key = String.join(" ",
                    markovStringList.subList(newPrefixStartIndex, markovStringList.size()));

            valueList = prefixSuffixMap.get(key);
            value = valueList.get(random.nextInt(valueList.size()));

            markovStringList.add(value);
        }

        return String.join(" ", markovStringList);
    }
}
