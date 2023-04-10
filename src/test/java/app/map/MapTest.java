package app.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import app.map.Map.UndefinedLineException;
import app.map.Line.StartStationNotFoundException;
import app.map.Line.DifferentStartException;

public class MapTest {

    private static final int DEFAULT_TIMEOUT = 2000;

    private static final String MAP_DATA_ALL = "map_data_all";

    private static final String MAP_TIME_ALL = "map_time_all";

    private String getPath(String filename) {
        if (filename == null)
            return null;
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
    public void sameSectionInMapAndLines()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        BinaryOperator<ArrayList<Section>> accumulator = (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        };
        ArrayList<Section> inMap = map.getMap().values()
                .stream()
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

    private void findPathHelper(String start, String arrival, int nbLine)
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException,
            PathNotFoundException {
        Map map = new Map(getPath(MAP_DATA_ALL));
        LinkedList<Section> trajet = map.findPathDistOpt(start, arrival);
        assertEquals(nbLine, trajet.size(), start + " to " + arrival);
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPathSameLine() throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException,
            PathNotFoundException {
        findPathHelper("Lourmel", "Commerce", 1);
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPath2Line() throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException,
            PathNotFoundException {
        findPathHelper("Cité", "Hôtel de Ville", 2);
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPath3Line() throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException,
            PathNotFoundException {
        findPathHelper("Alma - Marceau", "Invalides", 3);
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPathNordToLyon()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException,
            PathNotFoundException {
        findPathHelper("Gare du Nord", "Gare de Lyon", 4);
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void findPathBercyToParmentier()
            throws FileNotFoundException, IllegalArgumentException, IncorrectFileFormatException,
            PathNotFoundException {
        findPathHelper("Bercy", "Parmentier", 4);
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void addTimeToLines()
        throws IllegalArgumentException, FileNotFoundException, IncorrectFileFormatException, 
        UndefinedLineException, StartStationNotFoundException, DifferentStartException {

        Map map = new Map(getPath(MAP_DATA_ALL));
        map.addTime(getPath("time_data"));
        assertEquals(15,map.getLines().get("5 variant 2").getDepartures().size(), "nombre de départ de cette ligne");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void nullFileNameTime() throws FileNotFoundException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        assertThrows(IllegalArgumentException.class, () -> map.addTime(getPath(null)), "null file name");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void notFoundFileTime() throws FileNotFoundException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        assertThrows(FileNotFoundException.class, () -> map.addTime(getPath("test")), "File not found");
    }

    @ParameterizedTest
    @ValueSource(strings = { "time_station_missing", "time_bad_time_format", "time_line_missing" })
    @Timeout(DEFAULT_TIMEOUT)
    public void incorrectTimeFileFormat(String filename) throws FileNotFoundException, IncorrectFileFormatException {
        Map map = new Map(getPath("map_data"));
        assertThrows(IncorrectFileFormatException.class, () -> map.addTime(getPath(filename)),
                "Incorrect file format");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void nbOfSectionsInLine()
        throws IllegalArgumentException, FileNotFoundException, IncorrectFileFormatException, 
        UndefinedLineException, StartStationNotFoundException, DifferentStartException {

        Map map = new Map(getPath("map_data"));
        assertEquals(36,map.getLines().get("8 variant 1").getSections().size(), "nombre de sections de cette ligne");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void testSectionsTime() throws FileNotFoundException, IncorrectFileFormatException, StartStationNotFoundException,
            DifferentStartException {

        Map map = new Map(getPath("map_data"));
        Line huit_variant_1 = map.getLines().get("8 variant 1");
        huit_variant_1.setStart("Lourmel");
        ArrayList<Section> boucicautSections = huit_variant_1.getSections();
        Section lourmel_boucicaut = null;
        for (Section s : boucicautSections) {
            if (s.getStart().name().equals("Lourmel") && s.getArrival().name().equals("Boucicaut")) {
                lourmel_boucicaut = s;
                break;
            }
        }

        assertEquals(-1,huit_variant_1.getSectionsMap().get(lourmel_boucicaut), "temps associé au depart egal -1");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void testUpdateSectionsTime() throws FileNotFoundException, IncorrectFileFormatException, StartStationNotFoundException,
            DifferentStartException {

        Map map = new Map(getPath("map_data"));
        Line huit_variant_1 = map.getLines().get("8 variant 1");
        huit_variant_1.setStart("Lourmel");
        ArrayList<Section> boucicautSections = huit_variant_1.getSections();
        Section lourmel_boucicaut = null;
        for (Section s : boucicautSections) {
            if (s.getStart().name().equals("Lourmel") && s.getArrival().name().equals("Boucicaut")) {
                lourmel_boucicaut = s;
                break;
            }
        }

        huit_variant_1.updateSectionsTime();
        assertEquals(254,huit_variant_1.getSectionsMap().get(lourmel_boucicaut), "temps associé au depart egal 254sec");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void testUpdateSectionsTime2() throws FileNotFoundException, IncorrectFileFormatException, StartStationNotFoundException,
            DifferentStartException {

        Map map = new Map(getPath("map_data"));
        Line huit_variant_1 = map.getLines().get("8 variant 1");
        huit_variant_1.setStart("Lourmel");
        ArrayList<Section> boucicautSections = huit_variant_1.getSections();
        Section boucicaut_felix_faure = null;
        for (Section s : boucicautSections) {
            if (s.getStart().name().equals("Boucicaut") && s.getArrival().name().equals("Félix Faure")) {
                boucicaut_felix_faure = s;
                break;
            }
        }

        huit_variant_1.updateSectionsTime();
        assertEquals(452,huit_variant_1.getSectionsMap().get(boucicaut_felix_faure), "temps associé a la section boucicaut_felix_faure egal 452sec");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void testDeparturesFromStation() throws FileNotFoundException, IncorrectFileFormatException, StartStationNotFoundException,
            DifferentStartException, UndefinedLineException {

        Map map = new Map(getPath(MAP_DATA_ALL));
        map.addTime(getPath("time_data_ligne8"));
        Line huit_variant_1 = map.getLines().get("8 variant 1");
        huit_variant_1.setStart("Lourmel");
        huit_variant_1.updateSectionsTime();
        
        Station station =  map.getMap().get("Félix Faure").get(0).getStart();

        assertEquals(17,map.departuresFromStation(station).get("8 variant 1").size(), "nombre d'horaires depuis Félix Faure sur la ligne 8 variant 1");
    }

    @Test
    @Timeout(DEFAULT_TIMEOUT)
    public void testDeparturesFromStationFromTime() throws FileNotFoundException, IncorrectFileFormatException, StartStationNotFoundException,
            DifferentStartException, UndefinedLineException {

        Map map = new Map(getPath(MAP_DATA_ALL));
        map.addTime(getPath("time_data_ligne8"));
        Line huit_variant_1 = map.getLines().get("8 variant 1");
        huit_variant_1.setStart("Lourmel");
        huit_variant_1.updateSectionsTime();
        
        Station station =  map.getMap().get("Félix Faure").get(0).getStart();
        Time time = new Time(16,0,0);

        assertEquals(12,map.departuresFromStationFromTime(station,time).get("8 variant 1").size(), "nombre d'horaires depuis Félix Faure sur la ligne 8 variant 1 depuis 16h");
    }

}
