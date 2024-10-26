package javaxt.opencv;

//Java imports
import java.io.*;
import java.awt.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.Pattern;

//JavaXT imports
import javaxt.io.Jar;
import static javaxt.utils.Console.*;

//OpenCV imports
import org.opencv.dnn.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;


//******************************************************************************
//**  OpenCV
//******************************************************************************
/**
 *  Command line interface used to test OpenCV
 *
 ******************************************************************************/

public class OpenCV {


  //**************************************************************************
  //** main
  //**************************************************************************
  /** Entry point for the application
   *  @param arguments Command line arguments
   */
    public static void main(String[] arguments) throws Exception {
        detectFaces(arguments);

    }


  //**************************************************************************
  //** detectFaces
  //**************************************************************************
  /** Used to detect faces on an image using the "yunet" model from opencv_zoo
   */
    private static void detectFaces(String[] arguments) throws Exception {


      //Parse command line arguments
        var args = parseArgs(arguments);
        var modelFile = new javaxt.io.File(args.get("-model"));
        var imageFile = new javaxt.io.File(args.get("-input"));


      //Load library
        loadLib();


      //Load the YuNet model (validation purposes only)
        Net net = Dnn.readNetFromONNX(modelFile.toString()); //"yunet.onnx"
        console.log("Loaded model " + net.getLayerNames().size() + " with layers");



      //Load image
        Mat image = Imgcodecs.imread(imageFile.toString());
        int orgWidth = image.cols();
        int orgHeight = image.rows();
        console.log("Loaded " + orgWidth + "x" + orgHeight + " image");



      //Resize image as needed
        int maxWidth = 600;
        int inputWidth = orgWidth;
        int inputHeight = orgHeight;
        if (orgWidth>maxWidth){

            // Calculate the new height while maintaining aspect ratio
            int newHeight = (int) (image.height() * (maxWidth / (double) image.width()));

            // Resize the image
            Mat resizedImage = new Mat();
            Imgproc.resize(image, resizedImage, new Size(maxWidth, newHeight));

            image = resizedImage;
            inputWidth = image.cols();
            inputHeight = image.rows();
        }
        //Mat blob = Dnn.blobFromImage(image, 1.0, new Size(640, 640), new Scalar(0, 0, 0), true, false);



      //Set up model
        FaceDetectorYN tn = FaceDetectorYN.create(
            modelFile.toString(), //model name
            "", //
            new Size(inputWidth, inputHeight), //dimensions of the input image
            0.6f, //confThreshold
            0.3f, //nmsThreshold
            5000, //topK
            0, //backendId
            0 //targetId
        );
        //tn.setInputSize(input_size);


      //Run image detection
        Mat detections = new Mat();
        tn.detect(image, detections);
        console.log("Found " +  detections.rows() + " faces");


      //Extract faces
        var faces = new HashMap<Rect, Double>();
        for (int i=0; i<detections.rows(); i++) {

            Mat confidence = detections.row(i).colRange(2, 3);
            double confidenceValue = confidence.get(0, 0)[0];

            Mat bbox = detections.row(i); //.colRange(0, 2);
            int x = (int) bbox.get(0, 0)[0];
            int y = (int) bbox.get(0, 1)[0];
            int w = (int) bbox.get(0, 2)[0];
            int h = (int) bbox.get(0, 3)[0];



            if (orgWidth!=inputWidth || orgHeight!=inputHeight){
                x = (x*orgWidth)/inputWidth;
                y = (y*orgHeight)/inputHeight;
                w = (w*orgWidth)/inputWidth;
                h = (h*orgHeight)/inputHeight;
            }


            Rect rect = new Rect(x, y, w, h);
            faces.put(rect, confidenceValue);
        }


        javaxt.io.Image img = imageFile.getImage();
        addRectangles(faces, img);
        img.saveAs(imageFile.getDirectory() + imageFile.getName(false) + "_FACES_LG.jpg");
    }


  //**************************************************************************
  //** addRectangles
  //**************************************************************************
  /** Used to render rectangles on an image.
   */
    private static void addRectangles(HashMap<Rect, Double> rects, javaxt.io.Image img){
        Graphics2D g2d = img.getBufferedImage().createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(new Color(255, 0, 0));
        g2d.setStroke(new BasicStroke(3));
        for (Rect rect : rects.keySet()){
            g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        }

        g2d.dispose();
    }


  //**************************************************************************
  //** loadLib
  //**************************************************************************
  /** Used to extract and load a library (.dll, .so, etc) from the opencv
   *  jar file.
   */
    private static void loadLib() throws Exception {

      //Set relative path to the library
        OS os = OS.getCurrent();
        Arch arch = Arch.getCurrent();
        Jar jar = new Jar(nu.pattern.OpenCV.class);
        String path = "nu/pattern/opencv/" +
        (os.toString().toLowerCase()) + "/";
        String a = arch.toString();
        if (a.startsWith("X")) a = a.toLowerCase();
        path += a + "/";


      //Get library file extension
        String ext = null;
        if (os==OS.LINUX){
            ext = "so";
        }
        else if (os==OS.OSX){
            ext = "dylib";
        }
        else if (os==OS.WINDOWS){
            ext = "dll";
        }



      //Find library entry in the zip/jar file
        ZipEntry entry = null;
        String fileName = null;
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar.getFile()))){

            ZipEntry zipEntry;
            while((zipEntry = in.getNextEntry())!=null){
                String relPath = zipEntry.getName();
                if (relPath.startsWith(path + "opencv_java")){
                    if (relPath.endsWith(ext)){
                        entry = zipEntry;
                        path = relPath;
                        fileName = relPath.substring(relPath.lastIndexOf("/")+1);
                        break;
                    }
                }
            }
        }
        if (entry==null) throw new Exception("Failed to find library");



      //Extract library as needed
        java.io.File lib = new java.io.File(jar.getFile().getParentFile(), fileName);
        long checksum = entry.getCrc();
        if (lib.exists()){

          //Check whether the library equals the jar entry. Extract as needed.
            byte[] b = new byte[(int)lib.length()];

            try (java.io.DataInputStream is = new java.io.DataInputStream(new FileInputStream(lib))) {

                is.readFully(b, 0, b.length);

                java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                crc.update(b);
                if (checksum!=crc.getValue()){
                    lib.delete();
                    extractEntry(path, jar, lib);
                }
            }

        }
        else{

          //File does not exist so extract the library
            extractEntry(path, jar, lib);
        }



      //Load the library
        if (lib.exists()){
            System.load(lib.toString());
            console.log("Loaded!", Core.getVersionMajor(), Core.getVersionMinor());
        }
        else{
            throw new Exception("Failed to load library");
        }

    }


  //**************************************************************************
  //** extractEntry
  //**************************************************************************
  /** Used to extract an entry for a jar file
   */
    private static void extractEntry(String path, javaxt.io.Jar jar,
        java.io.File destination) throws Exception {

        destination.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(destination)){
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar.getFile()))){
                ZipEntry zipEntry;
                while((zipEntry = in.getNextEntry())!=null){
                    if (zipEntry.getName().equals(path)){

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        break;
                    }
                }
            }
        }
    }


  //**************************************************************************
  //** OS
  //**************************************************************************
    static enum OS {
      OSX("^[Mm]ac OS X$"),
      LINUX("^[Ll]inux$"),
      WINDOWS("^[Ww]indows.*");

      private final Set<Pattern> patterns;

      private OS(final String... patterns) {
        this.patterns = new HashSet<>();

        for (final String pattern : patterns) {
          this.patterns.add(Pattern.compile(pattern));
        }
      }

      private boolean is(final String id) {
        for (final Pattern pattern : patterns) {
          if (pattern.matcher(id).matches()) {
            return true;
          }
        }
        return false;
      }

      public static OS getCurrent() {
        final String osName = System.getProperty("os.name");

        for (final OS os : OS.values()) {
          if (os.is(osName)) {
            //logger.log(Level.FINEST, "Current environment matches operating system descriptor \"{0}\".", os);
            return os;
          }
        }

        throw new UnsupportedOperationException(String.format("Operating system \"%s\" is not supported.", osName));
      }
    }


  //**************************************************************************
  //** Arch
  //**************************************************************************
    static enum Arch {
      X86_32("i386", "i686", "x86"),
      X86_64("amd64", "x86_64"),
      ARMv7("arm"),
      ARMv8("aarch64", "arm64");

      private final Set<String> patterns;

      private Arch(final String... patterns) {
        this.patterns = new HashSet<String>(Arrays.asList(patterns));
      }

      private boolean is(final String id) {
        return patterns.contains(id);
      }

      public static Arch getCurrent() {
        final String osArch = System.getProperty("os.arch");

        for (final Arch arch : Arch.values()) {
          if (arch.is(osArch)) {
            //logger.log(Level.FINEST, "Current environment matches architecture descriptor \"{0}\".", arch);
            return arch;
          }
        }

        throw new UnsupportedOperationException(String.format("Architecture \"%s\" is not supported.", osArch));
      }
    }

}