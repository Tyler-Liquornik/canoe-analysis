package com.wecca.canoeanalysis.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StlImportServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void analysesAsciiStlAndConvertsMillimetresToMetres() throws IOException {
        Path stl = temporaryDirectory.resolve("canoe-ascii.stl");
        Files.writeString(stl, asciiTetrahedron());

        StlImportService.Analysis analysis = StlImportService.analyse(
                stl,
                StlImportService.InputUnit.MILLIMETRES);

        assertEquals("ASCII", analysis.format());
        assertEquals(4, analysis.triangleCount());
        assertEquals(12, analysis.vertexRecordCount());
        assertEquals(4, analysis.uniqueVertexCount());
        assertEquals(StlImportService.Axis.X, analysis.lengthAxis());
        assertEquals(StlImportService.Axis.Y, analysis.widthAxis());
        assertEquals(StlImportService.Axis.Z, analysis.heightAxis());
        assertEquals(1.0, analysis.lengthMetres(), 1.0e-9);
        assertEquals(0.2, analysis.widthMetres(), 1.0e-9);
        assertEquals(0.1, analysis.heightMetres(), 1.0e-9);
        assertEquals(StlImportService.PROFILE_SAMPLE_COUNT, analysis.sideProfile().size());
        assertEquals(StlImportService.PROFILE_SAMPLE_COUNT, analysis.topProfile().size());
    }

    @Test
    void detectsAndAnalysesBinaryStl() throws IOException {
        Path stl = temporaryDirectory.resolve("canoe-binary.stl");
        Files.write(stl, binaryTetrahedron());

        StlImportService.Analysis analysis = StlImportService.analyse(
                stl,
                StlImportService.InputUnit.MILLIMETRES);

        assertEquals("binary", analysis.format());
        assertEquals(4, analysis.triangleCount());
        assertEquals(1.0, analysis.lengthMetres(), 1.0e-9);
        assertEquals(0.2, analysis.widthMetres(), 1.0e-9);
        assertEquals(0.1, analysis.heightMetres(), 1.0e-9);
    }

    @Test
    void writesAnExplicitlyUnvalidatedPresetDraft() throws IOException {
        Path stl = temporaryDirectory.resolve("canoe.stl");
        Path draft = temporaryDirectory.resolve("test-preset.yaml");
        Files.writeString(stl, asciiTetrahedron());
        StlImportService.Analysis analysis = StlImportService.analyse(
                stl,
                StlImportService.InputUnit.MILLIMETRES);

        StlImportService.writePresetDraft(draft, analysis, "Test Canoe", 2027);
        JsonNode yaml = new ObjectMapper(new YAMLFactory()).readTree(draft.toFile());

        assertEquals("beta-unvalidated", yaml.path("status").asText());
        assertEquals("Test Canoe", yaml.path("name").asText());
        assertEquals(1.0, yaml.path("geometry").path("lengthMetres").asDouble(), 1.0e-9);
        assertTrue(yaml.path("valuesStillRequired").toString().contains("wall thickness"));
    }

    private static String asciiTetrahedron() {
        return """
                solid canoe
                  facet normal 0 0 0
                    outer loop
                      vertex 0 0 0
                      vertex 1000 0 0
                      vertex 0 200 0
                    endloop
                  endfacet
                  facet normal 0 0 0
                    outer loop
                      vertex 0 0 0
                      vertex 1000 0 0
                      vertex 0 0 100
                    endloop
                  endfacet
                  facet normal 0 0 0
                    outer loop
                      vertex 0 0 0
                      vertex 0 200 0
                      vertex 0 0 100
                    endloop
                  endfacet
                  facet normal 0 0 0
                    outer loop
                      vertex 1000 0 0
                      vertex 0 200 0
                      vertex 0 0 100
                    endloop
                  endfacet
                endsolid canoe
                """;
    }

    private static byte[] binaryTetrahedron() {
        float[][] vertices = {
                {0, 0, 0}, {1000, 0, 0}, {0, 200, 0},
                {0, 0, 0}, {1000, 0, 0}, {0, 0, 100},
                {0, 0, 0}, {0, 200, 0}, {0, 0, 100},
                {1000, 0, 0}, {0, 200, 0}, {0, 0, 100}
        };
        ByteBuffer buffer = ByteBuffer.allocate(84 + 50 * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(80);
        buffer.putInt(4);
        for (int triangle = 0; triangle < 4; triangle++) {
            buffer.putFloat(0).putFloat(0).putFloat(0);
            for (int vertex = 0; vertex < 3; vertex++) {
                float[] point = vertices[triangle * 3 + vertex];
                buffer.putFloat(point[0]).putFloat(point[1]).putFloat(point[2]);
            }
            buffer.putShort((short) 0);
        }
        return buffer.array();
    }
}
