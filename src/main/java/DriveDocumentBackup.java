
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class DriveDocumentBackup {

    private static final String APPLICATION_NAME = "Drive Document Backup";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart. If modifying
     * these scopes, delete your previously saved tokens/ folder.
     */
    //private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = DriveDocumentBackup.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the names and IDs for up to 10 files.
        /*
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
        /**
         * It might be a good idea to have a thread that continuously checks all
         * the files on TEXT_FILE_OF_FILES_TO_BACK_UP alongside a main GUI
         * thread that allows you to add more files to back up
         *
         *
         * Back up files IF the file does not exist on the drive OR the date
         * modified on the drive is older than the date modified on the local
         * version
         *
         */
        /////* create a folder */////
        String folderName = "My Document Backups";
        //need to see if folder is already created to begin with -- if not then create it
        Drive.Files.List request = service.files().list().setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + folderName + "'");
        FileList folders = request.execute();
        //if files is empty then it doesn't exist

        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder;
        if (folders == null || folders.isEmpty() || folders.getFiles().size() == 0) {
            folder = service.files().create(fileMetadata).setFields("id, lastModified").execute();
        } else {
            folder = folders.getFiles().get(0);
        }
        System.out.println("Folder ID: " + folder.getId());
        String folderId = folder.getId();

        ListOfFiles lof = new ListOfFiles();
        theGUI gui = new theGUI(lof, folderId, service);
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        FileChecker fc = new FileChecker(lof, folderId, service);
        ex.scheduleWithFixedDelay(fc,0,1, TimeUnit.MINUTES);
    }

}
