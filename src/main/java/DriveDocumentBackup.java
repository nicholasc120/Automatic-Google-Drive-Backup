
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

    /*
     this is a text file containing every document to back up -- but how do we organize it on the google drive side?
     */
    private static final String TEXT_FILE_OF_FILES_TO_BACK_UP = "/documents.txt";

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
            folder = service.files().create(fileMetadata).setFields("id").execute();
        } else {
            folder = folders.getFiles().get(0);
        }
        System.out.println("Folder ID: " + folder.getId());
        String folderId = folder.getId();

        /////* insert a file into a folder */////
        //String folderId = folder.getId();
        //create an open-save dialog to choose the file
        //also save this filepath to documments.txt
        java.io.File filePath = new java.io.File("C:\\Users\\cerva\\Documents\\Projects\\DriveDocumentBackup\\src\\main\\test files\\Avengers Infinity War.mp4");

        //add timestamped metadata for last modified
        fileMetadata = new File();
        //this will need to be updated with each file name
        fileMetadata.setName("txt.txt");
        fileMetadata.setParents(Collections.singletonList(folderId));

        //Arg 1 is a MIME type?
        FileContent mediaContent = new FileContent("text/plain", filePath);
        File file = service.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();
        System.out.println("File ID: " + file.getId());

        /////* download a file */////
        String fileId = file.getId();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream);

        File downloadedFile = service.files().get(fileId).execute();
        System.out.println("File Name: " + downloadedFile.getName());

        try (OutputStream fileOutputStream = new FileOutputStream(downloadedFile.getName())) {
            outputStream.writeTo(fileOutputStream);
        }
    }
}
