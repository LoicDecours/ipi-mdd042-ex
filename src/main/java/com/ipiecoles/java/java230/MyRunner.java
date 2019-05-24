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
    private static final String REGEX_SALAIRE = "^[0-9]*.[0-9]$";
    //regex du chiffre d'affaire (nombre.nombre)
    private static final String REGEX_CA = "^[0-9]*.[0-9]$";
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

        //Contrôle le matricule
        if (!commercialFields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("la chaîne " + commercialFields[0] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
        }

        //Contrôle le nom
        if(!commercialFields[1].matches(REGEX_NOM)){
            throw new BatchException("la chaîne " + commercialFields[1] + " ne respecte pas l'expression régulière " + REGEX_NOM);
        }

        //Contrôle le prénom
        if (!commercialFields[2].matches(REGEX_PRENOM)){
            throw new BatchException("la chaîne " + commercialFields[2] + " ne respecte pas l'expression régulière " + REGEX_PRENOM);
        }

        //Contrôle la date
        LocalDate date;
        try {
            date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(commercialFields[3]);
        } catch (Exception e){
            throw new BatchException(commercialFields[3] + " ne respecte pas le format de date dd/MM/yyyy");
        }

        //Contrôle le salaire
        if (!commercialFields[4].matches(REGEX_SALAIRE)){
            throw new BatchException(commercialFields[4] + " n'est pas un nombre valide pour le salaire");
        }
        Double salaire = Double.parseDouble(commercialFields[4]);

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


        Commercial c = new Commercial();
        c.setMatricule(commercialFields[0]);
        c.setNom(commercialFields[1]);
        c.setPrenom(commercialFields[2]);
        c.setDateEmbauche(date);
        c.setSalaire(salaire);
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

        //Contrôle la longueur de la ligne
        if (managerFields.length != NB_CHAMPS_MANAGER){
            throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + managerFields.length);
        }
        //Contrôle le matricule
        if (!managerFields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("la chaîne " + managerFields[0] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
        }
        //Contrôle le nom
        if(!managerFields[1].matches(REGEX_NOM)){
            throw new BatchException("la chaîne " + managerFields[1] + " ne respecte pas l'expression régulière " + REGEX_NOM);
        }

        //Contrôle le prénom
        if (!managerFields[2].matches(REGEX_PRENOM)){
            throw new BatchException("la chaîne " + managerFields[2] + " ne respecte pas l'expression régulière " + REGEX_PRENOM);
        }

        //Contrôle la date
        LocalDate date;
        try {
            date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(managerFields[3]);
        } catch (Exception e){
            throw new BatchException(managerFields[3] + " ne respecte pas le format de date dd/MM/yyyy");
        }

        //Contrôle le salaire
        if (!managerFields[4].matches(REGEX_SALAIRE)){
            throw new BatchException(managerFields[4] + " n'est pas un nombre valide pour le salaire");
        }
        Double salaire = Double.parseDouble(managerFields[4]);

        Manager m = new Manager();
        m.setMatricule(managerFields[0]);
        m.setNom(managerFields[1]);
        m.setPrenom(managerFields[2]);
        m.setDateEmbauche(date);
        m.setSalaire(salaire);
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

        //Contrôle le matricule
        if (!technicienFields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("la chaîne " + technicienFields[0] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
        }

        //Contrôle le nom
        if(!technicienFields[1].matches(REGEX_NOM)){
            throw new BatchException("la chaîne " + technicienFields[1] + " ne respecte pas l'expression régulière " + REGEX_NOM);
        }

        //Contrôle le prénom
        if (!technicienFields[2].matches(REGEX_PRENOM)){
            throw new BatchException("la chaîne " + technicienFields[2] + " ne respecte pas l'expression régulière " + REGEX_PRENOM);
        }

        //Contrôle la date
        LocalDate date;
        try {
            date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(technicienFields[3]);
        } catch (Exception e){
            throw new BatchException(technicienFields[3] + " ne respecte pas le format de date dd/MM/yyyy");
        }

        //Contrôle le salaire
        if (!technicienFields[4].matches(REGEX_SALAIRE)){
            throw new BatchException(technicienFields[4] + " n'est pas un nombre valide pour le salaire");
        }
        Double salaire = Double.parseDouble(technicienFields[4]);

        //Contrôle si le grade est un chiffre
        Integer grade;
        try{
            grade = Integer.parseInt(technicienFields[5]);
        } catch (Exception e){
            throw new BatchException("Le grade du technicien est incorrect : " + technicienFields[5]);
        }

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

        Technicien t = new Technicien();
        t.setMatricule(technicienFields[0]);
        t.setNom(technicienFields[1]);
        t.setPrenom(technicienFields[2]);
        t.setDateEmbauche(date);

        //Contrôle si le grade est compris entre 1 et 5 et le set si valide
        try{
            t.setGrade(grade);
        }catch (Exception e){
            throw new BatchException("Le grade doit être compris entre 1 et 5");
        }

        t.setSalaire(salaire);
        t.setManager(manager);
        employes.add(t);

    }
}
