import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * This class is a thread that checks all files with the drive version
 *
 * @author cerva
 */
public class FileChecker implements Runnable {

    private ListOfFiles lof;
    private String folderID;
    private Drive service;

    FileChecker(ListOfFiles lof, String folderID, Drive service) {
        this.lof = lof;
        this.folderID = folderID;
        this.service = service;
    }

    @Override
    public void run() {
        /*
         final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
         Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
         .setApplicationName(APPLICATION_NAME)
         .build();

         // Print the names and IDs for up to 10 files.
         FileList result = service.files().list()
         .setPageSize(10)
         .setFields("nextPageToken, files(id, name)")
         .execute();
         List<File> files = result.getFiles();
         if (files == null || files.isEmpty()) {
         System.out.println("No files found.");
         } else {
         System.out.println("Files:");
         for (File file : files) {
         System.out.printf("%s (%s)\n", file.getName(), file.getId());
         }
         }
         */

        /////* download a file */////
        /*
         String fileId = file.getId();
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         service.files().get(fileId).executeMediaAndDownloadTo(outputStream);

         File downloadedFile = service.files().get(fileId).execute();
         System.out.println("File Name: " + downloadedFile.getName());
        
        
         //gotta figure out where to save the file
         try (OutputStream fileOutputStream = new FileOutputStream(downloadedFile.getName())) {
         outputStream.writeTo(fileOutputStream);
         }
        
         System.out.println("Done, starting thread now");
        
         FileChecker object = new FileChecker();
         object.start();
         */
        try {
            System.out.println("Running file check");
            for (int i = 0; i < lof.getDocuments().size(); i++) {

                String[] pathAsAnArray = lof.getDocuments().get(i).split("\\\\");
                String fileName = pathAsAnArray[pathAsAnArray.length - 1];
                System.out.println("Searching for " + fileName + " in folder " + folderID);
                Drive.Files.List request = service.files().list().setQ("mimeType != 'application/vnd.google-apps.folder' and trashed=false and '" + folderID + "' in parents and name='" + fileName + "'")
                        .setFields("nextPageToken, files(id, name, modifiedTime)");
                //request = service.files().list().setQ("mimeType='application/vnd.google-apps.file' and trashed=false and "+folderID +" in parents ");

                FileList folders = request.execute();
                //if files is empty then it doesn't exist

                if (folders == null) {
                    System.out.println("Folders is null");
                }
                if (folders.isEmpty()) {
                    System.out.println("folders is empty");
                }
                if (folders.getFiles().size() == 0) {
                    System.out.println("folders has size of 0");
                }

                File theFile = folders.getFiles().get(0);

                //find the fileID of the file with the same name as the thing you're looking at
                System.out.println("File Name: " + fileName);
                System.out.println("File ID: " + theFile.getId());
                //String fileId = theFile.getId();

                System.out.println("Date Modified On Drive: " + theFile.getModifiedTime());
                //this is a long
                long lastModifiedOnDisk = (new java.io.File(lof.getDocuments().get(i)).lastModified());
                //DateTime constructor for long
                DateTime local = new DateTime(lastModifiedOnDisk);
                System.out.println("Date modified On Local: " + local);

                //then check the modified date of that file
                //only reupload the file IF the the local file has a date modified that is more recent than the cloud version
                if (theFile.getModifiedTime().getValue() < local.getValue()) {
                    System.out.println("local file is more recent");

                    java.io.File fileContent = new java.io.File(lof.getDocuments().get(i));
                    FileContent mediaContent = new FileContent("text/plain", fileContent);

                    File fileObjectWithUpdates = new File();
                    fileObjectWithUpdates.setDescription("This file was updated");

                    File updatedFile = service.files().update(theFile.getId(), fileObjectWithUpdates, mediaContent).execute();
                    
                    System.out.println("Uploaded updated file");
                } else {
                    System.out.println("cloud save up to date");
                    //don't do anything i guess
                }

            }
        } catch (Exception e) {

        }
    }
}
