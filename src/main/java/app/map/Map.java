package app.map;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.function.ToDoubleFunction;

import app.map.Line.DifferentStartException;
import app.map.Line.StartStationNotFoundException;

/**
 * Classe représentant la carte
 */
public final class Map {

    public static class IncorrectFileFormatException extends Exception {
        public IncorrectFileFormatException(String filename) {
            super(String.format("Le fichier %s n'est pas bien formé", filename));
        }
    }

    public static class UndefinedLineException extends Exception {
        public UndefinedLineException(String line) {
            super(String.format("La line %s n'existe pas dans la carte", line));
        }
    }

    /**
     * Map où chaque station est associée aux sections dont le départ est cette
     * station
     */
    private final HashMap<String, ArrayList<Section>> map = new HashMap<>();
    /**
     * Map où chaque nom (avec variant) de ligne est associée sa ligne
     */
    private final HashMap<String, Line> lines = new HashMap<>();

    /**
     * Créer une map à partir d'un fichier CSV contenant les sections des lignes du
     * réseau.
     *
     * @param mapFileName le nom du fichier
     * @throws FileNotFoundException        si le fichier n'a pas été trouvé
     * @throws IncorrectFileFormatException si le format du fichier est incorrect
     * @throws IllegalArgumentException     si `mapFileName` est `null`
     */
    public Map(String mapFileName)
            throws FileNotFoundException, IncorrectFileFormatException, IllegalArgumentException {
        if (mapFileName == null)
            throw new IllegalArgumentException();
        parseMap(mapFileName);
    }

    /**
     * Parse un fichier CSV contenant les sections de trajet du réseau.
     *
     * @param fileName le nom du fichier à parser
     * @throws FileNotFoundException        si le fichier n'a pas été trouvé
     * @throws IncorrectFileFormatException si le format du fichier est incorrect
     */
    private void parseMap(String fileName) throws FileNotFoundException, IncorrectFileFormatException {
        File file = new File(fileName);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                handleMapLine(sc.nextLine());
            }
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new IncorrectFileFormatException(file.getName());
        }
    }

    /**
     * Parse une ligne d'un fichier CSV contenant une section de trajet du réseau.
     *
     * @param s la ligne à parser
     * @throws IndexOutOfBoundsException si la ligne est mal formé
     * @throws NumberFormatException     si une des données qui doit être un nombre
     *                                   ne l'est pas
     */
    private void handleMapLine(String s) throws IndexOutOfBoundsException, NumberFormatException {
        String[] data = s.split(";");
        Station start = parseStation(data[0], data[1]);
        Station arrival = parseStation(data[2], data[3]);
        String line = data[4].trim();
        String[] time = data[5].trim().split(":");
        // on suppose que la durée est donnée au format mm:ss
        int duration = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);
        double distance = Double.parseDouble(data[6].trim());
        addSection(start, arrival, distance, duration, line);
    }

    /**
     * Retourne la station correspondant au nom et aux coordonnées.
     *
     * @param station le nom de la station
     * @param coord   les coordonnées de la station séparées par une virgule
     * @return une station correspondant aux paramètres
     * @throws IndexOutOfBoundsException si les coordonnées sont mal formées
     * @throws NumberFormatException     si l'une des coordonnées n'est pas un
     *                                   nombre
     */
    private Station parseStation(String station, String coord) throws IndexOutOfBoundsException, NumberFormatException {
        station = station.trim();
        String[] coords = coord.trim().split(",");
        double x = Double.parseDouble(coords[0]);
        double y = Double.parseDouble(coords[1]);
        Station s = new Station(station, x, y);
        map.putIfAbsent(station, new ArrayList<>());
        return s;
    }

    /**
     * Créer une section entre les stations `start`et `arrival`, et l'ajoute dans
     * `map` et `lines`
     *
     * @param start    la station de départ
     * @param arrival  la station d'arrivé
     * @param distance la distance entre les deux stations
     * @param duration la durée du trajet entre les deux stations
     * @param line     le nom et le variant de la ligne
     * @throws IndexOutOfBoundsException si le nom de la ligne est mal formé
     * @throws NumberFormatException     si le variant n'est pas un nombre
     */
    private void addSection(Station start, Station arrival, double distance, int duration, String lineName)
            throws IndexOutOfBoundsException, NumberFormatException {
        // création de la section
        Section section = new Section(start, arrival, distance, duration);
        // ajout dans map
        map.get(start.name()).add(section);
        // ajout dans lines
        Line line = lines.computeIfAbsent(lineName, n -> {
            String[] lineVariant = n.split(" ");
            String name = lineVariant[0];
            int variant = Integer.parseInt(lineVariant[2]);
            return new Line(name, variant);
        });
        line.addSection(section);
    }

    /**
     * Parse un fichier CSV contenant les horaires de départ des lignes du réseau.
     *
     * @param fileName le nom du fichier à parser
     * @throws IllegalArgumentException      si fileName est `null`
     * @throws FileNotFoundException         si le fichier n'a pas été trouvé
     * @throws IncorrectFileFormatException  si le format du fichier est incorrect
     * @throws UndefinedLineException        si la ligne n'existe pas dans la map
     * @throws StartStationNotFoundException si la ligne n'existe pas sur la ligne
     * @throws DifferentStartException       s'il y a plusieurs station de départ
     *                                       pour une même ligne
     */
    // public void addTime(String fileName) throws IllegalArgumentException,
    // FileNotFoundException,
    // IncorrectFileFormatException, UndefinedLineException,
    // StartStationNotFoundException,
    // DifferentStartException {
    // if (fileName == null)
    // throw new IllegalArgumentException();
    // File file = new File(fileName);
    // Scanner sc = new Scanner(file);
    // try {
    // while (sc.hasNextLine()) {
    // handleTimeLine(sc.nextLine());
    // }
    // } catch (IndexOutOfBoundsException | NumberFormatException e) {
    // throw new IncorrectFileFormatException(file.getName());
    // } finally {
    // sc.close();
    // }
    // }

    /**
     * Parse une ligne d'un fichier CSV contenant un horaire de départ d'une ligne
     * du réseau.
     *
     * @param s la ligne à parser
     * @throws IndexOutOfBoundsException     si la ligne est mal formé
     * @throws NumberFormatException         si l'horaire est mal formé
     * @throws UndefinedLineException        si la ligne n'existe pas dans la map
     * @throws StartStationNotFoundException si la ligne n'existe pas sur la ligne
     * @throws DifferentStartException       s'il y a plusieurs station de départ
     *                                       pour une même ligne
     */
    // private void handleTimeLine(String s) throws IndexOutOfBoundsException,
    // NumberFormatException, UndefinedLineException,
    // StartStationNotFoundException, DifferentStartException {
    // String[] data = s.split(";");
    // String line = data[0].trim();
    // String stationName = data[1].trim();
    // String[] time = data[2].trim().split(":");
    // int hour = Integer.parseInt(time[0]);
    // int minute = Integer.parseInt(time[1]);
    // addDepartureTime(line, stationName, hour, minute);
    // }

    /**
     * Ajoute l'horaire de départ et le section de départ à la ligne si elle n'a pas
     * été déjà déterminée
     *
     * @param line        le nom et le variant de la ligne
     * @param stationName le nom de la station de départ
     * @param hour        l'heure de l'horaire de départ
     * @param minute      les minutes de l'horaire de départ
     * @throws UndefinedLineException        si la ligne n'existe pas dans la map
     * @throws StartStationNotFoundException si la ligne n'existe pas sur la ligne
     * @throws DifferentStartException       s'il y a plusieurs station de départ
     *                                       pour une même ligne
     */
    // private void addDepartureTime(String line, String stationName, int hour, int
    // minute)
    // throws UndefinedLineException, StartStationNotFoundException,
    // DifferentStartException {
    // Line l = lines.get(line);
    // if (l == null)
    // throw new UndefinedLineException(line);
    // // ajoute la section de départ si nécessaire
    // l.setStart(stationName);
    // l.addDepartureTime(hour, minute);
    // }

    public HashMap<String, ArrayList<Section>> getMap() {
        return new HashMap<>(map);
    }

    public HashMap<String, Line> getLines() {
        return new HashMap<>(lines);
    }

    public class PathNotFoundException extends Exception {
        public PathNotFoundException(String start, String arrival) {
            super(String.format("Pas de chemin trouvé entre %s et %s", start, arrival));
        }

        public PathNotFoundException() {
            super();
        }
    }

    /**
     * Calcule un trajet entre 2 stations et renvoie la liste des sections du trajet
     *
     * @param start   le nom de la station de départ
     * @param arrival le nom de la station d'arrivé
     * @return la liste des sections du trajet
     * @throws IllegalArgumentException si start ou arrival est `null`
     * @throws PathNotFoundException    si il n'existe pas de trajet entre les deux
     *                                  stations
     */
    public LinkedList<Section> findPathDistOpt(String startStation, String arrivalStation)
            throws IllegalArgumentException, PathNotFoundException {
        if (startStation == null || arrivalStation == null)
            throw new IllegalArgumentException();
        return dijkstra(startStation, arrivalStation, Section::distance);
    }

    /**
     * Recherche un chemin entre 2 stations en appliquant l'algorithme de
     * dijkstra et renvoie les sections du trajet laissant que la station d'arrivé
     * dans arrivals
     *
     * @param start   la station (sommet) de départ
     * @param arrival la liste des stations (sommet) d'arrivé possible
     * @param f       la fonction qui associe à une section (arête) son poids
     * @return map associant une station à la section prendre pour aller à cette
     *         station
     * @throws PathNotFoundException si il n'existe pas de trajet entre les deux
     *                               stations
     */
    private LinkedList<Section> dijkstra(String start, String arrival, ToDoubleFunction<Section> f)
            throws PathNotFoundException {
        HashMap<String, Double> distance = new HashMap<>();
        HashMap<String, Section> previous = new HashMap<>();
        for (String station : map.keySet()) {
            distance.put(station, Double.MAX_VALUE);
            previous.put(station, null);
        }

        distance.put(start, 0.);
        PriorityQueue<String> queue = new PriorityQueue<>(map.size(),
                Comparator.comparingDouble(distance::get));
        queue.addAll(map.keySet());

        String u = null;
        while (!queue.isEmpty() && (!arrival.equals(u = queue.poll()))) {
            for (Section section : map.get(u)) {
                String v = section.arrival().name();
                double w = distance.get(u) + f.applyAsDouble(section);
                if (distance.get(v) > w) {
                    distance.put(v, w);
                    previous.put(v, section);
                    queue.remove(v);
                    queue.add(v);
                }
            }
        }
        if (!arrival.equals(u))
            throw new PathNotFoundException();
        return dijkstraResultToList(previous, start, u);
    }

    private LinkedList<Section> dijkstraResultToList(HashMap<String, Section> path, String start, String arrival)
            throws PathNotFoundException {
        LinkedList<Section> orderedPath = new LinkedList<>();
        String previous = arrival;
        while (!previous.equals(start)) {
            Section section = path.get(previous);
            if (section == null)
                throw new PathNotFoundException();
            orderedPath.add(section);
            previous = section.start().name();
        }
        Collections.reverse(orderedPath);
        return orderedPath;
    }
}
