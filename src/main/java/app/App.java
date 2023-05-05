/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.json.*;
import server.Server;
import server.map.PlanParser;
import server.map.PlanParser.InconsistentDataException;
import util.Logger;

public class App {

    private static final int PORT;

    static {
        try (InputStream stream = App.class.getResourceAsStream("/config/network.json")) {
            // Création d'un objet JsonReader
            JsonReader jsonReader = Json.createReader(stream);
            // Récupération de l'objet racine JSON
            JsonObject jsonObject = jsonReader.readObject();

            // Récupération des champs du fichier JSON
            PORT = jsonObject.getInt("port");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Commentaire d'erreur en static pour la gestion de fichier
     */
    private static final String ERROR_ILLEGAL_ARGUMENT =
            "Arguments invalides. Arguments Attendus : java App <file>";
    private static final String ERROR_FILE_MAP_NOT_EXIST =
            "Fichier du réseau est introuvable ou est un repertoire";
    private static final String ERROR_FILE_TIME_NOT_EXIST =
            "Fichier des horaires est introuvable ou est un repertoire";
    private static final String ERROR_INCORRECT_FILE = "Le fichier est incorrect ";
    private static final String ERROR_SERVER_START = "Le serveur n'a pas demarré";

    public static void main(String[] args) {
        if (!argsIsOk(args)) {
            Logger.error(ERROR_ILLEGAL_ARGUMENT);
            return;
        }

        final File mapFile = new File(args[0]);
        if (!isFile(mapFile)) {
            Logger.error(ERROR_FILE_MAP_NOT_EXIST);
            return;
        }

        try {
            final Server server = new Server(mapFile.getPath(), PORT, true);
            if (hasCsvTimeFile(args)) {
                final File timeFile = new File(args[1]);
                if (!isFile(timeFile)) {
                    Logger.error(ERROR_FILE_TIME_NOT_EXIST);
                    return;
                }
                server.updateTime(timeFile.getPath());
            }
            server.start();
        } catch (FileNotFoundException e) {
            Logger.error(ERROR_FILE_MAP_NOT_EXIST);
        } catch (PlanParser.IncorrectFileFormatException e) {
            Logger.error(ERROR_INCORRECT_FILE);
        } catch (IOException e) {
            Logger.error(ERROR_SERVER_START);
        } catch (InconsistentDataException e) {
            Logger.error(e.getMessage());
        }
    }

    /**
     * Cette fonction renvoie un vrai si les arguments sont correctes s'ils respectent le formatage
     * ou faux si les arguments ne respectent pas le formatage
     *
     * @param args l'ensemble des arguments
     * @return boolean
     */
    static boolean argsIsOk(String[] args) {
        int length = args.length;
        return length == 1 || length == 2;
    }

    static boolean hasCsvTimeFile(String[] args) {
        return args.length == 2;
    }

    static boolean isFile(File mapFile) {
        return mapFile.exists() && !mapFile.isDirectory();
    }
}
