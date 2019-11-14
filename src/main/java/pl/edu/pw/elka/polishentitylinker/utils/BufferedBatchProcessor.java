package pl.edu.pw.elka.polishentitylinker.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class BufferedBatchProcessor<T> {

    private Consumer<List<T>> consumer;
    private int batchSize;

    private List<T> buffer = new ArrayList<>();

    public BufferedBatchProcessor(Consumer<List<T>> consumer, int batchSize) {
        this.consumer = consumer;
        this.batchSize = batchSize;
    }

    public void process(T item) {
        buffer.add(item);
        if (buffer.size() == batchSize) {
            consumer.accept(buffer);
            buffer.clear();
            log.info(String.format("batch with size %d processed", batchSize));
        }
    }

    public void processRest() {
        consumer.accept(buffer);
        log.info(String.format("rest batch with size %d processed", buffer.size()));
        buffer.clear();
    }
}
