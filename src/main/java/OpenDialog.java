import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class OpenDialog extends JFrame {
   
    public String showOpenDialog() {
        this.setAlwaysOnTop (true);
        JFileChooser jfc = new JFileChooser();
        int status = jfc.showOpenDialog(this);
        if (status == JFileChooser.APPROVE_OPTION) {
            java.io.File theFile = jfc.getSelectedFile();
            String thePath = theFile.getAbsolutePath();
            return thePath;
        }
        return null;
    }
    
}