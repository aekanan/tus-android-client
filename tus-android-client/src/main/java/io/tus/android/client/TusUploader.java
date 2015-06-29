package io.tus.android.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class TusUploader {
    private URL uploadURL;
    private InputStream input;
    private long offset;

    private HttpURLConnection connection;
    private OutputStream output;

    public TusUploader(TusClient client, URL uploadURL, InputStream input, long offset) throws IOException {
        this.uploadURL = uploadURL;
        this.input = input;
        this.offset = offset;

        input.skip(offset);

        connection = (HttpURLConnection) uploadURL.openConnection();
        client.prepareConnection(connection);
        connection.setRequestProperty("Upload-Offset", Long.toString(offset));
        try {
            connection.setRequestMethod("PATCH");
            // Check whether we are running on a buggy JRE
        } catch (final ProtocolException pe) {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Tus-Method-Override", "PATCH");
        }

        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);
        output = connection.getOutputStream();
    }

    public int uploadChunk(int chunkSize) throws IOException {
        byte[] buffer = new byte[chunkSize];
        int bytesRead = input.read(buffer);
        if(bytesRead == -1) {
            // No bytes were read since the input stream is empty
            return -1;
        }

        output.write(buffer);
        output.flush();

        offset += bytesRead;

        return bytesRead;
    }

    public long getOffset() {
        return offset;
    }

    public URL getUploadURL() {
        return uploadURL;
    }

    public void finish() throws IOException {
        input.close();
        output.close();
        connection.disconnect();
    }
}