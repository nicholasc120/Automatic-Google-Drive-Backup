/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.*;
import java.io.*;

/**
 *
 * @author cerva
 */
public class ListOfFiles {

    /*
     this is a text file containing every document to back up -- but how do we organize it on the google drive side?
     what did i mean by this
     I meant organizing into folders on the drive side
     */
    private static final String TEXT_FILE_OF_FILES_TO_BACK_UP = "C:\\Users\\cerva\\Documents\\Projects\\DriveDocumentBackup\\src\\main\\resources\\documents.txt";
    private ArrayList<String> documents;

    public ListOfFiles() {
        try {
            Scanner sc = new Scanner(new java.io.File(TEXT_FILE_OF_FILES_TO_BACK_UP));
            documents = new ArrayList<>();

            while (sc.hasNext()) {
                documents.add(sc.nextLine());
            }

        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public ArrayList<String> getDocuments() {
        return documents;
    }

    public void addNewFile(String s) {
        documents.add(s);
        updateFileList();
    }

    public void updateFileList() {
        try {
            FileWriter fw = new FileWriter(TEXT_FILE_OF_FILES_TO_BACK_UP);
            PrintWriter pw = new PrintWriter(fw);

            for (String document : documents) {
                pw.println(document);
            }
            pw.close();
            fw.close();

        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
