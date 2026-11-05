package com.nordcodes.qa.env;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Запуск тестируемого приложения из jar-файла отдельным процессом:
 * java -jar -Dsecret=... -Dmock=... -Dserver.port=... internal-0.0.1-SNAPSHOT.jar
 */
final class ApplicationProcess {

    private final Process process;
    private final int port;
    private final Path logFile;

    private ApplicationProcess(Process process, int port, Path logFile) {
        this.process = process;
        this.port = port;
        this.logFile = logFile;
    }

    static ApplicationProcess start(String jar, String secret, String mockUrl, int timeoutSec) {
        Path jarPath = Paths.get(jar).toAbsolutePath();
        if (!Files.isRegularFile(jarPath)) {
            throw new IllegalStateException("Не найден jar тестируемого приложения: " + jarPath
                    + ". Укажите путь параметром -Dapp.jar=...");
        }
        int port = freePort();
        Path logFile = Paths.get("target", "app.log").toAbsolutePath();
        try {
            Files.createDirectories(logFile.getParent());
            String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
            List<String> cmd = List.of(java,
                    "-Dsecret=" + secret,
                    "-Dmock=" + mockUrl,
                    "-Dserver.port=" + port,
                    "-jar", jarPath.toString());
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
            ApplicationProcess app = new ApplicationProcess(process, port, logFile);
            Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
            app.awaitReady(Duration.ofSeconds(timeoutSec));
            return app;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось запустить приложение", e);
        }
    }

    String baseUrl() {
        return "http://localhost:" + port;
    }

    void stop() {
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(15, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Ждём, пока приложение начнёт отвечать на HTTP-запросы (любым кодом). */
    private void awaitReady(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Приложение завершилось при запуске, см. лог: " + logFile);
            }
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl() + "/endpoint").toURL().openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(2000);
                conn.getResponseCode();
                conn.disconnect();
                return;
            } catch (IOException notReadyYet) {
                sleep(500);
            }
        }
        stop();
        throw new IllegalStateException("Приложение не запустилось за " + timeout.toSeconds() + " с, см. лог: " + logFile);
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
