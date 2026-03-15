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
        Path filePath = Path.of(this.path);
        // Validamos y creamos las carpetas padre si no existen
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath, this.data);
    }

    public void write(byte[] bytes) throws Exception {
        this.data = bytes;
        Path filePath = Path.of(this.path);
        // Validamos y creamos las carpetas padre si no existen
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath, bytes);
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