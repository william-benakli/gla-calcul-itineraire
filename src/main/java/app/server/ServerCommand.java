package app.server;

import java.util.Arrays;
import java.util.List;

import app.map.Plan;
import app.map.PlanParser;

public interface ServerCommand {

    /**
     * @return description de la commande
     */
    String getdescription();

    /**
     * 
     * @param commandName nom de la commande associé a l'objet {@code ServerCommand}
     * @return une liste de descriptions visuelles sur l'usage de la commande 
     */
    List<String> getExemples(String commandName);

    /**
     * 
     * @param server instance du server en cours d'execution
     * @param args   arguments recues depuis d'entrée
     * @throws IllegalArgumentException si les arguements sont incompatibles avec la commande
     * @throws Exception n'importe quelle autre exception lancée par l'éxécution
     */
    void execute(Server server, String... args) throws IllegalArgumentException, Exception;
}

class SCUpdateMapFile implements ServerCommand {

    @Override
    public String getdescription() {
        return "commande permettant de changer le ficher de plan";
    }

    @Override
    public List<String> getExemples(String commandName) {
        return Arrays.asList(
            new StringBuilder().append(commandName).append(" <ficher des stations>").toString()
        );
    }

    @Override
    public void execute(Server server, String... args) throws IllegalArgumentException, Exception {
        if (args.length != 2) 
            throw new IllegalArgumentException("UpdateMap s'attend à recevoir uniquement le chemin vers le nouveau fichier");
        String filePath = args[1];
        Plan plan = PlanParser.planFromSectionCSV(filePath);
        server.updateMap(plan);
    }
}

class SCUpdateTimeFile implements ServerCommand {

    @Override
    public String getdescription() {
        return "commande permettant de changer les informations de temps au plan";
    }

    @Override
    public List<String> getExemples(String commandName) {
        return Arrays.asList(
            new StringBuilder().append(commandName).append(" <ficher des horaires>").toString()
        );
    }

    @Override
    public void execute(Server server, String... args) throws IllegalArgumentException, Exception {
        if (args.length != 2) 
            throw new IllegalArgumentException("S'attend à recevoir uniquement le chemin vers le nouveau fichier");
        String filePath = args[1];

        Plan copyPlan = new Plan(server.getPlan());
        PlanParser.addTimeFromCSV(copyPlan, filePath);
        server.updateMap(copyPlan);
    }
    
}