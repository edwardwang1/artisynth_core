package artisynth.demos.dicom;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import artisynth.core.renderables.DicomViewer;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.fileutil.FileManager;

/**
 * DICOM image of the brain, raw encoding, using REGEX to limit files
 */
public class DicomTest extends RootModel {

   String dicom_url = "http://www.osirix-viewer.com/datasets/DATA/BRAINIX.zip";
   String dicom_folder = "BRAINIX/BRAINIX";
   
   public void build(String[] args) throws IOException {
      
      // download the BRAINIX dicom data if it does not already exist
      String localDir = ArtisynthPath.getSrcRelativePath(this, "data/BRAINIX");
      FileManager fileManager = new FileManager(localDir, "zip:" + dicom_url + "!/");
      fileManager.setConsoleProgressPrinting(true);
      fileManager.setOptions(FileManager.DOWNLOAD_ZIP); // download zip file first
      File dicomPath = fileManager.get(dicom_folder);   // do the download
      
      // I'm actually interested in the folder:
      //    BRAINIX/BRAINIX/IRM cerebrale, neuro-crane/T2W-FE-EPI - 501
      // but due to UTF-8 character encoding in the filenames, which is not supported 
      // on all systems, I'm accessing the desired files using a regular expression
      // on the parent folder
      DicomViewer dcp = new DicomViewer("Brain", dicomPath.getAbsolutePath(), 
         Pattern.compile(".*/T2W-FE-EPI - 501/.*\\.dcm"), /*check subdirectories*/true);
      
      addRenderable(dcp);
      
   }

   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      
      getMainViewer().setBackgroundColor(Color.WHITE);
   }
   
   @Override
   public String getAbout() {
      return "Loads and displays a DICOM image of the brain, which is automatically "
         + "downloaded from www.osirix-viewer.com";
   }
   
}
