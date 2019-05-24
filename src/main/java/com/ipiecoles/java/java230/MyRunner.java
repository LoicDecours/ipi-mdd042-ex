package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = "^[\\p{L}- ]*$";
    private static final String REGEX_PRENOM = "^[\\p{L}- ]*$";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    //regex du salaire (nombre.nombre)
    private static final String REGEX_SALAIRE = "^[0-9]+(\\.[0-9]{1,2})?$";
    //regex du chiffre d'affaire (nombre.nombre)
    private static final String REGEX_CA = "^[0-9]+(\\.[0-9]{1,2})?$";
    //regex de la performance (0-100)
    private static final String REGEX_PERF = "^[0-9]$|^[1-9][0-9]$|^(100)$";

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName){
        Stream<String> stream;
        logger.info("Lecture du fichier : " + fileName);

        try{
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        } catch(IOException e){
            logger.error("Problème dans l'ouverture du fichier " + fileName);
            return new ArrayList<>();
        }
        List<String> lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size() + " lignes lues");

        for (int i = 0; i < lignes.size(); i++){
            try {
                processLine(lignes.get(i));
            } catch (BatchException e) {
                logger.error("Ligne " + (i+1) + " : " + e.getMessage() + " => " + lignes.get(i));
            }
        }
        //TODO

        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        String premiereLettre = ligne.substring(0,1);
        switch (premiereLettre){
            case "T" :
                processTechnicien(ligne);
                break;
            case "M" :
                processManager(ligne);
                break;
            case "C" :
                processCommercial(ligne);
                break;
            default :
                throw new BatchException("Type d'employé inconnu");

        }
    }

    /**
     * Méthode qui regroupe les méthodes communes à tout les types d'employés et les ajoute à l'employé créé
     * @param ligneEmploye la ligne contenant les infos de l'employé à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processEmploye (String ligneEmploye, Employe e) throws BatchException{
        String[] employeFields = ligneEmploye.split(",");

        //Contrôle le matricule
        if (!employeFields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("la chaîne " + employeFields[0] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
        }

        //Contrôle le nom
        if(!employeFields[1].matches(REGEX_NOM)){
            throw new BatchException("la chaîne " + employeFields[1] + " ne respecte pas l'expression régulière " + REGEX_NOM);
        }

        //Contrôle le prénom
        if (!employeFields[2].matches(REGEX_PRENOM)){
            throw new BatchException("la chaîne " + employeFields[2] + " ne respecte pas l'expression régulière " + REGEX_PRENOM);
        }

        //Contrôle la date
        LocalDate date;
        try {
            date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(employeFields[3]);
        } catch (Exception exc){
            throw new BatchException(employeFields[3] + " ne respecte pas le format de date dd/MM/yyyy");
        }

        //Contrôle le salaire
        if (!employeFields[4].matches(REGEX_SALAIRE)){
            throw new BatchException(employeFields[4] + " n'est pas un nombre valide pour le salaire");
        }
        Double salaire = Double.parseDouble(employeFields[4]);

        e.setMatricule(employeFields[0]);
        e.setNom(employeFields[1]);
        e.setPrenom(employeFields[2]);
        e.setDateEmbauche(date);
        e.setSalaire(salaire);
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        String[] commercialFields = ligneCommercial.split(",");

        //Contrôle la longueur de la ligne
        if (commercialFields.length != NB_CHAMPS_COMMERCIAL){
            throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + commercialFields.length);
        }
        Commercial c = new Commercial();
        processEmploye(ligneCommercial,c);

        //Contrôle le Chiffre d'affaire
        if (!commercialFields[5].matches(REGEX_CA)){
            throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + commercialFields[5]);
        }
        Double CA = Double.parseDouble(commercialFields[5]);

        //Contrôle la performance
        if (!commercialFields[6].matches(REGEX_PERF)){
            throw new BatchException("La performance du commercial est incorrecte : " + commercialFields[6]);
        }
        Integer perf = Integer.parseInt(commercialFields[6]);

        c.setCaAnnuel(CA);
        c.setPerformance(perf);
        employes.add(c);
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        String[] managerFields = ligneManager.split(",");

        Manager m = new Manager();

        //Contrôle la longueur de la ligne
        if (managerFields.length != NB_CHAMPS_MANAGER){
            throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + managerFields.length);
        }

        processEmploye(ligneManager, m);

        employes.add(m);
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        String[] technicienFields = ligneTechnicien.split(",");

        //Contrôle la longueur de la ligne
        if (technicienFields.length != NB_CHAMPS_TECHNICIEN){
            throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + technicienFields.length);
        }

        Technicien t = new Technicien();

        //Contrôle si le grade est un chiffre
        Integer grade;
        try{
            grade = Integer.parseInt(technicienFields[5]);
        } catch (Exception e){
            throw new BatchException("Le grade du technicien est incorrect : " + technicienFields[5]);
        }

        //Contrôle si le grade est compris entre 1 et 5 et le set si valide
        try{
            t.setGrade(grade);
        }catch (Exception e){
            throw new BatchException("Le grade doit être compris entre 1 et 5");
        }

        processEmploye(ligneTechnicien, t);

        //Contrôle la validité du matricule manager
        if (!technicienFields[6].matches(REGEX_MATRICULE_MANAGER)){
            throw new BatchException("la chaîne " + technicienFields[6] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE_MANAGER);
        }
        //Contrôle si le manager existe dans la BDD puis dans le fichier
        Manager manager = managerRepository.findByMatricule(technicienFields[6]);
        if (manager == null){
            for (int i = 0; i < employes.size(); i++){
                if(!(employes.get(i) instanceof Manager) || !employes.get(i).getMatricule().equals(technicienFields[6])){
                    throw new BatchException("Le manager de matricule " + technicienFields[6] + " n'a pas été trouvé dans le fichier ou en BDD");
                }else {
                    manager = (Manager) employes.get(i);
                    break;
                }
            }
        }

        t.setManager(manager);
        employes.add(t);
    }
}
