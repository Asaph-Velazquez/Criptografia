package encrypt;
import encrypt.GUI;

public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {

        // Archives
        FileData input = new FileData("src/main/resources/BaseTXT/Imagen.bmp");
        FileData encryptedFile = new FileData("src/main/resources/EncryptTXT/Imagen_e.bmp");
        FileData decryptedFile = new FileData("src/main/resources/DecryptTXT/Imagen_e_d.bmp");

        // Read data
        input.read();
        byte[] data = input.getData();

        // Create Encrypt instance
        Encrypt rsa = new Encrypt();

        // Encrypt and write
        byte[] encryptedData = rsa.encrypt(data);
        encryptedFile.write(encryptedData);

        // Decrypt and write
        byte[] decryptedData = rsa.decrypt(encryptedData);
        decryptedFile.write(decryptedData);

        System.out.println("Proceso terminado");
        
        new GUI();
    }
}