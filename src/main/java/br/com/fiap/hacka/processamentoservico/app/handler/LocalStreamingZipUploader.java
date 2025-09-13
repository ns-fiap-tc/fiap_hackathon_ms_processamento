package br.com.fiap.processamentoservico.app.handler;

import br.com.fiap.processamentoservice.core.commons.FilePart;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LocalStreamingZipUploader {

    /**
     * Consume a queue of FilePart and persist it as a ZIP locally
     */
    public void persistFileFromQueue(BlockingQueue<FilePart> queue,
                                     String outputPath,
                                     String fileName) throws IOException {

        try (FileOutputStream fos = new FileOutputStream(outputPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zipOut = new ZipOutputStream(bos)) {

            ZipEntry entry = new ZipEntry(fileName);
            zipOut.putNextEntry(entry);

            while (true) {
                FilePart part;
                try {
                    part = queue.take(); // blocks until a part arrives
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Thread interrupted while reading from queue", e);
                }

                if (part.getBytesRead() == -1) {
                    // EOF → finish ZIP
                    break;
                }

                zipOut.write(part.getBytes(), 0, part.getBytesRead());
            }

            zipOut.closeEntry();
        }
    }
}