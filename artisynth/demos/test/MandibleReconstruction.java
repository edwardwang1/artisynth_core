package artisynth.demos.test;

import java.io.*;
import java.text.NumberFormat;
import java.awt.Color;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.util.*;
import maspack.geometry.*;
import maspack.geometry.io.StlReader;
import maspack.interpolation.Interpolation;
import maspack.interpolation.Interpolation.Order;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.interpolation.NumericListSimplification;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.collision.*;
import java.awt.event.ActionEvent;

public class MandibleReconstruction extends RootModel {

   public static String rbpath =
      ArtisynthPath
         .getHomeRelativePath ("src/maspack/geometry/sampleData/", ".");

   PolygonalMesh mandibleMesh = null;
   PolygonalMesh fibulaMesh = null;
   PolygonalMesh tempMesh = null;
   PolygonalMesh plane1Mesh = null;
   PolygonalMesh plane2Mesh = null;
   PolygonalMesh resectionMesh = null;
   PolygonalMesh nonResectionMesh = null;
   PolylineMesh rdpMesh;
   PolygonalMesh clippedFibulaMesh = null;
   PolygonalMesh fibulaCuttingPlaneProx = null;
   PolygonalMesh fibulaCuttingPlanes[] = new PolygonalMesh[2];
   
   FixedMeshBody mandibleMeshBody;
   FixedMeshBody fibulaMeshBody;
   FixedMeshBody plane1MeshBody;
   FixedMeshBody plane2MeshBody;
   FixedMeshBody resectionMeshBody;
   FixedMeshBody nonResectionMeshBody;
   FixedMeshBody rdpMeshBody;
   RigidBody clippedFibulaMeshBody;
   
   NumericList plateNumericList;
   NumericList simpList;
   RigidBody tempMeshBody;

   PolygonalMeshRenderer meshRenderer;

   JButton loadMandibleButton, loadFibulaButton, alignButton, plateButton,
   createPlanesButton, clipMandibleButton, lineSimplificationButton, fibulaClipButton, fibulaPrepButton, transformButton;
   JFileChooser fileChooser;
   ImprovedFormattedTextField rdpMinDistance, rdpMaxSegments, fibulaDistanceProx, fibulaDistanceDis;

   ControlPanel myControlPanel;
   MechModel mechModel;
   
   HelperMathFunctions mathHelper = new HelperMathFunctions();
   HelperMeshFunctions meshHelper = new HelperMeshFunctions();
   
   int numberOfSegments = 3;

   private void addControlPanel () {

      // Buttons
      myControlPanel = new ControlPanel ("Control Panel", "");

      loadMandibleButton = new JButton ("Load  Mandible");
      loadMandibleButton.addActionListener (new LoadSTLFiles ());

      loadFibulaButton = new JButton ("Load Fibula");
      loadFibulaButton.addActionListener (new LoadSTLFiles ());

      alignButton = new JButton ("Align");
      alignButton.addActionListener (new AlignButtonClicked ());

      plateButton = new JButton ("Plate");
      plateButton.addActionListener (new PlateButtonClicked ());

      createPlanesButton = new JButton ("Create Planes");
      createPlanesButton.addActionListener (new CreatePlanesButtonClicked ());

      clipMandibleButton = new JButton ("Clip Mandible");
      clipMandibleButton.addActionListener (new ClipMandibleButtonClicked ());

      
      ///Line Simplification
      lineSimplificationButton = new JButton ("RDp Simp");
      lineSimplificationButton.addActionListener (new LineSimplificationButtonClicked ());
      

         //Setting Input Values for RDP Simplification Values
      JSplitPane minDistancePane = new JSplitPane();
      JLabel minDistanceLabel = new JLabel("Minimum Distance");
      NumberFormat floatFieldFormatter;
      floatFieldFormatter = NumberFormat.getNumberInstance();
      rdpMinDistance = new ImprovedFormattedTextField(floatFieldFormatter, 15);
      minDistancePane.setLeftComponent (minDistanceLabel);
      minDistancePane.setRightComponent (rdpMinDistance);
      
      
      JSplitPane maxSegmentsPane = new JSplitPane();
      JLabel maxSegmentsLabel = new JLabel("Max Segments");
      NumberFormat integerFieldFormatter;
      integerFieldFormatter = NumberFormat.getIntegerInstance();
      rdpMaxSegments = new ImprovedFormattedTextField(integerFieldFormatter, 3);
      maxSegmentsPane.setLeftComponent (maxSegmentsLabel);
      maxSegmentsPane.setRightComponent (rdpMaxSegments);
      
      ///Prep FIbula
      fibulaClipButton = new JButton("Prep Fibula");
      fibulaClipButton.addActionListener(new FibulaPrepButtonClicked());
      
         // Splitting Plane for Fibula distance from Prox and distal ends
      JSplitPane fibulaProxPane = new JSplitPane();
      JLabel fibulaProxLabel = new JLabel("Prox Distance");
      fibulaDistanceProx = new ImprovedFormattedTextField(floatFieldFormatter, 80);
      maxSegmentsPane.setLeftComponent (fibulaProxLabel);
      maxSegmentsPane.setRightComponent (fibulaDistanceProx);
      
      
      JSplitPane fibulaDisPane = new JSplitPane();
      JLabel fibulaDisLabel = new JLabel("Distal Distance");
      fibulaDistanceDis = new ImprovedFormattedTextField(floatFieldFormatter, 80);
      maxSegmentsPane.setLeftComponent (fibulaDisLabel);
      maxSegmentsPane.setRightComponent (fibulaDistanceDis);
      
         
      transformButton = new JButton("Transform");
      transformButton.addActionListener(new TransformButtonClicked());
      
      // File Chooser
      fileChooser = new JFileChooser ();

      myControlPanel.addWidget (loadMandibleButton);
      myControlPanel.addWidget (loadFibulaButton);
      myControlPanel.addWidget (alignButton);
      myControlPanel.addWidget (plateButton);
      myControlPanel.addWidget (createPlanesButton);
      myControlPanel.addWidget (clipMandibleButton);
      myControlPanel.addWidget (lineSimplificationButton);
      myControlPanel.addWidget (minDistancePane);
      myControlPanel.addWidget (maxSegmentsPane);
      myControlPanel.addWidget (fibulaClipButton);
      myControlPanel.addWidget (transformButton);
      addControlPanel (myControlPanel);
   }

   public class LoadSTLFiles extends AbstractAction {
      public LoadSTLFiles () {
         putValue (NAME, "Load Stl Files");
      }

      @Override
      public void actionPerformed (ActionEvent evt) {
         if (evt.getSource () == loadMandibleButton) {
            int returnVal = fileChooser.showOpenDialog (fileChooser);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
               File file = fileChooser.getSelectedFile ();
               // LoadingMesh
               mandibleMesh =
                  meshHelper.readMesh (file.getAbsolutePath (), file.getName ());
               mandibleMeshBody = new FixedMeshBody (mandibleMesh);
               mechModel.addMeshBody (mandibleMeshBody);
               Point3d pmin = new Point3d ();
               Point3d pmax = new Point3d ();
               mandibleMesh.getLocalBounds (pmin, pmax);
               mechModel.setBounds (pmin, pmax);
               rerender ();
            }
            else {
               System.out.println ("Open command cancelled by user.");
            }

         }
         else if (evt.getSource () == loadFibulaButton) {
            int returnVal = fileChooser.showOpenDialog (fileChooser);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
               File file = fileChooser.getSelectedFile ();
               // LoadingMesh
               fibulaMesh = meshHelper.readMesh (file.getAbsolutePath (), file.getName ());
               fibulaMeshBody = new FixedMeshBody (fibulaMesh);
               mechModel.addMeshBody (fibulaMeshBody);
               rerender ();
            }
            else {
               System.out.println ("Open command cancelled by user.");
            }
         }
      }
   }

   public class AlignButtonClicked extends AbstractAction {
      // Aligns the fibula to the mandible by translating the fibula centroid to
      // the mandible centroid
      public AlignButtonClicked () {
         putValue (NAME, "Align Button Clicked");
      }

      @Override
      public void actionPerformed (ActionEvent evt) {
         System.out.println ("Begin Alignment");
         Vector3d mandibleCentroid = new Vector3d ();
         mandibleMesh.computeCentroid (mandibleCentroid);
         Vector3d fibulaCentroid = new Vector3d ();
         fibulaMesh.computeCentroid (fibulaCentroid);

         Vector3d translation = mandibleCentroid.sub (fibulaCentroid);
         RigidTransform3d transform = new RigidTransform3d ();
         transform.setTranslation (translation);

         fibulaMesh.transform (transform);
         fibulaMeshBody = new FixedMeshBody (fibulaMesh);
         rerender ();

         System.out.println ("End Alignment");

        
      }
   }

   public class PlateButtonClicked extends AbstractAction {
      // Interpolates the frame marks on the mandible to create a list of
      // Points, stored as knots in numericList
      public PlateButtonClicked () {
         putValue (NAME, "Plate Mandible");
      }

      @Override
      public void actionPerformed (ActionEvent evt) {
         FrameMarker[] frameMarkers = tempMeshBody.getFrameMarkers ();
         int size = frameMarkers.length;
         Point3d[] points = new Point3d[size];
         plateNumericList = new NumericList (3);
         Interpolation cubic = new Interpolation ();
         cubic.setOrder (Order.SphericalCubic);
         plateNumericList.setInterpolation (cubic);
         int numberOfSubdivisions = 5000;

         
         for (int i = 0; i < size; i++) {
            points[i] = frameMarkers[i].getLocation ();
            plateNumericList.add (points[i], i * numberOfSubdivisions / (size - 1));
         }


         VectorNd[] interpolatedVectors = new VectorNd[numberOfSubdivisions];

         for (int i = 0; i < numberOfSubdivisions; i++) {
            interpolatedVectors[i] = new VectorNd ();
            interpolatedVectors[i].setSize (3);
            plateNumericList.interpolate (interpolatedVectors[i], i);

         }

         // Adding Results of interpolation to the numericList
         for (int i = 0; i < numberOfSubdivisions; i++) {
            plateNumericList.add (interpolatedVectors[i], i);
         }


         int newSize = plateNumericList.getNumKnots ();
         Point3d[] interpolatedPoints = new Point3d[newSize];
         int[][] indices = new int[newSize - 1][2];

         
         Iterator<NumericListKnot> itr = plateNumericList.iterator ();

         int i = 0;
         while (itr.hasNext ()) {
            interpolatedPoints[i] = new Point3d (itr.next ().v);
            i++;
         }

         for (i = 0; i < newSize - 1; i++) {
            indices[i][0] = i;
            indices[i][1] = i + 1;
         }
         PolylineMesh sphericalPolyLine =
            MeshFactory.createSphericalPolyline (50, 50, 50);

         PolylineMesh polyline = new PolylineMesh ();
         polyline.addMesh (sphericalPolyLine);
         polyline.set (interpolatedPoints, indices);

         FixedMeshBody plate = new FixedMeshBody ("Plate", polyline);

         mechModel.addMeshBody (plate);
         rerender ();
      }
   }

   public class CreatePlanesButtonClicked extends AbstractAction {
      public CreatePlanesButtonClicked () {
         putValue (NAME, "Create Planes");
      }

      @Override
      public void actionPerformed (ActionEvent evt) {
         Vector3d mandibleCentroid = new Vector3d ();
         tempMesh.computeCentroid (mandibleCentroid);

         plane1Mesh = MeshFactory.createPlane (90, 90);

         maspack.matrix.RigidTransform3d transform =
            new maspack.matrix.RigidTransform3d ();

         // MeshPlane is created at (0,0,0)
         transform.setTranslation (mandibleCentroid);
         plane1Mesh.transform (transform);

         plane2Mesh = plane1Mesh.copy ();

         plane1MeshBody = new FixedMeshBody ("Plane1", plane1Mesh);
         FixedMeshBody plane2Body = new FixedMeshBody ("Plane2", plane2Mesh);
         mechModel.addMeshBody (plane1MeshBody);
         mechModel.addMeshBody (plane2Body);
         rerender ();

      }
   }

   public class ClipMandibleButtonClicked extends AbstractAction {
      public ClipMandibleButtonClicked () {
         putValue (NAME, "Just Click It");
      }

      @Override
      public void actionPerformed (ActionEvent evt) {
         mechModel.removeMeshBody (resectionMeshBody);
         mechModel.removeMeshBody (nonResectionMeshBody);

         SurfaceMeshIntersector intersector =
            new maspack.collision.SurfaceMeshIntersector ();
         long startTime = System.nanoTime ();

         // Differencing Planes in sequence to get resection
         resectionMesh = intersector.findDifference01 (tempMesh, (PolygonalMesh)plane1MeshBody.getMesh ());
         resectionMesh =
            intersector.findDifference01 (resectionMesh, plane2Mesh);
         resectionMeshBody = new FixedMeshBody ("Resection", resectionMesh);

         //Creating new set of planes that have the opposite normals of the clipping planes
         Vector3d mandibleCentroid = new Vector3d ();
         tempMesh.computeCentroid (mandibleCentroid);
         PolygonalMesh reversePlane1Mesh =  MeshFactory.createPlane (90, 90);
         maspack.matrix.RigidTransform3d transform =
            new maspack.matrix.RigidTransform3d ();

         // MeshPlane is created at (0,0,0)
         transform.setTranslation (mandibleCentroid);
         reversePlane1Mesh.transform (transform);
         
         // Rotating Plane to be upside down
         maspack.matrix.RigidTransform3d rotmat = new maspack.matrix.RigidTransform3d();
         AxisAngle axisAngle = new AxisAngle();
         axisAngle.set (new Vector3d(0,1,0), Math.PI);
         rotmat.setRotation(axisAngle);
         reversePlane1Mesh.transform(rotmat);
         
         //Creating second plane here as not not redo previous operations
         PolygonalMesh reversePlane2Mesh = reversePlane1Mesh.copy ();
         
         //Corrective Transform of Plane1
         Vector3d originalCentroidPlane1 = new Vector3d();
         plane1Mesh.computeCentroid(originalCentroidPlane1);
         
         Vector3d newCentroid1 = new Vector3d();
         reversePlane1Mesh.computeCentroid (newCentroid1);
         RigidTransform3d correctiveTranslationPlane1 = new RigidTransform3d();
         correctiveTranslationPlane1.setTranslation(originalCentroidPlane1.sub(newCentroid1));
         reversePlane1Mesh.transform (correctiveTranslationPlane1);
         reversePlane1Mesh.setMeshToWorld(plane1Mesh.getMeshToWorld ());

         
         //Corrective Transform of Plane2
         Vector3d originalCentroidPlane2 = new Vector3d();
         plane2Mesh.computeCentroid(originalCentroidPlane2);
         
         Vector3d newCentroid2 = new Vector3d();
         reversePlane2Mesh.computeCentroid (newCentroid2);
         RigidTransform3d correctiveTranslationPlane2 = new RigidTransform3d();
         correctiveTranslationPlane2.setTranslation(originalCentroidPlane2.sub(newCentroid2));
         reversePlane2Mesh.transform (correctiveTranslationPlane2);
         reversePlane2Mesh.setMeshToWorld(plane2Mesh.getMeshToWorld ());

         // Differencing reverse plane from first mesh
         nonResectionMesh =
            intersector.findDifference01 (tempMesh, reversePlane1Mesh);

         // Differencing reverse plane from second mesh
         PolygonalMesh nonResectionMesh2 = new PolygonalMesh ();
         nonResectionMesh2 =
            intersector.findDifference01 (tempMesh, reversePlane2Mesh);

         // Combining first and second unresected mesh
         nonResectionMesh.addMesh(nonResectionMesh2);
         nonResectionMeshBody =
            new FixedMeshBody ("Unresected", nonResectionMesh);


         mechModel.addMeshBody (resectionMeshBody);
         mechModel.addMeshBody (nonResectionMeshBody);
         tempMeshBody.getRenderProps ().setVisible (false);
         rerender ();
      }
   }

   public class LineSimplificationButtonClicked extends AbstractAction {
      public LineSimplificationButtonClicked () {
         putValue (NAME, "Test2");
      }

      @Override
      public void actionPerformed (ActionEvent evt) {
         mechModel.removeMeshBody (rdpMeshBody);
         // Getting the upper and lower bounds in knot form
         
         Vector3d centroid1 = new Vector3d();
         plane1Mesh.computeCentroid (centroid1);
         NumericListKnot knot1 = mathHelper.closestNumericListKnotToPlane(plane1Mesh.getNormal(0), centroid1, plane1Mesh.getMeshToWorld (), plateNumericList);
         
         
         Vector3d centroid2 = new Vector3d();
         plane2Mesh.computeCentroid (centroid2);
         NumericListKnot knot2 = mathHelper.closestNumericListKnotToPlane(plane2Mesh.getNormal(0), centroid2, plane2Mesh.getMeshToWorld (), plateNumericList);
         
         // Creating new numericList to only contain knots between the bounds
         NumericListKnot lowestKnot = knot1;
         NumericListKnot highestKnot = knot2;
         if (knot2.t < lowestKnot.t) {
            lowestKnot = knot2;
            highestKnot = knot1;
         }
         
         NumericList curatedList = new NumericList(3);
         Iterator<NumericListKnot> itr = plateNumericList.iterator ();
         NumericListKnot tempKnot;
         while (itr.hasNext ()) {
            tempKnot = itr.next();
            if (tempKnot.t > lowestKnot.t) {
               curatedList.add (tempKnot);
            }
         }
         curatedList.clearAfter (highestKnot);
         
         
         // Creating a numericList that is the result of the line simplification
         NumericListSimplification simplifier= new NumericListSimplification();
         simpList = new NumericList(3);
         
         
         simplifier.bisectSimplifyDouglasPeucker(curatedList, Double.parseDouble (rdpMinDistance.getText ()), Integer.parseInt(rdpMaxSegments.getText ()), simpList);
         
         int size = simpList.getNumKnots ();
         numberOfSegments = size - 1;
         
         Point3d[] curatedPoints = new Point3d[size];
         int[][] indices = new int[numberOfSegments][2];
         
         Iterator<NumericListKnot> simpItr = simpList.iterator ();
         int i = 0;
         while (simpItr.hasNext ()) {
            curatedPoints[i] = new Point3d (simpItr.next ().v);
            i++;
         }

         for (i = 0; i < numberOfSegments; i++) {
            indices[i][0] = i;
            indices[i][1] = i + 1;
         }
         PolylineMesh sphericalPolyLine =
            MeshFactory.createSphericalPolyline (50, 50, 50);

         rdpMesh = new PolylineMesh ();
         rdpMesh.addMesh (sphericalPolyLine);
         rdpMesh.set (curatedPoints, indices);
         rdpMeshBody = new FixedMeshBody ("Clipped Plate", rdpMesh);
         RenderProps.setLineStyle(rdpMeshBody, LineStyle.CYLINDER);         

         mechModel.addMeshBody (rdpMeshBody);
      }
   }

   public class FibulaPrepButtonClicked extends AbstractAction{
      public FibulaPrepButtonClicked () {
         putValue (NAME, "Clip Fibula");
      }

      @Override
      public void actionPerformed (ActionEvent evt) {
         mechModel.removeRigidBody (clippedFibulaMeshBody);
         FrameMarker[] frameMarkers = tempMeshBody.getFrameMarkers ();
         Point3d proxPoint = new Point3d(frameMarkers[0].getLocation());
         Point3d disPoint = new Point3d(frameMarkers[1].getLocation());
         
         Vector3d dir = new Vector3d(disPoint);
         dir.sub (proxPoint);
         dir.normalize ();
         Vector3d proxTranslationVector = new Vector3d(dir.copy ());
         Vector3d disTranslationVector = new Vector3d(dir.copy ());
         
         
         RigidTransform3d proxTransform = new RigidTransform3d();
         //Scale translation Vector by value inputed by user
         proxTranslationVector.scale (Float.parseFloat (fibulaDistanceProx.getText ()));
         proxTransform.setTranslation(proxTranslationVector);
         proxPoint.transform (proxTransform);
         fibulaCuttingPlanes[0] = meshHelper.createPlane(dir, proxPoint, 90, 90);
         
         RigidTransform3d disTransform = new RigidTransform3d();
         //Reverses transaltion vector
         disTranslationVector.scale (- Float.parseFloat (fibulaDistanceDis.getText ()));
         disTransform.setTranslation(disTranslationVector);
         disPoint.transform (disTransform);
         // Reverse dir
         dir.negate ();
         fibulaCuttingPlanes[1] = meshHelper.createPlane(dir, disPoint, 90, 90);
         
         SurfaceMeshIntersector intersector = new maspack.collision.SurfaceMeshIntersector ();
         clippedFibulaMesh = intersector.findDifference01 (tempMesh, fibulaCuttingPlanes[0]);
         clippedFibulaMesh = intersector.findDifference01 (clippedFibulaMesh, fibulaCuttingPlanes[1]);
         clippedFibulaMeshBody = new RigidBody ("ClippedFibula");
         clippedFibulaMeshBody.setMesh (clippedFibulaMesh);
         
         
         tempMeshBody.getRenderProps ().setVisible (false);
         mechModel.addRigidBody (clippedFibulaMeshBody);
         rerender ();
         }
   }
   
   public class TransformButtonClicked extends AbstractAction{
      public TransformButtonClicked() {
         putValue (NAME, "Transform");
      }
      
      @Override
      public void actionPerformed (ActionEvent evt) {
         //Getting Best Fit Line of Fibula       
         FrameMarker[] fibulaClipFrameMarkers = tempMeshBody.getFrameMarkers();
         FrameMarker[] fibulaLineFrameMarkers = tempMeshBody.getFrameMarkers();
         int size = fibulaLineFrameMarkers.length;
         
         // Setting up matrix for SVD
         MatrixNd svdMatrix = new MatrixNd(size, 3);
         for (int i =0; i < size; i++) {
            svdMatrix.setRow(i, fibulaLineFrameMarkers[i].getLocation().copy());
         }
         
         // Calcluating mean value of points
         VectorNd xColumn = new VectorNd();
         VectorNd yColumn = new VectorNd();
         VectorNd zColumn = new VectorNd();
         
         svdMatrix.getColumn (0, xColumn);
         svdMatrix.getColumn (1, yColumn);
         svdMatrix.getColumn (2, zColumn);
         
         Vector3d mean = new Vector3d(xColumn.mean(), yColumn.mean(), zColumn.mean());
         
         // Creating matrix of mean
         MatrixNd svdMatrixMean = new MatrixNd(size, 3);
         for (int i =0; i < size; i++) {
            svdMatrixMean.setRow(i, mean.copy ());
         }
         //Subtraction mean from matrix based on formula
         svdMatrix.sub (svdMatrixMean);   
         SVDecomposition svd = new SVDecomposition(svdMatrix);
         
         VectorNd fibulaLengthDir = new VectorNd();
         svd.getV().getColumn (0, fibulaLengthDir);
         fibulaLengthDir.normalize ();
         fibulaLengthDir.scale (50);
         
         int newSize = 3;
         Point3d[] bestFitLinePoints = new Point3d[newSize];
         int[][] indices = new int[newSize - 1][2];
         bestFitLinePoints[0] = new Point3d(mean.copy().sub(fibulaLengthDir));
         bestFitLinePoints[1] = new Point3d(mean);
         bestFitLinePoints[2] = new Point3d(mean.copy().add(fibulaLengthDir));
         
         for (int i = 0; i < newSize - 1; i++) {
            indices[i][0] = i;
            indices[i][1] = i + 1;
         }
         PolylineMesh sphericalPolyLine =
            MeshFactory.createSphericalPolyline (50, 50, 50);

         PolylineMesh bestFitLine = new PolylineMesh ();
         bestFitLine.addMesh (sphericalPolyLine);
         bestFitLine.set (bestFitLinePoints, indices);

         FixedMeshBody bestFitLineMeshBody = new FixedMeshBody ("BestFit Line", bestFitLine);
         mechModel.addMeshBody (bestFitLineMeshBody);
      
         //Calculating and applying transform
         Vector3d normProx =  fibulaCuttingPlanes[0].getNormal (0);
         normProx.normalize ();
         
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         
         // Setting up the pair of cutting planes that will be transformed for each fibula segment
         PolygonalMesh[] fibulaPiecesClippingPlanes = new PolygonalMesh[2];
         if (fibulaClipFrameMarkers[0].distance (fibulaLineFrameMarkers[0]) < fibulaClipFrameMarkers[0].distance (fibulaLineFrameMarkers[fibulaLineFrameMarkers.length - 1])) {
            fibulaPiecesClippingPlanes[0] = meshHelper.createPlane(new Vector3d(normProx), new Point3d(0,0,0), 90, 90);
            fibulaPiecesClippingPlanes[0] = meshHelper.createPlane(new Vector3d(normProx.copy ().negate ()), new Point3d(0,0,0), 90, 90);
         }
         else {
            fibulaPiecesClippingPlanes[0] = meshHelper.createPlane(new Vector3d(normProx.copy ().negate ()), new Point3d(0,0,0), 90, 90);
            fibulaPiecesClippingPlanes[0] = meshHelper.createPlane(new Vector3d(normProx), new Point3d(0,0,0), 90, 90);
         }
         
         int prevdistance = 1000;
         // Projecting first placed fibulaLine Marker onto best fit line
         VectorNd srcPoint = new VectorNd();
         VectorNd AP = fibulaLineFrameMarkers[0].getLocation().copy().sub (new VectorNd(bestFitLinePoints[0]));
         VectorNd AB = new VectorNd(bestFitLinePoints[1]).sub (new VectorNd(bestFitLinePoints[0]));
         srcPoint =  new VectorNd(bestFitLinePoints[0]).copy().add (AB.copy ().scale (AP.dot (AB) / AB.dot (AB)));
         
         AffineTransform3d[] transforms = new AffineTransform3d[numberOfSegments];
         
         
         int numberOfPoints = plateNumericList.getNumKnots ();
         
         Vector3d[] platePoints = new Vector3d[newSize];
         
         Iterator<NumericListKnot> itr = plateNumericList.iterator ();

         int l = 0;
         while (itr.hasNext ()) {
            platePoints[l] = new Point3d (itr.next ().v);
            l++;
         }
         
         Vector3d[] pointsForPlane = new Vector3d[(int)(0.7 * (numberOfPoints - 1)) - (int)0.3 * (numberOfPoints - 1)];
         for (int j = (int)(0.3 * (numberOfPoints - 1)); j < (int)(0.7 * (numberOfPoints - 1)); j++ ) {
            pointsForPlane[j - (int)(0.3 * (numberOfPoints - 1))] = new Vector3d(platePoints[j]);
         }
         
         Vector3d mandiblePlanePoint = new Vector3d();
         Vector3d mandiblePlaneNormal = new Vector3d();
         
         // Calculating plane of Mandible
         mathHelper.planeFit (pointsForPlane, mandiblePlanePoint, mandiblePlaneNormal);
         double A = mandiblePlaneNormal.get (0);
         double B = mandiblePlaneNormal.get (1);
         double C = mandiblePlaneNormal.get (2);
         
         //Store simplified points in an array of points
         Iterator<NumericListKnot> simpItr = simpList.iterator ();
         Point3d[] simpPoints = new Point3d[simpList.getNumKnots ()];
         l = 0;
         while (itr.hasNext ()) {
            simpPoints[l] = new Point3d (itr.next ().v);
            l++;
         }
         //Iterate through each segment and calculate transforms
         Point3d startPoint = new Point3d();
         Point3d endPoint = new Point3d();
         VectorNd startExtentionVector = new VectorNd();
         VectorNd endExtensionVector = new VectorNd();
         double extensionLength = 0;
         double segmentLength = 0;
         for (int i =0; i < numberOfSegments; i++ ) {
            Vector3d vectorDirection = new Vector3d();
            vectorDirection.sub (simpPoints[i], simpPoints[i+1]);
            vectorDirection.normalize ();
            double vectorPlaneAngle = Math.asin(Math.abs(A * vectorDirection.get (0) + B * vectorDirection.get (1) + C * vectorDirection.get (2)) / 
               (Math.sqrt(A * A + B * B + C * C) * Math.sqrt(Math.pow (vectorDirection.get (0), 2) + Math.pow (vectorDirection.get (i), 2) + Math.pow (vectorDirection.get (2), 2))));
            extensionLength = vectorPlaneAngle * 20 / Math.PI + 2.2;
         
            //Setting start and end points for the cutting planes
            if (i == 0) {
               startPoint = new Point3d(srcPoint.copy ());
            }
            else {
               startExtentionVector = new VectorNd(fibulaLengthDir.copy ().scale (extensionLength));
               startPoint = new Point3d(srcPoint.copy().add(startExtentionVector));
            }
            segmentLength = Math.sqrt (mathHelper.getSquareDistance (simpPoints[i], simpPoints[i+1]));
            if (i == 0) {
               endExtensionVector = new VectorNd(fibulaLengthDir.copy().scale(segmentLength));
            }
            else {
               endExtensionVector = new VectorNd(fibulaLengthDir.copy().scale(segmentLength + extensionLength));
            }
         meshHelper.setPlaneOrigin(fibulaPiecesClippingPlanes[0], startPoint);
         meshHelper.setPlaneOrigin(fibulaPiecesClippingPlanes[1], endPoint);
            
         }              
   
         
      }
   }
      
   @Override
   public void build (String[] args) {
      mechModel = new MechModel ("msmod");
      RenderProps.setPointStyle (mechModel, PointStyle.SPHERE);
      RenderProps.setFaceStyle(mechModel, FaceStyle.FRONT_AND_BACK);
      RenderProps.setBackColor (mechModel, Color.GREEN);
      addModel (mechModel);
      /*
      tempMesh =
         readMesh (
            ArtisynthPath.getHomeRelativePath (
               "src/maspack/geometry/sampleData/", ".")
            + "Mandible1.stl", "MasterMandible");*/
      tempMesh =
      meshHelper.readMesh ("C:/Users/user/Documents/fibula.stl",  "Fibula");
      
      tempMeshBody = new RigidBody ("Manidble");
      tempMeshBody.setMesh (tempMesh);
      mechModel.addRigidBody (tempMeshBody);
      addControlPanel ();
   }
}

