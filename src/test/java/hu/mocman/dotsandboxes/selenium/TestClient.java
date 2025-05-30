package hu.mocman.dotsandboxes.selenium;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.util.io.pem.PemObject;

import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import org.bouncycastle.util.io.pem.PemWriter;


@Slf4j
@Getter
public class TestClient {
    private String privateKey;
    private String publicKey;

    public void createSslKeys() throws NoSuchAlgorithmException, IOException {

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        RSAPrivateKey rsaPrivStruct = RSAPrivateKey.getInstance(
                PrivateKeyInfo.getInstance(keyPair.getPrivate().getEncoded()).parsePrivateKey()
        );

        StringWriter privWriter = new StringWriter();
        try (PemWriter pemWriter = new PemWriter(privWriter)) {
            pemWriter.writeObject(new PemObject("RSA PRIVATE KEY", rsaPrivStruct.getEncoded()));
        }
        privateKey = privWriter.toString();

        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        publicKey = generateOpenSSHPublicKey(pub, "test@localhost");

    }

    private static String generateOpenSSHPublicKey(RSAPublicKey pub, String comment) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writeString(buf, "ssh-rsa");
        writeBigInt(buf, pub.getPublicExponent());
        writeBigInt(buf, pub.getModulus());
        String base64 = Base64.getEncoder().encodeToString(buf.toByteArray());
        return "ssh-rsa " + base64 + " " + comment;
    }

    private static void writeString(ByteArrayOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        out.write(intToBytes(bytes.length));
        out.write(bytes);
    }

    private static void writeBigInt(ByteArrayOutputStream out, BigInteger value) throws IOException {
        byte[] bytes = value.toByteArray();
        out.write(intToBytes(bytes.length));
        out.write(bytes);
    }

    private static byte[] intToBytes(int val) {
        return new byte[] {
                (byte) ((val >>> 24) & 0xFF),
                (byte) ((val >>> 16) & 0xFF),
                (byte) ((val >>> 8) & 0xFF),
                (byte) (val & 0xFF)
        };
    }


    private Path tempDir;

    public void cloneRepo(String cloneCommand) throws IOException {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        log.info("Cloning repository with command {}", cloneCommand);
        tempDir = Files.createTempDirectory("git-repo");

        Path privateKeyFile = tempDir.resolve("key");
        Path publicKeyFile = tempDir.resolve("key.pub");

        java.nio.file.Files.writeString(privateKeyFile, privateKey);
        java.nio.file.Files.writeString(publicKeyFile, publicKey);
        log.info("Private key has been written to {}", privateKeyFile);
        log.info("Public key has been written to {}", publicKeyFile);
        Files.setPosixFilePermissions(privateKeyFile, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        Files.setPosixFilePermissions(publicKeyFile, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(tempDir.toFile());
        processBuilder.environment().put("GIT_SSH_COMMAND", "ssh -i " + privateKeyFile + " -i " + publicKeyFile + " -o StrictHostKeyChecking=no");
        processBuilder.command("sh", "-c", cloneCommand);

        log.info("Spawning git process");

        Process process = processBuilder.start();
        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
             var outputStream = process.getOutputStream()) {
            outputStream.write("yes\n".getBytes());
            outputStream.flush();
            outputStream.close();

            String line;
            while ((line = inputReader.readLine()) != null) {
                log.info(line);
            }
            while ((line = errorReader.readLine()) != null) {
                log.error(line);
            }

            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void deleteRepo() {
        if (tempDir != null) {
            try {
                Path privateKeyFile = tempDir.resolve("key");
                Path publicKeyFile = tempDir.resolve("key.pub");
                if (Files.exists(privateKeyFile)) {
                    Files.delete(privateKeyFile);
                }
                if (Files.exists(publicKeyFile)) {
                    Files.delete(publicKeyFile);
                }
                Files.walk(tempDir)
                        .sorted((p1, p2) -> -p1.compareTo(p2))
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasRepo() {
        if (tempDir == null) {
            return false;
        }
        Path gitDir = tempDir.resolve("dots-and-boxes/.git");
        return Files.isDirectory(gitDir);
    }

    public void replaceInFile(String repoFile, String original, String replace) {
        try {
            Path filePath = tempDir.resolve(repoFile);
            String content = Files.readString(filePath);
            content = content.replace(original, replace);
            Files.writeString(filePath, content);
        } catch (IOException e) {
            log.error("Failed to replace content in file: " + repoFile, e);
        }
    }

    public void commitAndPush() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(tempDir.resolve("dots-and-boxes").toFile());
            processBuilder.environment().put("GIT_SSH_COMMAND", "ssh -i " + tempDir.resolve("key") + " -o StrictHostKeyChecking=no");

            // Configure git
            processBuilder.command("git", "config", "--local", "user.email", "test@localhost");
            Process process = processBuilder.start();
            process.waitFor();

            processBuilder.command("git", "config", "--local", "user.name", "Test User");
            process = processBuilder.start();
            process.waitFor();

            processBuilder.command("git", "config", "--local", "commit.gpgsign", "false");
            process = processBuilder.start();
            process.waitFor();

            // Add changes
            processBuilder.command("git", "add", ".");
            process = processBuilder.start();
            process.waitFor();

            // Commit changes
            processBuilder.command("git", "commit", "-m", "Automated commit");
            process = processBuilder.start();
            process.waitFor();

            // Push changes
            processBuilder.command("git", "push", "origin", "main");
            process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Failed to commit and push changes", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
