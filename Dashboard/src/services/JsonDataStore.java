package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonDataStore {

    private final ObjectMapper mapper;
    private final Path dataDirectory;

    public JsonDataStore() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        dataDirectory = findDataDirectory();
    }

    private Path findDataDirectory() {
        Path[] candidates = {
            Path.of("Dashboard", "data"),
            Path.of("data")
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)
                    && Files.isRegularFile(candidate.resolve("fields.json"))) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        return Path.of("Dashboard", "data").toAbsolutePath().normalize();
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    private Path resolveFile(String filename) throws IOException {
        Path file = dataDirectory.resolve(filename).normalize();

        if (!file.startsWith(dataDirectory)) {
            throw new IOException("Invalid data filename: " + filename);
        }

        return file;
    }

    public <T> List<T> loadList(String filename, Class<T> modelClass)
            throws IOException {

        Path file = resolveFile(filename);

        if (!Files.isRegularFile(file)) {
            throw new IOException("Required data file not found: " + file);
        }

        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, modelClass);

        List<T> items = mapper.readValue(file.toFile(), listType);

        if (items == null) {
            throw new IOException(
                    "Expected a JSON array, but found null in: " + file);
        }

        return items;
    }

    public <T> void saveList(String filename, List<T> items)
            throws IOException {

        Path file = resolveFile(filename);

        Files.createDirectories(dataDirectory);
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), items);
    }
}