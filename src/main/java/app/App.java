/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app;

import app.map.Map;

import java.io.File;
import java.io.FileNotFoundException;

public class App {

    /**
     * Commentaire d'erreur en static pour la gestion de fichier
     */
    private final static String errorIllegalArgument = "[Erreur] Arguments invalides. Arguments Attendus : java App <file>";
    private final static String errorFileNotExist = "[Erreur] Fichier introuvable ou est un repertoire";
    private final static String errorIncorrectFile = "[Erreur] Le fichier est incorrect ";


    private final static String succesMapCreate = "Object Map crée avec succes ";

    private static Map map;

    /**
     * Cette fonction permet de recup l'instance de map
     * @return L'object Map
     */
    public static Map getInstanceOfMap(){
        return map;
    }

    public static void main(String[] args) {
       if(argsIsOk(args)) {
           final File file = new File(args[0]);
           if (!file.exists() || file.isDirectory()) print(errorFileNotExist);
           else {
               try {
                   map = new Map(file.getPath());
                   System.out.println(succesMapCreate);
               } catch (FileNotFoundException e) {
                   print(errorFileNotExist);
               } catch (Map.IncorrectFileFormatException e) {
                   print(errorIncorrectFile);
               }
           }
       }
    }

    /**
     * Cette fonction renvoie un vrai si les arguments sont correctes s'ils respectent le formatage
     * ou faux si les arguments ne respectent pas le formatage
     * @param args l'ensemble des arguments
     * @return boolean
     */
    public static boolean argsIsOk(String[] args) {
        if (args.length < 1) {
            print(errorIllegalArgument);
            return false;
        } else if (args.length > 3) {
            print(errorIllegalArgument);
            return false;
        }
            return true;
        }

    /**
     * Fonction d'affichage
     * @param msg le tableau des messages pour les faire afficher
     */
    private static void print(String... msg){
        for (String line: msg) System.out.println(line);
    }
}
