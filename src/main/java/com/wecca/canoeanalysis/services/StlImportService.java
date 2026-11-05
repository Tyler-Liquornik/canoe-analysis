package com.wecca.canoeanalysis.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads STL triangle meshes and extracts a simple longitudinal canoe outline.
 *
 * <p>This is intentionally a beta geometry importer. It derives dimensions and
 * sampled side/top profiles, but it does not infer concrete properties, wall
 * thickness, bulkheads, or structural design values from a surface mesh.</p>
 */
public final class StlImportService {

    public static final int PROFILE_SAMPLE_COUNT = 101;

    private static final long BINARY_HEADER_BYTES = 84;
    private static final long BINARY_TRIANGLE_BYTES = 50;
    private static final Pattern ASCII_VERTEX_PATTERN = Pattern.compile(
            "^\\s*vertex\\s+([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)"
                    + "\\s+([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)"
                    + "\\s+([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private StlImportService() {
    }

    /** Source units offered by the beta UI. All derived values are stored in metres. */
    public enum InputUnit {
        MILLIMETRES("Millimetres (mm)", "mm", 0.001),
        CENTIMETRES("Centimetres (cm)", "cm", 0.01),
        METRES("Metres (m)", "m", 1.0),
        INCHES("Inches (in)", "in", 0.0254);

        private final String displayName;
        private final String symbol;
        private final double metresPerUnit;

        InputUnit(String displayName, String symbol, double metresPerUnit) {
            this.displayName = displayName;
            this.symbol = symbol;
            this.metresPerUnit = metresPerUnit;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSymbol() {
            return symbol;
        }

        public double getMetresPerUnit() {
            return metresPerUnit;
        }

        public static InputUnit fromDisplayName(String displayName) {
            return Arrays.stream(values())
                    .filter(unit -> unit.displayName.equals(displayName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported STL unit: " + displayName));
        }
    }

    /** Cartesian mesh axes before PADDL normalizes the canoe orientation. */
    public enum Axis {
        X(0), Y(1), Z(2);

        private final int index;

        Axis(int index) {
            this.index = index;
        }

        private double coordinate(Vertex vertex) {
            return switch (this) {
                case X -> vertex.x();
                case Y -> vertex.y();
                case Z -> vertex.z();
            };
        }
    }

    /** One sample in a normalized longitudinal profile. */
    public record ProfilePoint(double xMetres, double valueMetres) {
    }

    /** Immutable geometry summary returned after parsing an STL mesh. */
    public record Analysis(
            Path sourceFile,
            String format,
            int triangleCount,
            int vertexRecordCount,
            int uniqueVertexCount,
            InputUnit inputUnit,
            Axis lengthAxis,
            Axis widthAxis,
            Axis heightAxis,
            double lengthMetres,
            double widthMetres,
            double heightMetres,
            List<ProfilePoint> sideProfile,
            List<ProfilePoint> topProfile) {

        public Analysis {
            sourceFile = sourceFile.toAbsolutePath().normalize();
            sideProfile = List.copyOf(sideProfile);
            topProfile = List.copyOf(topProfile);
        }
    }

    private record Vertex(double x, double y, double z) {
        private Vertex {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("STL contains a non-finite vertex coordinate");
            }
        }
    }

    private record ParsedMesh(String format, int triangleCount, List<Vertex> vertices) {
    }

    /** Parses and analyses one ASCII or binary STL file. */
    public static Analysis analyse(Path sourceFile, InputUnit inputUnit) throws IOException {
        if (sourceFile == null || inputUnit == null) {
            throw new IllegalArgumentException("An STL file and its source units are required");
        }
        if (!Files.isRegularFile(sourceFile)) {
            throw new IOException("The selected STL file does not exist");
        }

        ParsedMesh mesh = isBinaryStl(sourceFile)
                ? readBinaryStl(sourceFile)
                : readAsciiStl(sourceFile);
        if (mesh.vertices().size() < 3) {
            throw new IOException("The STL does not contain any complete triangles");
        }

        double[][] bounds = findBounds(mesh.vertices());
        List<Axis> axesBySpan = Arrays.stream(Axis.values())
                .sorted(Comparator
                        .comparingDouble((Axis axis) -> bounds[1][axis.index] - bounds[0][axis.index])
                        .reversed()
                        .thenComparingInt(axis -> axis.index))
                .toList();

        Axis lengthAxis = axesBySpan.get(0);
        Axis widthAxis = axesBySpan.get(1);
        Axis heightAxis = axesBySpan.get(2);
        double scale = inputUnit.getMetresPerUnit();
        double length = span(bounds, lengthAxis) * scale;
        double width = span(bounds, widthAxis) * scale;
        double height = span(bounds, heightAxis) * scale;

        if (length <= 0 || width <= 0 || height <= 0) {
            throw new IOException("The STL must have non-zero length, width, and height");
        }

        List<List<ProfilePoint>> profiles = deriveProfiles(
                mesh.vertices(), bounds, scale, lengthAxis, widthAxis, heightAxis, length);
        Set<Vertex> uniqueVertices = new HashSet<>(mesh.vertices());

        return new Analysis(
                sourceFile,
                mesh.format(),
                mesh.triangleCount(),
                mesh.vertices().size(),
                uniqueVertices.size(),
                inputUnit,
                lengthAxis,
                widthAxis,
                heightAxis,
                length,
                width,
                height,
                profiles.get(0),
                profiles.get(1));
    }

    /**
     * Saves a developer-readable YAML draft. The file is deliberately not a
     * production CanoePreset because material and design values need validation.
     */
    public static void writePresetDraft(
            Path outputFile,
            Analysis analysis,
            String presetName,
            int year) throws IOException {
        if (outputFile == null || analysis == null) {
            throw new IllegalArgumentException("An analysis and output file are required");
        }
        if (presetName == null || presetName.isBlank()) {
            throw new IllegalArgumentException("Enter a preset name before saving");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Preset year must be between 2000 and 2100");
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("status", "beta-unvalidated");
        root.put("name", presetName.trim());
        root.put("year", year);
        root.put("sourceFile", analysis.sourceFile().getFileName().toString());
        root.put("sourceFormat", analysis.format());
        root.put("inputUnit", analysis.inputUnit().getSymbol());

        Map<String, Object> mesh = new LinkedHashMap<>();
        mesh.put("triangles", analysis.triangleCount());
        mesh.put("vertexRecords", analysis.vertexRecordCount());
        mesh.put("uniqueVertices", analysis.uniqueVertexCount());
        root.put("mesh", mesh);

        Map<String, Object> axes = new LinkedHashMap<>();
        axes.put("length", analysis.lengthAxis().name());
        axes.put("width", analysis.widthAxis().name());
        axes.put("height", analysis.heightAxis().name());
        root.put("detectedAxes", axes);

        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("lengthMetres", analysis.lengthMetres());
        geometry.put("maximumWidthMetres", analysis.widthMetres());
        geometry.put("maximumHeightMetres", analysis.heightMetres());
        geometry.put("sideProfile", profileForYaml(analysis.sideProfile(), "depthMetres"));
        geometry.put("topProfile", profileForYaml(analysis.topProfile(), "halfWidthMetres"));
        root.put("geometry", geometry);

        root.put("valuesStillRequired", List.of(
                "validated axis orientation and profile fit",
                "wall thickness",
                "concrete and bulkhead density",
                "bulkhead locations",
                "structural analysis and report values"));

        Path parent = outputFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outputFile.toFile(), root);
    }

    private static List<Map<String, Double>> profileForYaml(
            List<ProfilePoint> profile,
            String valueKey) {
        List<Map<String, Double>> samples = new ArrayList<>(profile.size());
        for (ProfilePoint point : profile) {
            Map<String, Double> sample = new LinkedHashMap<>();
            sample.put("xMetres", point.xMetres());
            sample.put(valueKey, point.valueMetres());
            samples.add(sample);
        }
        return samples;
    }

    private static boolean isBinaryStl(Path sourceFile) throws IOException {
        long size = Files.size(sourceFile);
        if (size < BINARY_HEADER_BYTES) {
            return false;
        }

        byte[] countBytes = new byte[4];
        try (DataInputStream input = new DataInputStream(Files.newInputStream(sourceFile))) {
            input.skipNBytes(80);
            input.readFully(countBytes);
        }
        long triangleCount = Integer.toUnsignedLong(
                ByteBuffer.wrap(countBytes).order(ByteOrder.LITTLE_ENDIAN).getInt());
        return triangleCount > 0
                && BINARY_HEADER_BYTES + BINARY_TRIANGLE_BYTES * triangleCount == size;
    }

    private static ParsedMesh readBinaryStl(Path sourceFile) throws IOException {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(sourceFile))) {
            input.skipNBytes(80);
            long triangleCountLong = Integer.toUnsignedLong(readLittleEndianInt(input));
            if (triangleCountLong > Integer.MAX_VALUE) {
                throw new IOException("The STL contains too many triangles for this beta importer");
            }
            int triangleCount = (int) triangleCountLong;
            List<Vertex> vertices = new ArrayList<>(Math.multiplyExact(triangleCount, 3));

            for (int triangle = 0; triangle < triangleCount; triangle++) {
                input.skipNBytes(12); // Surface normal; the beta derives outlines from vertices.
                for (int vertex = 0; vertex < 3; vertex++) {
                    vertices.add(new Vertex(
                            readLittleEndianFloat(input),
                            readLittleEndianFloat(input),
                            readLittleEndianFloat(input)));
                }
                input.skipNBytes(2); // Attribute byte count.
            }
            return new ParsedMesh("binary", triangleCount, vertices);
        } catch (EOFException exception) {
            throw new IOException("The binary STL ended before all triangles were read", exception);
        } catch (ArithmeticException exception) {
            throw new IOException("The STL contains too many triangles for this beta importer", exception);
        }
    }

    private static ParsedMesh readAsciiStl(Path sourceFile) throws IOException {
        List<Vertex> vertices = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = ASCII_VERTEX_PATTERN.matcher(line);
                if (matcher.matches()) {
                    try {
                        vertices.add(new Vertex(
                                Double.parseDouble(matcher.group(1)),
                                Double.parseDouble(matcher.group(2)),
                                Double.parseDouble(matcher.group(3))));
                    } catch (IllegalArgumentException exception) {
                        throw new IOException("The ASCII STL contains an invalid vertex", exception);
                    }
                }
            }
        }
        if (vertices.isEmpty() || vertices.size() % 3 != 0) {
            throw new IOException("The ASCII STL does not contain complete vertex triplets");
        }
        return new ParsedMesh("ASCII", vertices.size() / 3, vertices);
    }

    private static int readLittleEndianInt(DataInputStream input) throws IOException {
        return Integer.reverseBytes(input.readInt());
    }

    private static float readLittleEndianFloat(DataInputStream input) throws IOException {
        return Float.intBitsToFloat(readLittleEndianInt(input));
    }

    private static double[][] findBounds(List<Vertex> vertices) {
        double[][] bounds = {
                {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
                {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY}
        };
        for (Vertex vertex : vertices) {
            double[] values = {vertex.x(), vertex.y(), vertex.z()};
            for (int axis = 0; axis < values.length; axis++) {
                bounds[0][axis] = Math.min(bounds[0][axis], values[axis]);
                bounds[1][axis] = Math.max(bounds[1][axis], values[axis]);
            }
        }
        return bounds;
    }

    private static double span(double[][] bounds, Axis axis) {
        return bounds[1][axis.index] - bounds[0][axis.index];
    }

    private static List<List<ProfilePoint>> deriveProfiles(
            List<Vertex> vertices,
            double[][] bounds,
            double scale,
            Axis lengthAxis,
            Axis widthAxis,
            Axis heightAxis,
            double lengthMetres) {
        double[] depth = new double[PROFILE_SAMPLE_COUNT];
        double[] halfWidth = new double[PROFILE_SAMPLE_COUNT];
        Arrays.fill(depth, Double.NaN);
        Arrays.fill(halfWidth, Double.NaN);

        double minimumLength = bounds[0][lengthAxis.index];
        double maximumHeight = bounds[1][heightAxis.index];
        double widthCentre = (bounds[0][widthAxis.index] + bounds[1][widthAxis.index]) / 2.0;
        double rawLength = span(bounds, lengthAxis);

        for (Vertex vertex : vertices) {
            double normalizedLength = (lengthAxis.coordinate(vertex) - minimumLength) / rawLength;
            int sample = (int) Math.round(normalizedLength * (PROFILE_SAMPLE_COUNT - 1));
            sample = Math.max(0, Math.min(PROFILE_SAMPLE_COUNT - 1, sample));

            double vertexDepth = (maximumHeight - heightAxis.coordinate(vertex)) * scale;
            double vertexHalfWidth = Math.abs(widthAxis.coordinate(vertex) - widthCentre) * scale;
            depth[sample] = Double.isNaN(depth[sample])
                    ? vertexDepth
                    : Math.max(depth[sample], vertexDepth);
            halfWidth[sample] = Double.isNaN(halfWidth[sample])
                    ? vertexHalfWidth
                    : Math.max(halfWidth[sample], vertexHalfWidth);
        }

        interpolateMissingSamples(depth);
        interpolateMissingSamples(halfWidth);
        return List.of(
                toProfilePoints(depth, lengthMetres),
                toProfilePoints(halfWidth, lengthMetres));
    }

    private static void interpolateMissingSamples(double[] values) {
        int first = -1;
        for (int i = 0; i < values.length; i++) {
            if (!Double.isNaN(values[i])) {
                first = i;
                break;
            }
        }
        if (first < 0) {
            throw new IllegalArgumentException("No profile samples could be derived from the STL");
        }
        Arrays.fill(values, 0, first, values[first]);

        int previous = first;
        for (int next = first + 1; next < values.length; next++) {
            if (Double.isNaN(values[next])) {
                continue;
            }
            double start = values[previous];
            double end = values[next];
            for (int i = previous + 1; i < next; i++) {
                double fraction = (double) (i - previous) / (next - previous);
                values[i] = start + (end - start) * fraction;
            }
            previous = next;
        }
        Arrays.fill(values, previous + 1, values.length, values[previous]);
    }

    private static List<ProfilePoint> toProfilePoints(double[] values, double lengthMetres) {
        List<ProfilePoint> points = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            double x = lengthMetres * i / (values.length - 1);
            points.add(new ProfilePoint(x, values[i]));
        }
        return points;
    }

    /** Stable numeric formatting used by the UI and useful in diagnostics. */
    public static String formatMetres(double value) {
        return String.format(Locale.US, "%.3f m", value);
    }
}
