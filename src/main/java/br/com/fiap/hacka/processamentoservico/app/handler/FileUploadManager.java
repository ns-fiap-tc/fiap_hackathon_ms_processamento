package br.com.fiap.processamentoservico.app.handler;

import br.com.fiap.processamentoservice.core.commons.FilePart;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class FileUploadManager {

    private final LocalStreamingZipUploader uploader;
    private final Map<String, BlockingQueue<FilePart>> fileQueues = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final String outputDir;

    public FileUploadManager(LocalStreamingZipUploader uploader, String outputDir) {
        this.uploader = uploader;
        this.outputDir = outputDir;
    }

    /**
     * Handle a new chunk from RabbitMQ
     */
    public void handleFilePart(FilePart part) throws InterruptedException {
        String fileName = part.getFileName();

        // Create a queue for this file if it does not exist
        fileQueues.computeIfAbsent(fileName, fn -> {
            BlockingQueue<FilePart> queue = new LinkedBlockingQueue<>();

            // Start background thread for this file
            executor.submit(() -> {
                try {
                    String outputPath = outputDir + "/" + fileName + ".zip";
                    uploader.persistFileFromQueue(queue, outputPath, fileName);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to persist file: " + fileName, e);
                } finally {
                    // Cleanup after done
                    fileQueues.remove(fileName);
                }
            });

            return queue;
        });

        // Push chunk into the file's queue
        fileQueues.get(fileName).put(part);
    }

    public void shutdown() {
        executor.shutdown();
    }
}