package encrypt;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileData {

    private String path;
    private byte[] data;

    public FileData(String path) {
        this.path = path;
    }

    public void read() throws Exception {
        this.data = Files.readAllBytes(Path.of(this.path));
    }

    public void write() throws Exception {
        Files.write(Path.of(this.path), this.data);
    }

    public void write(byte[] bytes) throws Exception {
        this.data = bytes;
        Files.write(Path.of(this.path), bytes);
    }

    public String getPath() {
        return path;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}