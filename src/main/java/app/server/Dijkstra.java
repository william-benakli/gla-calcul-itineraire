package app.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.ToIntBiFunction;
import app.map.Plan;
import app.map.Section;
import app.map.Station;
import app.map.Time;

public final class Dijkstra {
    /**
     * Le plan du réseau
     */
    private final Plan plan;
    /**
     * Le graphe : les sommets sont les noms des stations et les arêtes sont les sections
     */
    private final Map<String, List<Section>> map;
    /**
     * Le sommet de départ
     */
    private final String start;
    /**
     * Le sommet d'arrivé
     */
    private final String arrival;
    /**
     * L'horaire de départ
     */
    private final Time departTime;
    /**
     * Optimisation en distance ou en temps
     */
    private boolean distOpt;
    /**
     * Si des sections à pied sont possibles
     */
    private boolean foot;
    /**
     * La distance maximale à parcourir à pied entre 2 sections
     */
    private static final int MAX_FOOT_DISTANCE = 1000;
    /**
     * Le poids pour les trajets à pied
     */
    private static final double WEIGHT_FOOT = 1.5;
    /**
     * La fonction qui calcule le poids d'une arrête à partir de la dernière arête traitée
     */
    private final ToIntBiFunction<Section, Section> getWeight;
    /**
     * Associe chaque sommet à sa distance par rapport au sommet de départ
     */
    private final Map<String, Integer> distance;
    /**
     * Associe chaque sommet à l'arête pris pour arriver à ce sommet
     */
    private final Map<String, Section> previous;
    /**
     * File de priorité sur les sommets par rapport à leur distance avec le sommet de départ
     */
    private final PriorityQueue<String> queue;
    /**
     * Le sommet en cours de traitement
     */
    private String u;
    /**
     * Le résultat de l'algorithme
     */
    private List<Section> result;

    /**
     * @param plan le plan à utiliser
     * @param start le sommet de départ
     * @param arrival le sommet d'arrivé
     * @param departTime l'horaire de départ
     * @param getWeight la fonction qui associe à une section (arête) son poids
     */
    Dijkstra(Plan plan, String start, String arrival, Time departTime, boolean distOpt,
            boolean foot) {
        if (plan == null || start == null || arrival == null)
            throw new IllegalArgumentException();
        this.plan = plan;
        this.map = plan.getMap();
        this.start = start;
        this.arrival = arrival;
        this.departTime = departTime;
        this.distOpt = distOpt;
        this.foot = foot;
        this.getWeight = distOpt ? Section::distanceTo : Section::durationTo;
        distance = new HashMap<>();
        previous = new HashMap<>();
        queue = new PriorityQueue<>(map.size(), Comparator.comparingInt(distance::get));
        this.u = null;
        this.result = null;
    }

    public static class PathNotFoundException extends Exception {
        public PathNotFoundException(String start, String arrival) {
            super(String.format("Pas de chemin trouvé entre %s et %s", start, arrival));
        }

        public PathNotFoundException() {
            super();
        }
    }

    /**
     * Recherche un chemin entre 2 sommets en appliquant l'algorithme de dijkstra et renvoie la
     * liste des arêtes dans l'ordre du chemin
     *
     * @return la liste des arêtes dans l'ordre du départ à l'arrivé
     * @throws PathNotFoundException s'il n'existe pas de chemin entre les deux sommets
     */
    List<Section> getPath() throws PathNotFoundException {
        if (result == null)
            compute();
        return result;
    }

    /**
     * Exécute l'algorithme de dijkstra
     *
     * @throws PathNotFoundException s'il n'existe pas de chemin entre les deux sommets
     */
    private void compute() throws PathNotFoundException {
        init();
        while (!finished())
            loop();
        if (arrival.equals(u))
            setResult();
        else
            throw new PathNotFoundException();
    }

    /**
     * Initialise les valeurs pour l'algorithme
     */
    private void init() {
        for (String station : map.keySet()) {
            distance.put(station, Integer.MAX_VALUE);
        }
        distance.put(start, 0);
        queue.addAll(map.keySet());
    }

    /**
     * @return {@code true} si l'algo à terminer {@code false} sinon
     */
    private boolean finished() {
        if (queue.isEmpty())
            return true;
        u = queue.poll();
        return arrival.equals(u) || distance.get(u) == Integer.MAX_VALUE;
    }

    /**
     * @param station une station
     * @return une liste de section à pied partant de {@code station} et arrivant à une autre
     *         station à moins de {@code MAX_FOOT_DISTANCE} mètres
     */
    private List<Section> getCloseStations(Station station) {
        List<Section> res = new ArrayList<>();
        for (Station s : plan.getStations()) {
            int dist = station.distanceBetween(s);
            if (dist < MAX_FOOT_DISTANCE)
                res.add(new Section(station, s, null, (int) Math.ceil(dist * WEIGHT_FOOT),
                        (int) Math.ceil(station.durationBetween(s) * WEIGHT_FOOT)));
        }
        return res;
    }

    /**
     * Corps de l'algorithme
     */
    private void loop() {
        Section prev = previous.get(u);
        List<Section> neighbors = map.get(u);
        if (neighbors == null)
            neighbors = new ArrayList<>();
        if (prev != null && foot)
            neighbors.addAll(getCloseStations(previous.get(u).getArrival()));

        for (Section section : neighbors) {
            if (prev == null) {
                prev = new Section(section.getStart(), section.getStart(), "", 0, 0);
                prev.setTime(departTime);
            }
            plan.updateSectionTime(section, prev.getArrivalTime());
            if (distOpt || section.getTime() != null) {
                String v = section.getArrival().getName();
                int w = distance.get(u) + getWeight.applyAsInt(prev, section);
                if (distance.get(v) > w) {
                    distance.put(v, w);
                    previous.put(v, section);
                    queue.remove(v);
                    queue.add(v);
                }
            }
        }
    }

    /**
     * Récupère la liste des arêtes dans l'ordre du chemin
     *
     * @throws PathNotFoundException s'il n'existe pas de chemin entre les deux sommets
     */
    private void setResult() throws PathNotFoundException {
        List<Section> orderedPath = new LinkedList<>();
        String last = arrival;
        while (!last.equals(start)) {
            Section section = previous.get(last);
            if (section == null)
                throw new PathNotFoundException();
            orderedPath.add(section);
            last = section.getStart().getName();
        }
        Collections.reverse(orderedPath);
        result = orderedPath;
    }
}
