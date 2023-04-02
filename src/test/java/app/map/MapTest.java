package app.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import app.map.Map.IncorrectFileFormatException;
import app.map.Map.PathNotFoundException;

public class MapTest {

    private static final int DEFAULT_TIMEOUT = 2000;

    private String getPath(String filename) {
        return "src/test/resources/" + filename + ".csv";
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void nullFileName() {
        assertThrows(IllegalArgumentException.class, () -> new Map(null), "null file name");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void notFoundFile() {
        assertThrows(FileNotFoundException.class, () -> new Map("test"), "File not found");
    }

    @ParameterizedTest
    @ValueSource(strings = { "arrival_missing", "bad_coord_format", "bad_time_format", "coord_missing",
            "line_missing" })
    @Timeout(DEFAULT_TIMEOUT)
    public void incorrectFileFormat(String filename) {
        assertThrows(IncorrectFileFormatException.class, () -> new Map(getPath(filename)),
                "Incorrect file format");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void sameStationHasSameReference()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        Map map = new Map(getPath("same_station"));
        Station station = new Station("Lourmel", 2.2822419598550767, 48.83866086365992);
        ArrayList<Section> sections = map.getMap().get(station).getSections();
        assertSame(sections.get(0).start(), sections.get(1).start(), "Same station has same reference");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void sameNameDifferentCoordAreDifferentStation()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        Map map = new Map(getPath("different_coord"));
        assertEquals(4, map.getMap().size(), "Same station has same reference");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void sameSectionInMapAndLines()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        BinaryOperator<ArrayList<Section>> accumulator = (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        };
        ArrayList<Section> inMap = map.getMap().values()
                .stream()
                .map(Connection::getSections)
                .reduce(new ArrayList<>(), accumulator);

        ArrayList<Section> inLines = map.getLines().values()
                .stream()
                .map(Line::getSections)
                .reduce(new ArrayList<>(), accumulator);
        assertTrue(inLines.containsAll(inMap) && inMap.containsAll(inLines),
                "map and lines field contains the sames sections");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPathWithNullStart()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        assertThrows(IllegalArgumentException.class, () -> map.findPathDistOpt(null, "test"),
                "Find path from null station");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPathWithNullArrival()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        assertThrows(IllegalArgumentException.class, () -> map.findPathDistOpt("test", null),
                "Find path to null station");
    }

    private void pathNotFoundHelper(String start, String arrival)
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        assertThrows(PathNotFoundException.class, () -> map.findPathDistOpt(start, arrival),
                "Path not found from " + start + " to " + arrival);
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void notExistingStart()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        pathNotFoundHelper("test", "Commerce");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void notExistingArrival()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        pathNotFoundHelper("Lourmel", "test");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void notPathBetween()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        pathNotFoundHelper("Commerce", "Lourmel");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPathSameLine() throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException,
            PathNotFoundException {
        Map map = new Map(getPath("map_data"));
        LinkedList<Section> trajet = map.findPathDistOpt("Lourmel", "Commerce");
        assertEquals(3, trajet.size(), "Lourmel to Commerce");
    }
}
