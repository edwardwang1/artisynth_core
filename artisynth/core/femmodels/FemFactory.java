/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.render.RenderableUtils;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

import java.util.*;

public class FemFactory {

   public enum FemElementType {
      Tet, Hex, Wedge, Pyramid, QuadTet, QuadHex, QuadWedge, QuadPyramid
   }

   // not currently used
   public enum FemShapingType {
      Linear, Quadratic
   }

//   public enum FemElemType {
//      Tet, Hex, QuadTet, QuadHex, Wedge, QuadWedge
//   }

   private static void createGridNodes(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {

      if (numX < 1 || numY < 1 || numZ < 1) {
         throw new IllegalArgumentException(
            "number of elements in each direction must be >= 1");
      }
      // create all the particles
      double dx = 1.0 / numX;
      double dy = 1.0 / numY;
      double dz = 1.0 / numZ;

      Point3d p = new Point3d();

      for (int k = 0; k <= numZ; k++) {
         for (int j = 0; j <= numY; j++) {
            for (int i = 0; i <= numX; i++) {
               p.x = widthX * (-0.5 + i * dx);
               p.y = widthY * (-0.5 + j * dy);
               p.z = widthZ * (-0.5 + k * dz);
               model.addNode(new FemNode3d(p));
            }
         }
      }
   }

   /**
    * Creates a regular grid composed of tet elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {

      if (model != null) {
         model.clear();
      } else {
         model = new FemModel3d();
      }
      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();
      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               TetElement[] elems =
                  TetElement.createCubeTesselation(
                     nodes.get((k + 1) * wk + j * wj + i),
                     nodes.get((k + 1) * wk + j * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i),
                     nodes.get(k * wk + j * wj + i),
                     nodes.get(k * wk + j * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i),
                     /* even= */(i + j + k) % 2 == 0);
               for (FemElement3d e : elems) {
                  model.addElement(e);
               }
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }

   private static final int[] apexNodeTable = new int[] { 2, 3, 1, 0, 6, 7, 5,
                                                         4 };

   /**
    * Creates a regular grid composed of pyramid elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Pyramid}.
    */
   public static FemModel3d createPyramidGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {

      model.clear();
      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();
      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               int evenCode = 0;
               if ((i % 2) == 0) {
                  evenCode |= 0x1;
               }
               if ((j % 2) == 0) {
                  evenCode |= 0x2;
               }
               if ((k % 2) == 0) {
                  evenCode |= 0x4;
               }
               PyramidElement[] elems =
                  PyramidElement.createCubeTesselation(
                     nodes.get((k + 1) * wk + j * wj + i),
                     nodes.get((k + 1) * wk + j * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i),
                     nodes.get(k * wk + j * wj + i),
                     nodes.get(k * wk + j * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i),
                     apexNodeTable[evenCode]);
               for (FemElement3d e : elems) {
                  model.addElement(e);
               }
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }

   /**
    * Creates a regular grid composed of hex elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      // clear();

      if (model != null) {
         model.clear();
      } else {
         model = new FemModel3d();
      }
      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // System.out.println("num nodes: "+myNodes.size());
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();

      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               // TetElement[] elems = TetElement.createCubeTesselation(
               HexElement e =
                  new HexElement(
                     nodes.get((k + 1) * wk + j * wj + i), nodes.get((k + 1)
                        * wk + j * wj + i + 1), nodes.get((k + 1) * wk
                        + (j + 1) * wj + i + 1), nodes.get((k + 1) * wk
                        + (j + 1) * wj + i), nodes.get(k * wk + j * wj + i),
                     nodes.get(k * wk + j * wj + i + 1), nodes.get(k * wk
                        + (j + 1) * wj + i + 1), nodes.get(k * wk + (j + 1)
                        * wj + i));

               // System.out.println ("node idxs");
               // for (int c = 0; c < e.getNodes().length; c++)
               // System.out.print (e.getNodes()[c].getNumber() + ", ");
               // System.out.println ("");

               e.setParity((i + j + k) % 2 == 0 ? 1 : 0);

               // /* even= */(i + j + k) % 2 == 0);

               model.addElement(e);
               // for (FemElement3d e : elems)
               // {
               // addElement(e);
               // }
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }


   /**
    * Creates a regular grid composed of wedge elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Wedge}.
    */
   public static FemModel3d createWedgeGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      // clear();

      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // System.out.println("num nodes: "+myNodes.size());
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();

      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               // node numbers reflect their location in a hex node
               FemNode3d n0 = nodes.get((k + 1) * wk + j * wj + i);
               FemNode3d n1 = nodes.get((k + 1) * wk + j * wj + i + 1);
               FemNode3d n2 = nodes.get((k + 1) * wk + (j + 1) * wj + i + 1);
               FemNode3d n3 = nodes.get((k + 1) * wk + (j + 1) * wj + i);
               FemNode3d n4 = nodes.get(k * wk + j * wj + i);
               FemNode3d n5 = nodes.get(k * wk + j * wj + i + 1);
               FemNode3d n6 = nodes.get(k * wk + (j + 1) * wj + i + 1);
               FemNode3d n7 = nodes.get(k * wk + (j + 1) * wj + i);

               WedgeElement e1 = new WedgeElement(n0, n1, n4, n3, n2, n7);
               WedgeElement e2 = new WedgeElement(n1, n5, n4, n2, n6, n7);

               model.addElement(e1);
               model.addElement(e2);
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }

   public static FemModel3d mergeCollapsedNodes(
      FemModel3d model, FemModel3d orig, double epsilon) {

      HashMap<FemNode3d,FemNode3d> pointMap =
         new HashMap<FemNode3d,FemNode3d>();
      HashMap<FemNode3d,ArrayList<FemNode3d>> invPointMap =
         new HashMap<FemNode3d,ArrayList<FemNode3d>>();

      // group nodes based on position
      for (FemNode3d node : orig.getNodes()) {

         boolean found = false;
         for (FemNode3d pos : invPointMap.keySet()) {
            if (node.getRestPosition().distance(pos.getRestPosition()) < epsilon) {
               found = true;
               pointMap.put(node, pos);
               invPointMap.get(pos).add(node);
               break;
            }
         }

         if (!found) {
            FemNode3d pos = new FemNode3d(new Point3d(node.getRestPosition()));
            ArrayList<FemNode3d> nodeList = new ArrayList<FemNode3d>();
            nodeList.add(node);

            pointMap.put(node, pos);
            invPointMap.put(pos, nodeList);
         }

      }

      // clear output model
      model.clear();
      for (FemNode3d node : invPointMap.keySet()) {
         model.addNode(node);
      }

      FemNode3d[] elemNodes = null;
      FemElement3d newElem = null;

      // now we should have a new set of nodes, time to build elements
      for (FemElement3d elem : orig.getElements()) {

         // unwind existing element, replace nodes with potentially
         // reduced ones then build a new element
         elemNodes = get8Nodes(elem);
         for (int i = 0; i < elemNodes.length; i++) {
            elemNodes[i] = pointMap.get(elemNodes[i]); // replace
         }

         newElem = createElem(elemNodes);
         model.addElement(newElem);

      }

      return model;
   }

   private static FemNode3d[] get8Nodes(FemElement3d elem) {

      FemNode3d[] nodeList = elem.getNodes();
      FemNode3d[] node8List = new FemNode3d[8];

      if (elem instanceof HexElement) {
         node8List[0] = nodeList[4];
         node8List[1] = nodeList[5];
         node8List[2] = nodeList[6];
         node8List[3] = nodeList[7];
         node8List[4] = nodeList[0];
         node8List[5] = nodeList[1];
         node8List[6] = nodeList[2];
         node8List[7] = nodeList[3];
      } else if (elem instanceof WedgeElement) {
         node8List[0] = nodeList[0];
         node8List[1] = nodeList[0];
         node8List[2] = nodeList[1];
         node8List[3] = nodeList[2];
         node8List[4] = nodeList[3];
         node8List[5] = nodeList[3];
         node8List[6] = nodeList[4];
         node8List[7] = nodeList[5];
      } else if (elem instanceof PyramidElement) {
         node8List[0] = nodeList[0];
         node8List[1] = nodeList[1];
         node8List[2] = nodeList[2];
         node8List[3] = nodeList[3];
         node8List[4] = nodeList[4];
         node8List[5] = nodeList[4];
         node8List[6] = nodeList[4];
         node8List[7] = nodeList[4];
      } else if (elem instanceof TetElement) {
         node8List[0] = nodeList[0];
         node8List[1] = nodeList[0];
         node8List[2] = nodeList[1];
         node8List[3] = nodeList[2];
         node8List[4] = nodeList[3];
         node8List[5] = nodeList[3];
         node8List[6] = nodeList[3];
         node8List[7] = nodeList[3];
      } else {
         throw new IllegalArgumentException("Invalid element type");
      }

      return node8List;
   }

   private static int[][] node8Faces = { { 0, 1, 2, 3 }, { 4, 5, 6, 7 },
                                        { 0, 4, 5, 1 }, { 3, 7, 6, 2 },
                                        { 0, 3, 7, 4 }, { 1, 2, 6, 5 } };

   private static FemElement3d createElem(FemNode3d[] node8List) {

      // determine element type, 8=hex,6=wedge,5=pyramid,4=tet
      ArrayList<FemNode3d> unique = new ArrayList<FemNode3d>();
      for (FemNode3d node : node8List) {
         if (!unique.contains(node)) {
            unique.add(node);
         }
      }

      int nFaceNodes = 4;
      if (unique.size() == 6 || unique.size() == 4) {
         nFaceNodes = 3;
      }

      // find first face
      ArrayList<FemNode3d> faceNodes = new ArrayList<FemNode3d>(4);
      int faceIdx = -1;
      for (int i = 0; i < 6; i++) {
         faceNodes.clear();
         for (int j = 0; j < 4; j++) {
            int idx = node8Faces[i][j];
            if (!faceNodes.contains(node8List[idx])) {
               faceNodes.add(node8List[idx]);
            }
         }

         if (faceNodes.size() == nFaceNodes) {
            faceIdx = i;
            break;
         }
      }

      if (unique.size() == 8 || unique.size() == 6) {
         // add nodes from opposite face
         for (int i = 0; i < 4; i++) {
            FemNode3d nextNode = node8List[node8Faces[faceIdx + 1][i]];
            if (!faceNodes.contains(nextNode)) {
               faceNodes.add(nextNode);
            }
         }
      } else {
         // check if mirrored
         if (faceIdx % 2 == 1) {
            // swap nodes 2/(3,4)
            FemNode3d tmp = faceNodes.get(1);
            faceNodes.set(1, faceNodes.get(nFaceNodes - 1));
            faceNodes.set(nFaceNodes - 1, tmp);
         }

         // fill in final node
         for (FemNode3d node : unique) {
            if (!faceNodes.contains(node)) {
               faceNodes.add(node);
               break;
            }
         }
      }

      // we should now have complete oriented set of nodes
      // create the actual element

      switch (faceNodes.size()) {
         case 8:
            return new HexElement(
               faceNodes.get(4), faceNodes.get(5), faceNodes.get(6),
               faceNodes.get(7), faceNodes.get(0), faceNodes.get(1),
               faceNodes.get(2), faceNodes.get(3));
         case 6:
            return new WedgeElement(
               faceNodes.get(0), faceNodes.get(1), faceNodes.get(2),
               faceNodes.get(3), faceNodes.get(4), faceNodes.get(5));
         case 5:
            return new PyramidElement(
               faceNodes.get(0), faceNodes.get(1), faceNodes.get(2),
               faceNodes.get(3), faceNodes.get(4));
         case 4:
            return new TetElement(
               faceNodes.get(0), faceNodes.get(1), faceNodes.get(2),
               faceNodes.get(3));
         default:
      }

      throw new IllegalArgumentException(
         "Invalid number or ordering of unique nodes");

   }


   /**
    * Creates a tet-based spherical model based on a icosahedron. The method
    * works by creating an icosahedral surface mesh and then using tetgen.
    * 
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param r radius
    * @param ndivisions number of divisions used in creating the surface mesh.
    * Typical values are 1 or 2.
    * @param quality quality parameter passed to tetgen. See
    * {@link #createFromMesh} for a full description.
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createIcosahedralSphere (
      FemModel3d model, double r, int ndivisions, double quality) {

      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (
         r, Point3d.ZERO, ndivisions);
      return createFromMesh (model, mesh, quality);
   }

   /**
    * Convenience method to create a symmetric hex/wedge dominant sphere
    * using {@link #createEllipsoid}.
    * 
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param r radius
    * @param nt number of nodes in each ring parallel to the equator
    * @param nl number of nodes in each quarter ring perpendicular to the
    * equator (including end nodes)
    * @param ns number of nodes in each radial line extending out from
    * the polar axis (including end nodes)
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createSphere (
       FemModel3d model, double r, int nt, int nl, int ns) {
      return createEllipsoid (model, r, r, r, nt, nl, ns);
   }

   /**
    * Creates an ellipsoidal model using a combination of hex, wedge, and tet
    * elements. The model is created symmetrically about a central polar axis,
    * using wedge elements at the core.  <code>rl</code> should be the longest
    * radius, and corresponds to the polar axis.
    *
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param rl longest radius (also the polar radius)
    * @param rs1 first radius perpendicular to the polar axis
    * @param rs2 second radius perpendicular to the polar axis
    * @param nt number of nodes in each ring parallel to the equator
    * @param nl number of nodes in each quarter ring perpendicular to the
    * equator (including end nodes)
    * @param ns number of nodes in each radial line extending out from
    * the polar axis (including end nodes)
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createEllipsoid(
      FemModel3d model, 
      double rl, double rs1, double rs2, int nt, int nl, int ns) {

      double dl = Math.PI / (2 * nl - 2);
      double dt = 2 * Math.PI / nt;
      double dr = 1.0 / (ns - 1);

      FemNode3d nodes[][][] = new FemNode3d[nt][2 * nl - 1][ns];
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }

      // generate nodes
      for (int k = 0; k < ns; k++) {
         for (int j = 0; j < 2 * nl - 1; j++) {

            if (k == 0) {
               FemNode3d node =
                  new FemNode3d(new Point3d(0, 0, -rl + 2 * rl * dl * j
                     / Math.PI));

               // System.out.println(node.getPosition());

               for (int i = 0; i < nt; i++) {
                  nodes[i][j][k] = node;
               }
               model.addNode(node);
            } else {
               if (j == 0) {
                  for (int i = 0; i < nt; i++) {
                     nodes[i][j][k] = nodes[i][j][0];
                  }
               } else if (j == 2 * nl - 2) {
                  for (int i = 0; i < nt; i++) {
                     nodes[i][j][k] = nodes[i][j][0];
                  }
               } else {
                  for (int i = 0; i < nt; i++) {
                     double kInterp =
                        Math.pow(((double)k) / (ns - 1), 2) * (rs1 + rs2)
                           / (2 * rl);
                     double l =
                        (-rl + 2 * rl * dl * j / Math.PI) * (1 - kInterp)
                           + (-rl * Math.cos(j * dl)) * (kInterp);
                     double rAdj = dr * k * Math.sqrt(1 - l * l / rl / rl);
                     nodes[i][j][k] =
                        new FemNode3d(
                           new Point3d(-rs1 * rAdj * Math.sin(dt * i), rs2
                              * rAdj * Math.cos(dt * i), l));
                     model.addNode(nodes[i][j][k]);
                     // System.out.println(nodes[i][j][k].getPosition());
                  }
               }
            }
         }
      }

      FemNode3d[] node8List = new FemNode3d[8]; // storing 8 nodes, repeated or
                                                // not

      // generate elements
      for (int k = 0; k < ns - 1; k++) {
         for (int j = 0; j < 2 * nl - 2; j++) {
            for (int i = 0; i < nt; i++) {

               node8List[0] = nodes[i][j][k];
               node8List[1] = nodes[(i + 1) % nt][j][k];
               node8List[2] = nodes[(i + 1) % nt][j + 1][k];
               node8List[3] = nodes[i][j + 1][k];
               node8List[4] = nodes[i][j][k + 1];
               node8List[5] = nodes[(i + 1) % nt][j][k + 1];
               node8List[6] = nodes[(i + 1) % nt][j + 1][k + 1];
               node8List[7] = nodes[i][j + 1][k + 1];

               FemElement3d elem = createElem(node8List);
               model.addElement(elem);
            }
         }
      }

      return model;
   }
   
   /**
    * Creates a cylinder made of mostly hex elements, with wedges in the centre
    * column.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param l length along the z axis
    * @param r radius in the x-y plane
    * @param nt element resolution around the center axis
    * @param nl element resolution along the length
    * @param nr element resolution along the radius
    * @return created FEM model
    */
   public static FemModel3d createCylinder(
      FemModel3d model, double l, double r, int nt, int nl, int nr) {

      return createHexWedgeCylinder(model, l, r, nt, nl, nr);
   }
   
   /**
    * Creates a cylinder made of mostly hex elements, with wedges in the centre
    * column. Identical to {@link 
    * #createCylinder(FemModel3d,double,double,int,int,int)}.
    */  
   public static FemModel3d createHexWedgeCylinder(
      FemModel3d model, double l, double r, int nt, int nl, int nr) {

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }

      FemNode3d nodes[][][] = new FemNode3d[nt][nl][nr];

      double dl = l / (nl - 1);
      double dt = 2 * Math.PI / nt;
      double dr = 1.0 / (nr - 1);

      // generate nodes
      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {

            if (k == 0) {
               FemNode3d node =
                  new FemNode3d(new Point3d(0, 0, -l / 2 + j * dl));
               for (int i = 0; i < nt; i++) {
                  nodes[i][j][k] = node;
               }
               model.addNode(node);
            } else {
               for (int i = 0; i < nt; i++) {
                  double rr = r * Math.pow(dr * k, 0.7);
                  nodes[i][j][k] =
                     new FemNode3d(new Point3d(-rr * Math.sin(dt * i), rr
                        * Math.cos(dt * i), -l / 2 + j * dl));
                  model.addNode(nodes[i][j][k]);
               }
            }
         }
      }

      // generate elements
      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl - 1; j++) {
            for (int i = 0; i < nt; i++) {

               if (k == 0) {
                  // wedge element
                  WedgeElement wedge =
                     new WedgeElement(
                        nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                        nodes[i][j][k],

                        nodes[i][j + 1][k + 1],
                        nodes[(i + 1) % nt][j + 1][k + 1], nodes[i][j + 1][k]);
                  model.addElement(wedge);
               } else {
                  // hex element
                  HexElement hex =
                     new HexElement(
                        nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                        nodes[(i + 1) % nt][j + 1][k + 1],
                        nodes[i][j + 1][k + 1],

                        nodes[i][j][k], nodes[(i + 1) % nt][j][k],
                        nodes[(i + 1) % nt][j + 1][k], nodes[i][j + 1][k]);
                  model.addElement(hex);
               }

            }
         }
      }

      return model;
   }
   
   /**
    * Creates a partial cylinder made of mostly hex elements, with wedges in
    * the centre column
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param l length along the z axis
    * @param r radius in the x-y plane
    * @param theta size of the slice, in radians
    * @param nl element resolution along the length
    * @param nr element resolution along the radius
    * @param ntheta element resolution around the slice
    * @return created FEM model
    */
   public static FemModel3d createPartialCylinder(
      FemModel3d model, double l, double r, double theta,
      int nl, int nr, int ntheta) {

      return createPartialHexWedgeCylinder(model, l, r, theta, nl, nr, ntheta);

   }
   
   public static FemModel3d createPartialHexWedgeCylinder(
      FemModel3d model, double l, double r, double theta,
      int nl, int nr, int ntheta) {
 
      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }

     FemNode3d nodes[][][] = new FemNode3d[ntheta][nl][nr];

      double dl = l / (nl - 1);
      double dt = theta / (ntheta-1);
      double dr = 1.0 / (nr - 1);

      // generate nodes
      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {

            if (k == 0) {
               FemNode3d node =
                  new FemNode3d(new Point3d(0, 0, -l / 2 + j * dl));
               for (int i = 0; i < ntheta; i++) {
                  nodes[i][j][k] = node;
               }
               model.addNode(node);
            } else {
               for (int i = 0; i < ntheta; i++) {
                  double rr = r * Math.pow(dr * k, 0.7);
                  nodes[i][j][k] =
                     new FemNode3d(new Point3d(-rr * Math.sin(dt * i), rr
                        * Math.cos(dt * i), -l / 2 + j * dl));
                  model.addNode(nodes[i][j][k]);
               }
            }
         }
      }

      // generate elements
      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl - 1; j++) {
            for (int i = 0; i < ntheta -1; i++) {

               if (k == 0) {
                  // wedge element
                  WedgeElement wedge =
                     new WedgeElement(
                        nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                        nodes[i][j][k],

                        nodes[i][j + 1][k + 1],
                        nodes[i + 1][j + 1][k + 1], nodes[i][j + 1][k]);
                  model.addElement(wedge);
               } else {
                  // hex element
                  HexElement hex =
                     new HexElement(
                        nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                        nodes[i + 1][j + 1][k + 1],
                        nodes[i][j + 1][k + 1],

                        nodes[i][j][k], nodes[i + 1][j][k],
                        nodes[i + 1][j + 1][k], nodes[i][j + 1][k]);
                  model.addElement(hex);
               }

            }
         }
      }

      return model;
   }

   /**
    * Creates a tube made of hex elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexTube(
      FemModel3d model, double l, double rin, double rout, int nt, int nl, int nr) {
      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];

      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;
      // double dr = 0.5*r;

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl+1; j++) {
            for (int i = 0; i < nt; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(
                  // -l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // Changed to align tube with z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      HexElement elems[][][] = new HexElement[nt][nl][nr];
      LinkedList<HexElement> elemList = new LinkedList<HexElement>();

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  new HexElement(
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][j + 1][k + 1], nodes[i][j + 1][k + 1],

                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][j + 1][k], nodes[i][j + 1][k]

                  );

               // elems[i][j][k].setParity ((i+j)%2==0 ? 1 : 0);

               elemList.add(elems[i][j][k]);
               model.addElement(elems[i][j][k]);
            }
         }
      }
      HexElement.setParities(elemList);

      for (int i = 0; i < nt; i++) {
         for (int j = 0; j < nr; j++) {
            // nodes[i][0][j].setDynamic(false);
         }
      }
      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }

   /**
    * Creates a tube made of tet elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetTube(
      FemModel3d model, 
      double l, double rin, double rout, int nt, int nl, int nr) {

      // round nt up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      // HexModel model = new HexModel();

      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];

      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;
      // double dr = 0.5*r;

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl+1; j++) {
            for (int i = 0; i < nt; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(-l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // changed to make tube align with the z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      // for(FemNode3d n : tetMod.getNodes())
      // {
      // R.mul(pos, n.getPosition());
      // n.setPosition(new Point3d(pos));
      // }

      TetElement elems[][][][] = new TetElement[nt][nl][nr][5];

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  TetElement.createCubeTesselation(
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][j + 1][k + 1], nodes[i][j + 1][k + 1],
                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][j + 1][k], nodes[i][j + 1][k], (i + j + k) % 2 == 0);

               model.addElement(elems[i][j][k][0]);
               model.addElement(elems[i][j][k][1]);
               model.addElement(elems[i][j][k][2]);
               model.addElement(elems[i][j][k][3]);
               model.addElement(elems[i][j][k][4]);
            }
         }
      }

      // model.getSurfaceMesh().setEdgeHard(model.getNode(3), model.getNode(34),
      // true);
      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }
   
   /**
    * Creates a partial tube made of hex elements. Identical to
    * {@link
    * #createPartialTube(FemModel3d,FemElementType,double,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createPartialHexTube(
      FemModel3d model, double l, double rin, double rout, double theta, 
      int nl, int nr, int ntheta) {
      FemNode3d nodes[][][] = new FemNode3d[ntheta][nl][nr];

      double dl = l / (nl - 1);
      double dt = theta / (ntheta-1);
      double dr = (rout - rin) / (nr - 1);
      // double dr = 0.5*r;

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < ntheta; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(
                  // -l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // Changed to align tube with z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      HexElement elems[][][] = new HexElement[ntheta][nl - 1][nr - 1];
      LinkedList<HexElement> elemList = new LinkedList<HexElement>();

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl - 1; j++) {
            for (int i = 0; i < ntheta-1; i++) {
               elems[i][j][k] =
                  new HexElement(
                     nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                     nodes[i + 1][j + 1][k + 1], nodes[i][j + 1][k + 1],

                     nodes[i][j][k], nodes[i + 1][j][k], nodes[i + 1][j + 1][k],
                     nodes[i][j + 1][k]
                  );

               // elems[i][j][k].setParity ((i+j)%2==0 ? 1 : 0);

               elemList.add(elems[i][j][k]);
               model.addElement(elems[i][j][k]);
            }
         }
      }
      HexElement.setParities(elemList);

      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }

   /**
    * Creates a partial tube made of tet elements. Identical to
    * {@link
    * #createPartialTube(FemModel3d,FemElementType,double,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createPartialTetTube(
      FemModel3d model, double l, double rin, double rout, double theta,
      int nl, int nr, int ntheta) {
      // HexModel model = new HexModel();

      FemNode3d nodes[][][] = new FemNode3d[ntheta][nl][nr];

      double dl = l / (nl - 1);
      double dt = theta / (ntheta-1);
      double dr = (rout - rin) / (nr - 1);
      // double dr = 0.5*r;

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < ntheta; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(-l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // changed to make tube align with the z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      // for(FemNode3d n : tetMod.getNodes())
      // {
      // R.mul(pos, n.getPosition());
      // n.setPosition(new Point3d(pos));
      // }

      TetElement elems[][][][] = new TetElement[ntheta][nl - 1][nr - 1][5];

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl - 1; j++) {
            for (int i = 0; i < ntheta -1; i++) {
               elems[i][j][k] =
                  TetElement.createCubeTesselation(
                     nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                     nodes[i + 1][j + 1][k + 1], nodes[i][j + 1][k + 1],
                     nodes[i][j][k], nodes[i + 1][j][k], nodes[i + 1][j + 1][k],
                     nodes[i][j + 1][k], (i + j + k) % 2 == 0);

               model.addElement(elems[i][j][k][0]);
               model.addElement(elems[i][j][k][1]);
               model.addElement(elems[i][j][k][2]);
               model.addElement(elems[i][j][k][3]);
               model.addElement(elems[i][j][k][4]);
            }
         }
      }

      // model.getSurfaceMesh().setEdgeHard(model.getNode(3), model.getNode(34),
      // true);
      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }

   /**
    * Creates a hollow torus made of hex elements. Identical to {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexTorus(
      FemModel3d model,
      double R, double rin, double rout, int nt, int nl, int nr) {

      FemNode3d nodes[][][] = new FemNode3d[nt][nl][nr];

      double dT = 2 * Math.PI / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / (nr - 1);

      RotationMatrix3d RM = new RotationMatrix3d(1.0, 0, 0, Math.PI / 2.0);
      Vector3d pos = new Vector3d();

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               pos.set(
                  R * Math.cos(dT * j) + (rin + dr * k) * Math.cos(dt * i)
                     * Math.cos(dT * j), R * Math.sin(dT * j) + (rin + dr * k)
                     * Math.cos(dt * i) * Math.sin(dT * j), (rin + dr * k)
                     * Math.sin(dt * i));
               RM.mul(pos);

               nodes[i][j][k] = new FemNode3d(new Point3d(pos));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      HexElement elems[][][] = new HexElement[nt][nl][nr - 1];

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  new HexElement(
                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][(j + 1) % nl][k], nodes[i][(j + 1) % nl][k],
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][(j + 1) % nl][k + 1], nodes[i][(j + 1)
                        % nl][k + 1]

                  );

               model.addElement(elems[i][j][k]);
            }
         }
      }

      return model;
   }

   /**
    * Creates a hollow torus made of tet elements. Identical to {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetTorus(
      FemModel3d model,
      double R, double rin, double rout, int nt, int nl, int nr) {

      FemNode3d nodes[][][] = new FemNode3d[nt][nl][nr];
      
      // round nt and nl up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      if ((nl % 2) == 1) {
         nl++;
      }

      double dT = 2 * Math.PI / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / (nr - 1);

      RotationMatrix3d RM = new RotationMatrix3d(1.0, 0, 0, Math.PI / 2.0);
      Vector3d pos = new Vector3d();

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               pos.set(
                  R * Math.cos(dT * j) + (rin + dr * k) * Math.cos(dt * i)
                     * Math.cos(dT * j), R * Math.sin(dT * j) + (rin + dr * k)
                     * Math.cos(dt * i) * Math.sin(dT * j), (rin + dr * k)
                     * Math.sin(dt * i));
               RM.mul(pos);

               nodes[i][j][k] = new FemNode3d(new Point3d(pos));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      TetElement elems[][][][] = new TetElement[nt][nl][nr - 1][5];

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  TetElement.createCubeTesselation(
                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][(j + 1) % nl][k], nodes[i][(j + 1) % nl][k],
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][(j + 1) % nl][k + 1], nodes[i][(j + 1)
                        % nl][k + 1], (i + j + k) % 2 == 0);

               model.addElement(elems[i][j][k][0]);
               model.addElement(elems[i][j][k][1]);
               model.addElement(elems[i][j][k][2]);
               model.addElement(elems[i][j][k][3]);
               model.addElement(elems[i][j][k][4]);
            }
         }
      }
      return model;
   }

   private static int X_POS = 0x001;
   private static int X_NEG = 0x002;
   private static int Y_POS = 0x004;
   private static int Y_NEG = 0x008;
   private static int Z_POS = 0x010;
   private static int Z_NEG = 0x020;
   private static int R_INNER = 0x40;
   private static int R_OUTER = 0x80;

   private static int gridBoundarySurfaces (
      Point3d pnt, double widthX, double widthY, double widthZ) {
      double tol =
         (1e-14) * (Math.abs(widthX) + Math.abs(widthY) + Math.abs(widthZ));

      int boundarySurfaces = 0;
      
      if (pnt.x >= widthX / 2 - tol) {
         boundarySurfaces |= X_POS;
      }
      else if (pnt.x <= -widthX / 2 + tol) {
         boundarySurfaces |= X_NEG;
      }
      if (pnt.y >= widthY / 2 - tol) {
         boundarySurfaces |= Y_POS;
      }
      else if (pnt.y <= -widthY / 2 + tol) {
         boundarySurfaces |= Y_NEG;
      }
      if (pnt.z >= widthZ / 2 - tol) {
         boundarySurfaces |= Z_POS;
      }
      else if (pnt.z <= -widthZ / 2 + tol) {
         boundarySurfaces |= Z_NEG;
      }
      return boundarySurfaces;
   }

   private static int bitCount (int val) {
      int cnt = 0;
      while (val != 0) {
         if ((val & 0x1) != 0) {
            cnt++;
         }
         val = (val >>> 1);
      }
      return cnt;
   }

   private static int tubeBoundarySurfaces (
      Point3d pnt, double l, double rin, double rout) {
      double tol = (1e-14) * (Math.abs(l) + Math.abs(rout));
      double radius = Math.sqrt(pnt.x * pnt.x + pnt.y * pnt.y);

      int boundarySurfaces = 0;

      if (radius >= rout - tol) {
         boundarySurfaces |= R_OUTER;
      }
      else if (radius <= rin + tol) {
         boundarySurfaces |= R_INNER;
      }
      if (pnt.z >= l / 2 - tol) {
         boundarySurfaces |= Z_POS;
      }
      else if (pnt.z <= -l / 2 + tol) {
         boundarySurfaces |= Z_NEG;
      }
      return boundarySurfaces;
   }

   private static void setTubeEdgesHard(
      FemModel3d model, double l, double rin, double rout) {
      // and now set the surface edges hard ...
      PolygonalMesh mesh = model.getSurfaceMesh();
      // iterate through all edges in the surface mesh.
      for (Face face : mesh.getFaces()) {
         Vertex3d vtx = (Vertex3d)face.getVertex(0);
         for (int i = 1; i < face.numVertices(); i++) {
            Vertex3d nextVtx = (Vertex3d)face.getVertex(i);
            // a hard edge occurs when both vertices share two
            // or more boundary surfaces.
            int mutualSurfaces = 
               (tubeBoundarySurfaces (vtx.pnt, l, rin, rout) &
                tubeBoundarySurfaces (nextVtx.pnt, l, rin, rout));
            if (bitCount (mutualSurfaces) > 1) {
               mesh.setHardEdge(vtx, nextVtx, true);
            }
            vtx = nextVtx;
         }
      }
   }

   protected static FemModel3d createQuadraticTube(
      FemModel3d model,
      double l, double rin, double rout, int nt, int nl, int nr, 
      boolean useHexes) {
      
      // round nt up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      // double nt, nl, nr to get the equivalent element res
      // for a linear element tesselation. This is what we will work with
      nt *= 2;
      nl *= 2;
      nr *= 2;      

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }
      
      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];

      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;
      // double dr = 0.5*r;

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl+1; j++) {
            for (int i = 0; i < nt; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(
                  // -l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // Changed to align tube with z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));

            }
         }
      }

      for (int k = 0; k < nr - 1; k += 2) {
         for (int j = 0; j < nl - 1; j += 2) {
            for (int i = 0; i < nt; i += 2) {
               if (useHexes) {
                  FemNode3d enodes[] = new FemNode3d[] 
                    {
                    nodes[i][j][k+2],
                    nodes[(i+2)%nt][j][k+2],
                    nodes[(i+2)%nt][j+2][k+2],
                    nodes[i][j+2][k+2],
                    nodes[i][j][k],
                    nodes[(i+2)%nt][j][k],
                    nodes[(i+2)%nt][j+2][k],
                    nodes[i][j+2][k],

                    nodes[i+1][j][k+2],
                    nodes[(i+2)%nt][j+1][k+2],
                    nodes[i+1][j+2][k+2],
                    nodes[i][j+1][k+2],

                    nodes[i+1][j][k],
                    nodes[(i+2)%nt][j+1][k],
                    nodes[i+1][j+2][k],
                    nodes[i][j+1][k],

                    nodes[i][j][k+1],
                    nodes[(i+2)%nt][j][k+1],
                    nodes[(i+2)%nt][j+2][k+1],
                    nodes[i][j+2][k+1], 
                  };
                  QuadhexElement e = new QuadhexElement (enodes);
                  for (FemNode3d n : enodes) {
                     if (!model.getNodes().contains(n)) {
                        model.addNode(n);
                     }
                  }
                  model.addElement (e);
               }
               else {
                  QuadtetElement[] elems = 
                     createCubeTesselation(
                        new FemNode3d[][][]
                         {
                            {
                               { nodes[i][j][k],
                                 nodes[i][j][k + 1],
                                 nodes[i][j][k + 2] },
                               { nodes[i][j + 1][k],
                                 nodes[i][j + 1][k + 1],
                                 nodes[i][j + 1][k + 2] },
                               { nodes[i][j + 2][k],
                                 nodes[i][j + 2][k + 1],
                                 nodes[i][j + 2][k + 2] } },
                            
                            {
                               { nodes[i + 1][j][k],
                                 nodes[i + 1][j][k + 1],
                                 nodes[i + 1][j][k + 2] },
                               { nodes[i + 1][j + 1][k],
                                 nodes[i + 1][j + 1][k + 1],
                                 nodes[i + 1][j + 1][k + 2] },
                               { nodes[i + 1][j + 2][k],
                                 nodes[i + 1][j + 2][k + 1],
                                 nodes[i + 1][j + 2][k + 2] } },
                            
                            {
                               { nodes[(i + 2) % nt][j][k],
                                 nodes[(i + 2) % nt][j][k + 1],
                                 nodes[(i + 2) % nt][j][k + 2] },
                               { nodes[(i + 2) % nt][j + 1][k],
                                 nodes[(i + 2) % nt][j + 1][k + 1],
                                 nodes[(i + 2) % nt][j + 1][k + 2] },
                               { nodes[(i + 2) % nt][j + 2][k],
                                 nodes[(i + 2) % nt][j + 2][k + 1],
                                 nodes[(i + 2) % nt][j + 2][k + 2] } }
                         },
                        ((i + j + k) / 2 % 2 == 0));
                  for (QuadtetElement e : elems) {
                     FemNode3d[] enodes = e.getNodes();
                     for (FemNode3d n : enodes) {
                        if (!model.getNodes().contains(n)) {
                           model.addNode(n);
                        }
                     }
                     model.addElement (e);
                  }
               }
            }
         }
      }

      setTubeEdgesHard(model, l, rin, rout);

      return model;
   }

   protected static FemModel3d createQuadraticTorus (
      FemModel3d model, double R, double rin, double rout,
      int nt, int nl, int nr, boolean useHexes) {

      // round nt and nl up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      if ((nl % 2) == 1) {
         nl++;
      }
      // double nt, nl, nr to get the equivalent element res
      // for a linear element tesselation. This is what we will work with
      nt *= 2;
      nl *= 2;
      nr *= 2;      

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }

      FemNode3d nodes[][][] = new FemNode3d[nt][nl][nr+1];

      double dT = 2 * Math.PI / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;

      RotationMatrix3d RM = new RotationMatrix3d(1.0, 0, 0, Math.PI / 2.0);
      Vector3d pos = new Vector3d();

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               pos.set(
                  R * Math.cos(dT * j) + (rin + dr * k) * Math.cos(dt * i)
                     * Math.cos(dT * j), R * Math.sin(dT * j) + (rin + dr * k)
                     * Math.cos(dt * i) * Math.sin(dT * j), (rin + dr * k)
                     * Math.sin(dt * i));
               RM.mul(pos, pos);

               nodes[i][j][k] = new FemNode3d(new Point3d(pos));
            }
         }
      }

      for (int k = 0; k < nr - 1; k += 2) {
         for (int j = 0; j < nl; j += 2) {
            for (int i = 0; i < nt; i += 2) {
               if (useHexes) {
                  FemNode3d enodes[] = new FemNode3d[] 
                     {
                        nodes[i][j][k],
                        nodes[(i+2)%nt][j][k],
                        nodes[(i+2)%nt][(j+2)%nl][k],
                        nodes[i][(j+2)%nl][k],
                        nodes[i][j][k+2],
                        nodes[(i+2)%nt][j][k+2],
                        nodes[(i+2)%nt][(j+2)%nl][k+2],
                        nodes[i][(j+2)%nl][k+2],

                        nodes[i+1][j][k],
                        nodes[(i+2)%nt][j+1][k],
                        nodes[i+1][(j+2)%nl][k],
                        nodes[i][j+1][k],

                        nodes[i+1][j][k+2],
                        nodes[(i+2)%nt][j+1][k+2],
                        nodes[i+1][(j+2)%nl][k+2],
                        nodes[i][j+1][k+2],

                        nodes[i][j][k+1],
                        nodes[(i+2)%nt][j][k+1],
                        nodes[(i+2)%nt][(j+2)%nl][k+1],
                        nodes[i][(j+2)%nl][k+1],
                     };
                  QuadhexElement e = new QuadhexElement (enodes);
                  for (FemNode3d n : enodes) {
                     if (!model.getNodes().contains(n)) {
                        model.addNode(n);
                     }
                  }
                  model.addElement (e);
               }
               else {
                  QuadtetElement[] elems = 
                     createCubeTesselation(
                     new FemNode3d[][][]
                      {
                         {
                            { nodes[i][j][k + 2],
                              nodes[i][j][k + 1],
                              nodes[i][j][k] },
                            { nodes[i][j + 1][k + 2],
                              nodes[i][j + 1][k + 1],
                              nodes[i][j + 1][k] },
                            { nodes[i][(j + 2) % nl][k + 2],
                              nodes[i][(j + 2) % nl][k + 1],
                              nodes[i][(j + 2) % nl][k] } },
                         
                         {
                            { nodes[i + 1][j][k + 2],
                              nodes[i + 1][j][k + 1],
                              nodes[i + 1][j][k] },
                            { nodes[i + 1][j + 1][k + 2],
                              nodes[i + 1][j + 1][k + 1],
                              nodes[i + 1][j + 1][k] },
                            { nodes[i + 1][(j + 2) % nl][k + 2],
                              nodes[i + 1][(j + 2) % nl][k + 1],
                              nodes[i + 1][(j + 2) % nl][k] } },
                         
                         {
                            { nodes[(i + 2) % nt][j][k + 2],
                              nodes[(i + 2) % nt][j][k + 1],
                              nodes[(i + 2) % nt][j][k] },
                            { nodes[(i + 2) % nt][j + 1][k + 2],
                              nodes[(i + 2) % nt][j + 1][k + 1],
                              nodes[(i + 2) % nt][j + 1][k] },
                            { nodes[(i + 2) % nt][(j + 2) % nl][k + 2],
                              nodes[(i + 2) % nt][(j + 2) % nl][k + 1],
                              nodes[(i + 2) % nt][(j + 2) % nl][k] } }
                      },
                     ((i + j + k) / 2 % 2 == 0));
                  for (QuadtetElement e : elems) {
                     FemNode3d[] enodes = e.getNodes();
                     for (FemNode3d n : enodes) {
                        if (!model.getNodes().contains(n)) {
                           model.addNode(n);
                        }
                     }
                     model.addElement (e);
                  }        
               }
            }
         }
      }

      return model;
   }

   private static QuadtetElement[] createCubeTesselation(
      FemNode3d[][][] nodes27, boolean even) {
      QuadtetElement qelems[] = new QuadtetElement[5];

      if (even) {
         qelems[0] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][0][0], nodes27[2][2][0],
               nodes27[2][0][2], nodes27[1][0][0], nodes27[2][1][0],
               nodes27[1][1][0], nodes27[1][0][1], nodes27[2][0][1],
               nodes27[2][1][1]);

         qelems[1] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][0][2], nodes27[0][2][2],
               nodes27[0][0][2], nodes27[1][0][1], nodes27[1][1][2],
               nodes27[0][1][1], nodes27[0][0][1], nodes27[1][0][2],
               nodes27[0][1][2]);

         qelems[2] =
            new QuadtetElement(
               nodes27[0][2][2], nodes27[2][0][2], nodes27[2][2][0],
               nodes27[2][2][2], nodes27[1][1][2], nodes27[2][1][1],
               nodes27[1][2][1], nodes27[1][2][2], nodes27[2][1][2],
               nodes27[2][2][1]);

         qelems[3] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][2][0], nodes27[0][2][0],
               nodes27[0][2][2], nodes27[1][1][0], nodes27[1][2][0],
               nodes27[0][1][0], nodes27[0][1][1], nodes27[1][2][1],
               nodes27[0][2][1]);

         qelems[4] =
            new QuadtetElement(
               nodes27[0][2][2], nodes27[0][0][0], nodes27[2][2][0],
               nodes27[2][0][2], nodes27[0][1][1], nodes27[1][1][0],
               nodes27[1][2][1], nodes27[1][1][2], nodes27[1][0][1],
               nodes27[2][1][1]);
      } else {
         qelems[0] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][0][0], nodes27[0][2][0],
               nodes27[0][0][2], nodes27[1][0][0], nodes27[1][1][0],
               nodes27[0][1][0], nodes27[0][0][1], nodes27[1][0][1],
               nodes27[0][1][1]);

         qelems[1] =
            new QuadtetElement(
               nodes27[0][0][2], nodes27[2][0][0], nodes27[2][2][2],
               nodes27[2][0][2], nodes27[1][0][1], nodes27[2][1][1],
               nodes27[1][1][2], nodes27[1][0][2], nodes27[2][0][1],
               nodes27[2][1][2]);

         qelems[2] =
            new QuadtetElement(
               nodes27[0][2][2], nodes27[0][0][2], nodes27[0][2][0],
               nodes27[2][2][2], nodes27[0][1][2], nodes27[0][1][1],
               nodes27[0][2][1], nodes27[1][2][2], nodes27[1][1][2],
               nodes27[1][2][1]);

         qelems[3] =
            new QuadtetElement(
               nodes27[0][2][0], nodes27[2][0][0], nodes27[2][2][0],
               nodes27[2][2][2], nodes27[1][1][0], nodes27[2][1][0],
               nodes27[1][2][0], nodes27[1][2][1], nodes27[2][1][1],
               nodes27[2][2][1]);

         qelems[4] =
            new QuadtetElement(
               nodes27[0][2][0], nodes27[2][0][0], nodes27[2][2][2],
               nodes27[0][0][2], nodes27[1][1][0], nodes27[2][1][1],
               nodes27[1][2][1], nodes27[0][1][1], nodes27[1][0][1],
               nodes27[1][1][2]);
      }

      return qelems;
   }

   /**
    * Takes a FemModel3d containing linear elements, and creates a quadratic
    * model whose elements are the corresponding quadratic elements, with new
    * nodes inserted along the edges as required. The new quadratic model will
    * have straight edges in the rest position.
    * 
    * @param linMod
    * A FemModel3d previously inialized with only linear elements.
    */
   public static FemModel3d createQuadraticModel(
      FemModel3d quadMod, FemModel3d linMod) {
      ComponentListView<FemNode3d> quadNodes = quadMod.getNodes();

      if (quadMod == linMod) {
         throw new IllegalArgumentException(
            "quadMod and linMod must be different");
      }

      HashMap<FemNode3d,FemNode3d> nodeMap = new HashMap<FemNode3d,FemNode3d>();

      for (FemNode3d n : linMod.getNodes()) {
         FemNode3d newn = new FemNode3d(n.getPosition());
         nodeMap.put(n, newn);
         quadMod.addNode(newn);
      }

      for (FemElement3d e : linMod.getElements()) {
         ArrayList<FemNode3d> allNodes = new ArrayList<FemNode3d>();
         FemNode3d qnodes[];

         for (FemNode3d n : e.getNodes()) {
            allNodes.add(nodeMap.get(n));
         }

         if (e instanceof TetElement) {
            qnodes = QuadtetElement.getQuadraticNodes((TetElement)e);
         } else if (e instanceof HexElement) {
            qnodes = QuadhexElement.getQuadraticNodes((HexElement)e);
         } else if (e instanceof WedgeElement) {
            qnodes = QuadwedgeElement.getQuadraticNodes((WedgeElement)e);
         } else if (e instanceof PyramidElement) {
            qnodes = QuadpyramidElement.getQuadraticNodes((PyramidElement)e);
         } else {
            throw new UnsupportedOperationException(
               "Only linear elements supported");
         }
         for (int i = 0; i < qnodes.length; i++) {
            boolean nodeExists = false;
            for (FemNode3d n : quadNodes) {
               if (qnodes[i].getPosition().equals(n.getPosition())) {
                  qnodes[i] = n;
                  nodeExists = true;
                  break;
               }
            }
            if (!nodeExists) {
               quadMod.addNode(qnodes[i]);
            }
         }
         for (FemNode3d n : qnodes) {
            allNodes.add(n);
         }
         FemNode3d[] nodes = allNodes.toArray(new FemNode3d[0]);
         FemElement3d qe = null;
         if (e instanceof TetElement) {
            qe = new QuadtetElement(nodes);
         } else if (e instanceof HexElement) {
            qe = new QuadhexElement(nodes);
         } else if (e instanceof WedgeElement) {
            qe = new QuadwedgeElement(nodes);
         } else if (e instanceof PyramidElement) {
            qe = new QuadpyramidElement(nodes);
         }
         quadMod.addElement(qe);
      }

      quadMod.setMaterial(linMod.getMaterial());
      /*
       * redistributes mass to quadratic model. ONLY works for uniform density
       */
      // double linModPerElementMass = 0;
      // for (FemElement3d e : linMod.getElements()) {
      // linModPerElementMass += e.getMass();
      // }
      // linModPerElementMass /= quadMod.getElements().size();

      for (FemNode3d n : quadNodes) {
         n.clearMass();
      }

      double density = linMod.getDensity();
      for (FemElement3d e : quadMod.getElements()) {
         double mass = e.getRestVolume() * density;
         e.setMass(mass);
         e.invalidateNodeMasses();
      }
      return quadMod;
   }

   /**
    * Creates a regular grid composed of quadratic tet elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      FemModel3d tetmod = new FemModel3d();
      createTetGrid(tetmod, widthX, widthY, widthZ, numX, numY, numZ);

      createQuadraticModel(model, tetmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);
      return model;
   }

   /**
    * Creates a tube made of quadratic tet elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetTube(
      FemModel3d model,
      double l, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTube (
         model, l, rin, rout, nt, nl, nr, /*useHexes=*/false);
   }

   /**
    * Creates a hollow torus made of quadratic tet elements. Identical to
    * {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetTorus(
      FemModel3d model, 
      double R, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTorus (
         model, R, rin, rout, nt, nl, nr, /*useHexes=*/false);
   }

   /**
    * Creates a shell-based FEM model made of quadratic tet elements by
    * extruding a surface mesh along the normal direction of its faces.
    * Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      FemModel3d tetmod = new FemModel3d();
      createTetExtrusion(tetmod, n, d, zOffset, surface);
      createQuadraticModel(model, tetmod);
      return model;
   }

   /**
    * Creates a regular grid composed of quadratic hex elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      
      FemModel3d hexmod = new FemModel3d();
      createHexGrid(hexmod, widthX, widthY, widthZ, numX, numY, numZ);
      createQuadraticModel(model, hexmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);
      return model;
   }

   /**
    * Creates a regular grid composed of quadratic wedge elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadWedge}.
    */
   public static FemModel3d createQuadwedgeGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      FemModel3d linmod = new FemModel3d();
      createWedgeGrid(linmod, widthX, widthY, widthZ, numX, numY, numZ);

      createQuadraticModel(model, linmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);

      return model;
   }

   /**
    * Creates a regular grid composed of quadratic pyramid elements. Identical
    * to {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadPyramid}.
    */
   public static FemModel3d createQuadpyramidGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      FemModel3d linmod = new FemModel3d();
      createPyramidGrid(linmod, widthX, widthY, widthZ, numX, numY, numZ);

      createQuadraticModel(model, linmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);

      return model;
   }


   /**
    * Creates a tube made of quadratic hex elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexTube(
      FemModel3d model,
      double l, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTube (
         model, l, rin, rout, nt, nl, nr, /*useHexes=*/true);
   }

   /**
    * Creates a hollow torus made of quadratic hex elements. Identical to
    * {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexTorus(
      FemModel3d model,
      double R, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTorus (
         model, R, rin, rout, nt, nl, nr, /*useHexes=*/true);
   }

   /**
    * Creates a shell-based FEM model made of quadratic hex elements by
    * extruding a surface mesh along the normal direction of its faces. The
    * surface mesh must be composed of quads.  Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      FemModel3d hexmod = new FemModel3d();
      createHexExtrusion(hexmod, n, d, zOffset, surface);
      createQuadraticModel(model, hexmod);
      return model;
   }

   /**
    * Creates a shell-based FEM model made of quadratic wedge elements by
    * extruding a surface mesh along the normal direction of its faces.  The
    * surface mesh must be composed of triangles. Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Wedge}.
    */
   public static FemModel3d createQuadwedgeExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      FemModel3d wedgemod = new FemModel3d();
      createWedgeExtrusion(wedgemod, n, d, zOffset, surface);
      createQuadraticModel(model, wedgemod);
      return model;
   }

   private static void setGridEdgesHard(
      FemModel3d model, double widthX, double widthY, double widthZ) {

      // and now set the surface edges hard ...
      PolygonalMesh mesh = model.getSurfaceMesh();
      // iterate through all edges in the surface mesh.
      for (Face face : mesh.getFaces()) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            Vertex3d tailv = he.getTail();
            Vertex3d headv = he.getHead();
            int mutualSurfaces = 
               (gridBoundarySurfaces (tailv.pnt, widthX, widthY, widthZ) &
                gridBoundarySurfaces (headv.pnt, widthX, widthY, widthZ));
            if (bitCount (mutualSurfaces) > 1) {
               mesh.setHardEdge(tailv, headv, true);
            }
            he = he.getNext();
         }
         while (he != he0);
      }
   }

   /**
    * Creates a shell-based FEM model made of hex elements by extruding a
    * surface mesh along the normal direction of its faces.  The surface mesh
    * must be composed of quads.  Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }
      if (!surface.isQuad()) {
         throw new IllegalArgumentException (
            "Hex extrusion requires a quad mesh");
      }

      for (Vertex3d v : surface.getVertices()) {
         model.addNode(new FemNode3d(v.pnt));
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();
      for (int i = 0; i < n; i++) {
         for (Vertex3d v : surface.getVertices()) {
            v.computeNormal(nrm);
            newpnt.scaledAdd((i + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         for (Face f : surface.getFaces()) {
            FemNode3d[] nodes = new FemNode3d[8];
            int cnt = 0;

            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
            }
            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] = model.getNode(idx + i * surface.numVertices());
            }

            HexElement e = new HexElement(nodes);
            model.addElement(e);
         }
      }
      return model;
   }

   /**
    * Creates a shell-based FEM model made of wedge elements by extruding a
    * surface mesh along the normal direction of its faces.  The surface mesh
    * must be composed of triangles. Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Wedge}.
    */
   public static FemModel3d createWedgeExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }
      if (!surface.isTriangular()) {
         throw new IllegalArgumentException (
            "Wedge extrusion requires a triangular mesh");
      }

      for (Vertex3d v : surface.getVertices()) {
         model.addNode(new FemNode3d(v.pnt));
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();
      for (int i = 0; i < n; i++) {
         for (Vertex3d v : surface.getVertices()) {
            v.computeNormal(nrm);
            newpnt.scaledAdd((i + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         for (Face f : surface.getFaces()) {
            FemNode3d[] nodes = new FemNode3d[6];
            int cnt = 0;

            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
            }
            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] = model.getNode(idx + i * surface.numVertices());
            }

            WedgeElement e = new WedgeElement(nodes);
            model.addElement(e);
         }
      }
      return model;
   }

   /**
    * Given a triangular face associated with an element, finds the
    * corresponding face in the element, and if that face is a quad, returns the
    * additional node completes the quad.
    * @param surfaceFem TODO
    */
   private static FemNode3d getQuadFaceNode(
      Face tri, FemElement3d elem, FemModel3d surfaceFem) {

      int[] faceNodeIdxs = elem.getFaceIndices();
      boolean[] marked = new boolean[4];
      int[] localTriIdxs = new int[3];

      for (int k = 0; k < 3; k++) {
         FemNode node = surfaceFem.getSurfaceNode (tri.getVertex(k));
         localTriIdxs[k] = elem.getLocalNodeIndex(node);
         if (localTriIdxs[k] == -1) {
            throw new InternalErrorException(
               "tri does not share all nodes with element");
         }
      }

      // Check each face in the element to see if it is a quad, and if
      // so, whether it contains tri.
      for (int i = 0; i < faceNodeIdxs.length; i += (faceNodeIdxs[i] + 1)) {
         int j, k;
         if (faceNodeIdxs[i] == 4) {
            // only consider quad faces
            for (j = 0; j < 4; j++) {
               marked[j] = false;
            }
            // see if every node in tri lies in the face
            for (k = 0; k < 3; k++) {
               int li = localTriIdxs[k];
               for (j = 0; j < 4; j++) {
                  if (li == faceNodeIdxs[j + i + 1]) {
                     marked[j] = true;
                     break;
                  }
               }
               if (j == 4) {
                  // node is not in face
                  break;
               }
            }
            if (k == 3) {
               // every node in tri does lie in the i-th face, so
               // return the remaining node
               for (j = 0; j < 4; j++) {
                  if (!marked[j]) {
                     int li = faceNodeIdxs[j + i + 1];
                     return elem.getNodes()[li];
                  }
               }
            }
         }
      }
      return null;
   }

   /**
    * Creates a shell-based FEM model by extruding a surface mesh along the
    * normal direction of its faces. The element types used depend on the
    * underlying faces: triangular faces generate wedge elements, while quad
    * faces generate hex elements. If the mesh is the surface mesh of an
    * underlying FemModel, then each triangle is examined to see if it is
    * associated with an underlying hex element, and if it is, then a hex
    * element is extruded from both the surface triangles connected to that
    * element. The shell can have multiple layers; the number of layers is
    * <code>n</code>.
    * 
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch. Note that
    * <code>model</code> must be different from <code>surfaceFem</code>
    * @param n number of layers
    * @param d layer thickness
    * @param zOffset offset from the surface
    * @param surface surface mesh to extrude
    * @param surfaceFem FEM associated with the surface mesh, or 
    * <code>null</code> if there is no associated FEM.
    * @return extruded FEM model, which will be <code>model</code> if
    * that argument is not <code>null</code>.
    */
   public static FemModel3d createHexWedgeExtrusion(
      FemModel3d model, int n, double d, double zOffset, PolygonalMesh surface, 
      FemModel3d surfaceFem) {

      if (model == null) {
         model = new FemModel3d();
      } else if (model == surfaceFem) {
         throw new IllegalArgumentException (
            "model and surfaceFem cannot be the same FEM");
      } else {
         model.clear();
      }
      if (n < 1) {
         throw new IllegalArgumentException ("n must be >= 1");
      }

      for (Vertex3d v : surface.getVertices()) {
         FemNode3d node = new FemNode3d(v.pnt);
         model.addNode(node);
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();
      for (int l = 0; l < n; l++) {
         // surface.transform (new RigidTransform3d (avgNormal,
         // new RotationMatrix3d()));

         boolean[] marked = new boolean[surface.numFaces()];

         for (Vertex3d v : surface.getVertices()) {
            v.computeAngleWeightedNormal(nrm);
            newpnt.scaledAdd((l + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         int numSurfVtxs = surface.numVertices();

         for (int i = 0; i < surface.numFaces(); i++) {
            if (!marked[i]) {
               Face f = surface.getFaces().get(i);
               int numv = f.numVertices();

               if (numv != 3 && numv != 4) {
                  throw new IllegalArgumentException(
                     "Surface mesh must consist of triangles and/or quads");
               }

               int[] vertexIndices = null;
               // For cases where the surface mesh is an an actual FEM surface
               // mesh, find the element corresponding to this face. Otherwise,
               // elem will be set to null.
               FemNode3d quadNode = null;
               if (surfaceFem != null) {
                  FemElement3d elem = surfaceFem.getSurfaceElement(f);
                  if (elem != null && numv == 3) {
                     // If there is an element associated with f, and f is a
                     // triangle, see if the element has a corresponding quad 
                     // face and if so, find the extra node associated with it.
                     quadNode = getQuadFaceNode(f, elem, surfaceFem);
                  }
               }
               if (quadNode != null) {
                  vertexIndices = new int[4];
                  // iterate through the face edges to build up the list
                  // of vertex indices
                  HalfEdge he = f.firstHalfEdge();
                  int k = 0;
                  for (int j = 0; j < 3; j++) {
                     vertexIndices[k++] = he.tail.getIndex();
                     Vertex3d vop = he.opposite.getNext().head;
                     if (surfaceFem.getSurfaceNode(vop) == quadNode) {
                        // add the extra quad vertex if it is on the triangle
                        // opposite this half edge, and mark that triangle.
                        vertexIndices[k++] = vop.getIndex();
                        marked[he.opposite.getFace().getIndex()] = true;
                     }
                     he = he.getNext();
                  }
               } else {
                  vertexIndices = f.getVertexIndices();
               }

               FemElement3d e;
               // Note: vertexIndices gives the indices of the surface face (or
               // composed quad face) in counter-clockwise order
               if (vertexIndices.length == 3) {
                  // add wedge element, which requires the first three nodes be
                  // around clockwise around a face
                  FemNode3d[] nodes = new FemNode3d[6];
                  for (int j = 0; j < 3; j++) {
                     int idx = vertexIndices[j];
                     nodes[j] = model.getNode(idx + l * numSurfVtxs);
                     nodes[j + 3] = model.getNode(idx + (l + 1) * numSurfVtxs);
                  }
                  e = new WedgeElement(nodes);
               } else {
                  // add hex element, which requires first four nodes to
                  // be arranged counter-clockwise around a face
                  FemNode3d[] nodes = new FemNode3d[8];
                  for (int j = 0; j < 4; j++) {

                     int idx = vertexIndices[j];
                     nodes[j] = model.getNode(idx + (l + 1) * numSurfVtxs);
                     nodes[j + 4] = model.getNode(idx + l * numSurfVtxs);
                  }
                  e = new HexElement(nodes);
               }
               model.addElement(e);
               marked[f.getIndex()] = true;
            }
         }
      }
      return model;
   }

   private static void getTypeConstraints(int[] res, Face face, int[] types) {

      int mustHave = 0;
      int dontCare = 0;

      HalfEdge he = face.firstHalfEdge();
      for (int i = 0; i < 3; i++) {
         Face opface = he.opposite != null ? he.opposite.getFace() : null;
         if (opface == null || types[opface.getIndex()] == 0) {
            dontCare |= (1 << i);
         } else {
            int optype = types[opface.getIndex()];
            int k = opface.indexOfEdge(he.opposite);
            if ((optype & (1 << k)) == 0) {
               mustHave |= (1 << i);
            }
         }
         he = he.getNext();
      }
      res[0] = mustHave;
      res[1] = dontCare;
   }

   private static int[] computeTesselationTypes(PolygonalMesh surface) {

      int numFaces = surface.numFaces();

      int[] types = new int[numFaces];

      // compute a valid set of tetrahedral tesselation types for an extruded
      // triangular mesh, to ensure that all faces of the resulting tesselation
      // match up properly. This algorithm is from Erleben and Dohlmann,
      // "The Thin Shell Tetrahedral Mesh".

      Random rand = new Random(0x1234);

      int[] res = new int[2];
      int[] candidates = new int[6];

      LinkedList<Face> queue = new LinkedList<Face>();
      queue.offer(surface.getFaces().get(0));
      while (!queue.isEmpty()) {
         Face face = queue.poll();

         if (types[face.getIndex()] != 0) {
            // already visited; continue
            continue;
         }

         getTypeConstraints(res, face, types);
         int mustHave = res[0];
         int dontCare = res[1];
         int type = 0;
         if (dontCare == 0 && (mustHave == 0 || mustHave == 7)) {
            HalfEdge he = face.firstHalfEdge();
            for (int i = 0; i < 3; i++) {
               Face opface = he.opposite.getFace();
               int flippedType = (types[opface.getIndex()] ^ (1 << i));
               if (0 < flippedType && flippedType < 7) {
                  // good - fixes it
                  types[opface.getIndex()] = flippedType;
                  type = (mustHave ^ (1 << i));
                  System.out.println("flipping types");
                  break;
               }
               he = he.getNext();
            }
            if (type == 0) {
               // have to keep looking
               type = (mustHave == 0 ? 0x1 : 0x6);
               System.out.println("Warning: incompatible type " + type
                  + ", face " + face.getIndex());

            }
         } else {
            int k = 0;
            for (int code = 1; code <= 6; code++) {
               if ((code & ~dontCare) == mustHave) {
                  candidates[k++] = code;
               }
            }
            type = candidates[rand.nextInt(k)];
         }
         // System.out.println ("face "+face.getIndex()+" " + type);

         types[face.getIndex()] = type;
         HalfEdge he = face.firstHalfEdge();
         for (int i = 0; i < 3; i++) {
            Face opface = he.opposite != null ? he.opposite.getFace() : null;
            if (opface != null && types[opface.getIndex()] == 0) {
               // System.out.println ("offering " + opface.getIndex());
               queue.offer(opface);
            }
            he = he.getNext();
         }
      }
      return types;
   }

   /**
    * Creates a shell-based FEM model by extruding a surface mesh along the
    * normal direction of its faces. The element types used depend on the
    * underlying faces: triangular faces generate wedge elements, while quad
    * faces generate hex elements. The shell can have multiple layers; the
    * number of layers is <code>n</code>.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param n number of layers
    * @param d layer thickness
    * @param zOffset offset from the surface
    * @param surface surface mesh to extrude
    * @return extruded FEM model, which will be <code>model</code> if that
    * argument is not <code>null</code>
    * @throws IllegalArgumentException if the specified element type is not
    * supported, or if the surface faces are not triangles or quads.
    */
   public static FemModel3d createExtrusion(
      FemModel3d model, int n, double d, double zOffset, 
      PolygonalMesh surface) {
      
      // create model
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      if (n < 1) {
         throw new IllegalArgumentException ("n must be >= 1");
      }

      // compute normals
      Vector3d[] normals = new Vector3d[surface.numVertices()];
      for (int i=0; i<surface.numVertices(); i++) {
         normals[i] = new Vector3d();
         Vertex3d vtx = surface.getVertex(i);
         vtx.computeNormal(normals[i]);
      }
      
      // add vertices as nodes
      Point3d newpnt = new Point3d();
      for (int i=0; i<surface.numVertices(); i++) {
         Vertex3d v = surface.getVertex(i);
         FemNode3d newnode = new FemNode3d(v.pnt);
         model.addNode(newnode);
      }

      
      for (int i = 0; i < n; i++) {
         for (int j=0; j < surface.numVertices(); j++) {
            Vertex3d v = surface.getVertex(j);
            newpnt.scaledAdd((i + 1) * d + zOffset, normals[j], v.pnt);
            FemNode3d newnode = new FemNode3d(newpnt);
            model.addNode(newnode);
         }

         for (Face f : surface.getFaces()) {
            int numv = f.numVertices();
            if (numv != 3 && numv != 4) {
               throw new IllegalArgumentException (
                  "Surfaces face "+f.getIndex()+" has "+numv+
                  " vertices. Only triangles and quads are supported");
            }
            FemNode3d[] nodes = new FemNode3d[2 * numv];
            int cnt = 0;

            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
            }
            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] = model.getNode(idx + i * surface.numVertices());
            }

            // hex and wedge have different winding order, swap around
            if (numv != 4) {
               FemNode3d tmp;
               for (int k=0; k<numv; ++k) {
                  tmp = nodes[k];
                  nodes[k] = nodes[k+numv];
                  nodes[k+numv] = tmp;
               }
            }
            FemElement3d e = FemElement3d.createElement(nodes);
            model.addElement(e);

            // System.out.println("node idxs");
            // for (int c = 0; c < e.getNodes().length; c++)
            //    System.out.print(e.getNodes()[c].getNumber() + ", ");
            // System.out.println("");
         }
      }
      return model;
   }
   
   /**
    * Creates a shell-based FEM model made of tet elements by
    * extruding a surface mesh along the normal direction of its faces. 
    * Identical to
    * {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      if (!surface.isTriangular()) {
         throw new IllegalArgumentException (
            "Tet extrusion requires a triangular mesh");
      }

      for (Vertex3d v : surface.getVertices()) {
         model.addNode(new FemNode3d(v.pnt));
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();

      int[] tesselationTypes = null;
      tesselationTypes = computeTesselationTypes(surface);

      for (int i = 0; i < n; i++) {

         for (Vertex3d v : surface.getVertices()) {
            v.computeAngleWeightedNormal(nrm);
            newpnt.scaledAdd((i + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         for (Face f : surface.getFaces()) {

            // HalfEdge he = f.firstHalfEdge();
            // for (int k=0; k<3; k++) {
            // System.out.print (he.head.getIndex() + " ");
            // he = he.getNext();
            // }
            // System.out.println ("");

            int numf = f.numVertices();
            FemNode3d[] nodes = new FemNode3d[2 * numf];
            // int cnt = 0;

            HalfEdge he = f.firstHalfEdge();
            for (int k = 0; k < numf; k++) {
               int idx = he.tail.getIndex();
               nodes[k] = model.getNode(idx + i * surface.numVertices());
               nodes[k + numf] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
               he = he.getNext();
            }

            // for (Integer idx : f.getVertexIndices()) {
            // nodes[cnt++] =
            // model.getNode (idx + i * surface.numVertices());
            // }
            // for (Integer idx : f.getVertexIndices()) {
            // nodes[cnt++] =
            // model.getNode (idx + (i + 1) * surface.numVertices());
            // }

            TetElement[] tets;
            if (surface.isQuad()) {
               tets =
                  TetElement.createCubeTesselation(
                     nodes[4], nodes[5], nodes[6], nodes[7], nodes[0],
                     nodes[1], nodes[2], nodes[3], true);
            } else {
               tets =
                  TetElement.createWedgeTesselation(
                     nodes[3], nodes[4], nodes[5], nodes[0], nodes[1],
                     nodes[2], tesselationTypes[f.getIndex()]);
            }

            for (TetElement tet : tets) {
               model.addElement(tet);
            }
         }
      }
      return model;
   }

   /**
    * Creates a regular grid composed of tet elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */

   /**
    * Creates a regular grid, composed of elements of the type specified by
    * <code>type</code>, centered on the origin, with specified widths and grid
    * resolutions along each axis.
    *
    * @param model model to which the hex elements be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param widthX x axis model width
    * @param widthY y axis model width
    * @param widthZ z axis model width
    * @param numX element resolution along the x axis
    * @param numY element resolution along the y axis
    * @param numZ element resolution along the z axis
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createGrid(
      FemModel3d model, FemElementType type, double widthX, double widthY,
      double widthZ, int numX, int numY, int numZ) {
      switch (type) {
         case Tet:
            return createTetGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case Hex:
            return createHexGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case Wedge:
            return createWedgeGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case QuadTet:
            return createQuadtetGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case QuadHex:
            return createQuadhexGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case QuadWedge:
            return createQuadwedgeGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a tube made of either tet, hex, quadTet, or quadHex elements, as
    * specified by <code>type</code>. Note that the element resolution 
    * <code>nt</code> around the central axis will be rounded up to 
    * an even number for tet or quadTet models. 
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param l length along the z axis
    * @param rin inner radius
    * @param rout outer radius
    * @param nt element resolution around the central axis (will be 
    * rounded up to an even number for tet or quadTet models)
    * @param nl element resolution along the length
    * @param nr element resolution along the thickness
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createTube (
      FemModel3d model, FemElementType type,
      double l, double rin, double rout, int nt, int nl, int nr) {
      switch (type) {
         case Tet:
            return createTetTube(model, l, rin, rout, nt, nl, nr);
         case Hex:
            return createHexTube(model, l, rin, rout, nt, nl, nr);
         case QuadTet:
            return createQuadtetTube(model, l, rin, rout, nt, nl, nr);
         case QuadHex:
            return createQuadhexTube(model, l, rin, rout, nt, nl, nr);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a partial tube made of either tet or hex elements, as specified by
    * <code>type</code>.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param l length along the z axis
    * @param rin inner radius
    * @param rout outer radius
    * @param theta size of the partial tube slice, in radians
    * @param nl element resolution along the length
    * @param nr element resolution along the thickness
    * @param ntheta element resolution along the slice
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createPartialTube(
      FemModel3d model, FemElementType type, double l, double rin, double rout,
      double theta, int nl, int nr, int ntheta) {
      switch (type) {
         case Tet:
            return createPartialTetTube (
               model, l, rin, rout, theta, nl, nr, ntheta);
         case Hex:
            return createPartialHexTube (
               model, l, rin, rout, theta, nl, nr, ntheta);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a hollow torus made of either tet, hex, quadTet, or quadHex
    * elements, as specified by <code>type</code>. The result is essentially
    * a tube, with inner and outer radii given by <code>rin</code>
    * and <code>rout</code>, bent around the major radius R and connected.
    * For tet or quadTet models, the element resolutions <code>nt</code>
    * and <code>nl</code> will be rounded up to an even number.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param R major radius
    * @param rin inner part of the minor radius
    * @param rout outer part of the minor radius
    * @param nt element resolution around the major radius (will be rounded
    * up to an even number for tet or quadTet models)
    * @param nl element resolution around the minor radius (will be rounded
    * up to an even number for tet or quadTet models)
    * @param nr element resolution along the inner thickness
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createTorus(
      FemModel3d model, FemElementType type, double R, double rin, double rout,
      int nt, int nl, int nr) {
      switch (type) {
         case Tet:
            return createTetTorus(model, R, rin, rout, nt, nl, nr);
         case Hex:
            return createHexTorus(model, R, rin, rout, nt, nl, nr);
         case QuadTet:
            return createQuadtetTorus(model, R, rin, rout, nt, nl, nr);
         case QuadHex:
            return createQuadhexTorus(model, R, rin, rout, nt, nl, nr);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a shell-based FEM model by extruding a surface mesh along the
    * normal direction of its faces. The model is composed of either tet, hex,
    * quadTet or quadHex elements, as specified by <code>type</code>. The shell
    * can have multiple layers; the number of layers is <code>n</code>.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param n number of layers
    * @param d layer thickness
    * @param zOffset offset from the surface
    * @param surface surface mesh to extrude
    * @return extruded FEM model, which will be <code>model</code> if
    * that argument is not <code>null</code>
    * @throws IllegalArgumentException if the specified element type is not
    * supported, or if the surface faces are incompatible with the element
    * type.
    */
   public static FemModel3d createExtrusion(
      FemModel3d model, FemElementType type, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      switch (type) {
         case Tet:
            return createTetExtrusion(model, n, d, zOffset, surface);
         case Hex:
            return createHexExtrusion(model, n, d, zOffset, surface);
         case Wedge:
            return createWedgeExtrusion(model, n, d, zOffset, surface);
         case QuadTet:
            return createQuadtetExtrusion(model, n, d, zOffset, surface);
         case QuadHex:
            return createQuadhexExtrusion(model, n, d, zOffset, surface);
         case QuadWedge:
            return createQuadwedgeExtrusion(model, n, d, zOffset, surface);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a tetrahedral FEM model from a triangular surface mesh. The
    * tetrahedra will be added to either an existing model (supplied through the
    * argument <code>model</code>), or a newly created <code>FemModel3d</code>
    * (if <code>model</code> is <code>null</code>).
    * 
    * <p>
    * The tessellation is done using tetgen, which is called through a JNI
    * interface. The tessellation quality is controlled using the
    * <code>quality</code> variable, described below.
    *
    * @param model
    * model to which the tetrahedra should be added, or <code>null</code> if the
    * model is to be created from scratch.
    * @param surface
    * triangular surface mesh used to define the tessellation.
    * @param quality
    * If 0, then only the
    * mesh nodes will be used to form the tessellation. However, this may result
    * in highly degenerate tetrahedra. Otherwise, if &gt;
    * 0, tetgen will add additional nodes to ensure that the minimum edge-radius
    * ratio does not exceed <code>quality</code>. A good default value for
    * <code>quality</code> is 2. If set too small (such as less then 1), then
    * tetgen may not terminate.
    * @return the FEM model
    */
   public static FemModel3d createFromMesh(
      FemModel3d model, PolygonalMesh surface, double quality) {
      TetgenTessellator tetgen = new TetgenTessellator();
      tetgen.buildFromMesh(surface, quality);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();
      int[] tets = tetgen.getTets();
      for (int i = 0; i < tets.length / 4; i++) {
         FemNode3d n0 = nodes.get(tets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(tets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(tets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(tets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Constrained Delaunay, including the supplied list of points if they fall
    * inside the surface
    */
   public static FemModel3d createFromMeshAndPoints(
      FemModel3d model, PolygonalMesh surface, double quality, Point3d[] pnts) {

      TetgenTessellator tetgen = new TetgenTessellator();
      tetgen.buildFromMeshAndPoints(surface, quality, pnts);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();

      int[] tets = tetgen.getTets();
      for (int i = 0; i < tets.length / 4; i++) {
         FemNode3d n0 = nodes.get(tets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(tets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(tets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(tets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Creates a refined version of a an existing tetrahedral FEM model using
    * tetgen and a list of supplemental node locations.
    * 
    * @param model model in which the refined model be built, or
    * <code>null</code> if the model is to be created from scratch.
    * @param input original FEM model which is to be refined
    * @param quality quality factor used by tetgen to refine the model
    * @param pnts locations of the supplemental node
    * @return refined FEM model
    */
   public static FemModel3d refineFem(
      FemModel3d model, FemModel3d input, double quality, Point3d[] pnts) {

      TetgenTessellator tetgen = new TetgenTessellator();

      int[] tets = new int[4 * input.numElements()];
      double[] nodeCoords = new double[3 * input.numNodes()];
      double[] addCoords = new double[3 * pnts.length];

      int idx = 0;
      for (FemNode3d node : input.getNodes()) {
         node.setIndex(idx);
         Point3d pos = node.getPosition();
         nodeCoords[3 * idx] = pos.x;
         nodeCoords[3 * idx + 1] = pos.y;
         nodeCoords[3 * idx + 2] = pos.z;
         idx++;
      }

      idx = 0;
      int numTets = 0;
      for (FemElement3d elem : input.getElements()) {
         if (elem instanceof TetElement) {
            FemNode3d[] nodes = elem.getNodes();
            tets[idx++] = nodes[0].getIndex();
            tets[idx++] = nodes[1].getIndex();
            tets[idx++] = nodes[2].getIndex();
            tets[idx++] = nodes[3].getIndex();
            numTets++;
         }
      }

      idx = 0;
      for (Point3d pnt : pnts) {
         addCoords[idx++] = pnt.x;
         addCoords[idx++] = pnt.y;
         addCoords[idx++] = pnt.z;

      }

      //
      // tetgen.buildFromMeshAndPoints (surface, quality, pnts);
      tetgen.refineMesh(
         nodeCoords, input.numNodes(), tets, numTets, quality, addCoords,
         pnts.length);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();

      int[] outTets = tetgen.getTets();
      for (int i = 0; i < outTets.length / 4; i++) {
         FemNode3d n0 = nodes.get(outTets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(outTets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(outTets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(outTets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Creates a refined version of a an existing tetrahedral FEM model using
    * tetgen.
    * 
    * @param model model in which the refined model be built, or
    * <code>null</code> if the model is to be created from scratch.
    * @param input original FEM model which is to be refined
    * @param quality quality factor used by tetgen to refine the model
    * @return refined FEM model
    */
   public static FemModel3d refineFem(
      FemModel3d model, FemModel3d input, double quality) {

      TetgenTessellator tetgen = new TetgenTessellator();

      int[] tets = new int[4 * input.numElements()];
      double[] nodeCoords = new double[3 * input.numNodes()];

      int idx = 0;
      for (FemNode3d node : input.getNodes()) {
         node.setIndex(idx);
         Point3d pos = node.getPosition();
         nodeCoords[3 * idx] = pos.x;
         nodeCoords[3 * idx + 1] = pos.y;
         nodeCoords[3 * idx + 2] = pos.z;
         idx++;
      }

      idx = 0;
      int numTets = 0;
      for (FemElement3d elem : input.getElements()) {
         if (elem instanceof TetElement) {
            FemNode3d[] nodes = elem.getNodes();
            tets[idx++] = nodes[0].getIndex();
            tets[idx++] = nodes[1].getIndex();
            tets[idx++] = nodes[2].getIndex();
            tets[idx++] = nodes[3].getIndex();
            numTets++;
         }
      }

      //
      // tetgen.buildFromMeshAndPoints (surface, quality, pnts);
      tetgen.refineMesh(nodeCoords, input.numNodes(), tets, numTets, quality);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();
      int[] outTets = tetgen.getTets();
      for (int i = 0; i < outTets.length / 4; i++) {
         FemNode3d n0 = nodes.get(outTets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(outTets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(outTets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(outTets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Adds a copy of the nodes, elements, markers and attachments from
    * <code>fem1</code> to <code>fem0</code>. Nodes in fem1 are merged with
    * nodes in fem0 that are within TOL distance of each other, where TOL is
    * 1e-8 times the maximum radius of fem0 and fem1. For precise control of
    * node merging, use {@link #addFem(FemModel3d,FemModel3d,double)}.
    * 
    * @param fem0
    * FEM model to which components should be added
    * @param fem1
    * FEM model providing components
    */
   public static void addFem(FemModel3d fem0, FemModel3d fem1) {

      double tol =
         1e-8 * Math.max(
            RenderableUtils.getRadius(fem0), RenderableUtils.getRadius(fem1));
      addFem(fem0, fem1, tol);
   }

   /**
    * Adds a copy of the nodes, elements, markers and attachments from
    * <code>fem1</code> to <code>fem0</code>.
    * 
    * @param fem0
    * FEM model to which components should be added
    * @param fem1
    * FEM model providing components
    * @param nodeMergeDist
    * If &gt;= 0, causes nearby nodes of <code>fem1</code> and <code>fem0</code> to
    * be merged: any node of <code>fem1</code> that is within
    * <code>nodeMergeDist</code> of a node in <code>fem0</code> is replaced by
    * the nearest node in <code>fem0</code>.
    */
   public static void addFem(
      FemModel3d fem0, FemModel3d fem1, double nodeMergeDist) {

      int flags = CopyableComponent.COPY_REFERENCES;
      HashMap<ModelComponent,ModelComponent> copyMap =
         new HashMap<ModelComponent,ModelComponent>();
      ArrayList<FemNode3d> newNodes = new ArrayList<FemNode3d>();

      // Go through all nodes in fem1 and either copy them, or find their
      // nearest counterparts in fem0 that are within a distance given by
      // nodeMergeDist.
      //
      // Note that we want to first find all the new nodes, and then add them
      // later as a group, to avoid merging nodes in fem1 (and also to avoid
      // constantly recomputing the bounding volume hierarchy in fem0).
      for (FemNode3d n : fem1.myNodes) {
         FemNode3d newn;
         if (nodeMergeDist < 0
            || (newn = fem0.findNearestNode(n.getPosition(), nodeMergeDist)) == null) {
            newn = n.copy(flags, copyMap);
            newn.setName(n.getName());
            newNodes.add(newn);
         }
         copyMap.put(n, newn);
      }

      for (FemNode3d n : newNodes) {
         fem0.myNodes.add(n);
      }
      for (FemElement3d e : fem1.myElements) {
         FemElement3d newe = e.copy(flags, copyMap);
         newe.setName(e.getName());
         copyMap.put(e, newe);
         fem0.myElements.add(newe);
      }
      for (FemMarker m : fem1.myMarkers) {
         FemMarker newm = m.copy(flags, copyMap);
         newm.setName(m.getName());
         fem0.myMarkers.add(newm);
      }
      for (DynamicAttachment a : fem1.myAttachments) {
         DynamicAttachment newa = a.copy(flags, copyMap);
         newa.setName(a.getName());
         fem0.myAttachments.add(newa);
      }
   }

   private static class Edge {

      FemNode3d myN0;
      FemNode3d myN1;

      public Edge(FemNode3d n0, FemNode3d n1) {
         myN0 = n0;
         myN1 = n1;
      }

      public boolean equals(Object obj) {
         if (obj instanceof Edge) {
            Edge e = (Edge)obj;
            return ((e.myN0 == myN0 && e.myN1 == myN1) || (e.myN1 == myN0 && e.myN0 == myN1));
         } else {
            return false;
         }
      }

      public int hashCode() {
         return (myN0.hashCode() + myN1.hashCode());
      }
   }

   private static FemNode3d createNode(FemNode3d[] nodes) {
      Point3d pos = new Point3d();
      FemNode3d node = new FemNode3d();
      for (FemNode3d n : nodes) {
         pos.add(n.getPosition());
      }
      pos.scale(1.0 / nodes.length);
      node.setPosition(pos);
      pos.setZero();
      for (FemNode3d n : nodes) {
         pos.add(n.getRestPosition());
      }
      pos.scale(1.0 / nodes.length);
      node.setRestPosition(pos);
      return node;
   }

   private static TetElement createTet(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3) {
      return new TetElement(nodes[i0], nodes[i1], nodes[i2], nodes[i3]);
   }

   private static WedgeElement createWedge(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3, int i4, int i5) {
      return new WedgeElement(
         nodes[i0], nodes[i1], nodes[i2], nodes[i3], nodes[i4], nodes[i5]);
   }

   private static PyramidElement createPyramid(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3, int i4) {

      return new PyramidElement(
         nodes[i0], nodes[i1], nodes[i2], nodes[i3], nodes[i4]);
   }

   private static HexElement createHex(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3, int i4, int i5,
      int i6, int i7) {
      return new HexElement(
         nodes[i0], nodes[i1], nodes[i2], nodes[i3], nodes[i4], nodes[i5],
         nodes[i6], nodes[i7]);
   }

   private static FemNode3d getEdgeNode(
      FemModel3d fem, FemNode3d n0, FemNode3d n1,
      HashMap<Edge,FemNode3d> edgeNodeMap) {

      Edge edge = new Edge(n0, n1);
      FemNode3d node;
      if ((node = edgeNodeMap.get(edge)) == null) {
         node = createNode(new FemNode3d[] { n0, n1 });
         edgeNodeMap.put(edge, node);
         fem.addNode(node);
      }
      return node;
   }

   private static FemNode3d getQuadFaceNode(
      FemModel3d fem, FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3,
      HashMap<Edge,FemNode3d> edgeNodeMap) {

      Edge edge02 = new Edge(n0, n2);
      Edge edge13 = new Edge(n1, n3);
      FemNode3d node;
      if ((node = edgeNodeMap.get(edge02)) == null
         && (node = edgeNodeMap.get(edge13)) == null) {
         node = createNode(new FemNode3d[] { n0, n1, n2, n3 });
         edgeNodeMap.put(edge02, node);
         edgeNodeMap.put(edge13, node);
         fem.addNode(node);
      }
      return node;
   }

   private static void addSubdivisionNodes(
      FemNode3d[] newn, FemModel3d fem, FemElement3d e,
      HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      int idx = 0;
      FemNode3d[] oldn = e.getNodes();
      for (int i = 0; i < oldn.length; i++) {
         newn[idx++] = (FemNode3d)copyMap.get(oldn[i]);
      }
      int[] edgeIdxs = e.getEdgeIndices();
      for (int i = 0; i < edgeIdxs.length;) {
         // for each edge ...
         i++;
         FemNode3d n0 = oldn[edgeIdxs[i++]];
         FemNode3d n1 = oldn[edgeIdxs[i++]];
         newn[idx++] = getEdgeNode(fem, n0, n1, edgeNodeMap);
      }
      int[] faceIdxs = e.getFaceIndices();
      for (int i = 0; i < faceIdxs.length;) {
         // for each face ...
         int nn = faceIdxs[i++];
         if (nn == 4) {
            FemNode3d n0 = oldn[faceIdxs[i++]];
            FemNode3d n1 = oldn[faceIdxs[i++]];
            FemNode3d n2 = oldn[faceIdxs[i++]];
            FemNode3d n3 = oldn[faceIdxs[i++]];
            newn[idx++] = getQuadFaceNode(fem, n0, n1, n2, n3, edgeNodeMap);
         } else {
            i += nn;
         }
      }
      if (idx < newn.length) {
         // create center node ...
         newn[idx] = createNode((FemNode3d[])e.getNodes());
         fem.addNode(newn[idx]);
      }
   }

   private static void subdivideTet(
      FemModel3d fem, TetElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[10];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createTet(newn, 0, 4, 5, 6));
      fem.addElement(createTet(newn, 5, 7, 2, 8));
      fem.addElement(createTet(newn, 4, 1, 7, 9));
      fem.addElement(createTet(newn, 6, 9, 8, 3));
      fem.addElement(createTet(newn, 4, 9, 7, 5));
      fem.addElement(createTet(newn, 5, 7, 8, 9));
      fem.addElement(createTet(newn, 6, 4, 5, 9));
      fem.addElement(createTet(newn, 5, 9, 8, 6));
   }

   private static void subdivideHex(
      FemModel3d fem, HexElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[27];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createHex(newn, 0, 8, 20, 11, 16, 24, 26, 23));
      fem.addElement(createHex(newn, 8, 1, 9, 20, 24, 17, 21, 26));
      fem.addElement(createHex(newn, 20, 9, 2, 10, 26, 21, 18, 25));
      fem.addElement(createHex(newn, 11, 20, 10, 3, 23, 26, 25, 19));
      fem.addElement(createHex(newn, 16, 24, 26, 23, 4, 12, 22, 15));
      fem.addElement(createHex(newn, 24, 17, 21, 26, 12, 5, 13, 22));
      fem.addElement(createHex(newn, 26, 21, 18, 25, 22, 13, 6, 14));
      fem.addElement(createHex(newn, 23, 26, 25, 19, 15, 22, 14, 7));
   }

   private static void subdivideWedge(
      FemModel3d fem, WedgeElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[18];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createWedge(newn, 0, 6, 7, 12, 15, 17));
      fem.addElement(createWedge(newn, 7, 8, 2, 17, 16, 14));
      fem.addElement(createWedge(newn, 6, 1, 8, 15, 13, 16));
      fem.addElement(createWedge(newn, 6, 8, 7, 15, 16, 17));
      fem.addElement(createWedge(newn, 12, 15, 17, 3, 9, 10));
      fem.addElement(createWedge(newn, 17, 16, 14, 10, 11, 5));
      fem.addElement(createWedge(newn, 15, 13, 16, 9, 4, 11));
      fem.addElement(createWedge(newn, 15, 16, 17, 9, 11, 10));
   }

   private static void subdividePyramid(
      FemModel3d fem, PyramidElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[14];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createPyramid(newn, 9, 10, 11, 12, 4));
      fem.addElement(createPyramid(newn, 12, 11, 10, 9, 13));

      fem.addElement(createPyramid(newn, 8, 0, 5, 13, 9));
      fem.addElement(createPyramid(newn, 5, 1, 6, 13, 10));
      fem.addElement(createPyramid(newn, 6, 2, 7, 13, 11));
      fem.addElement(createPyramid(newn, 7, 3, 8, 13, 12));

      fem.addElement(createTet(newn, 5, 9, 10, 13));
      fem.addElement(createTet(newn, 6, 10, 11, 13));
      fem.addElement(createTet(newn, 7, 11, 12, 13));
      fem.addElement(createTet(newn, 8, 12, 9, 13));
   }
   
   /**
    * Creates a subdvided FEM model by subdividing all the elements of an
    * existing model into eight sub-elements, adding additional nodes as
    * required. The existing model is not modified. At present, this is
    * supported only for models composed of tetrahedra, hexahedra, and wedges.
    * Markers in the original mesh are copied, but attachments (T-junction
    * connections) are not. Likewise, if the original FEM is a FemMuscleModel,
    * the muscle group information is not copied either.
    * 
    * @param femr
    * model in which refined FEM is to be constructed, or <code>null</code> if
    * the model is to be created from scratch.
    * @param fem0
    * existing FEM model to be refined.
    */
   public static FemModel3d subdivideFem (FemModel3d femr, FemModel3d fem0) {
      return subdivideFem(femr, fem0, true);
   }

   public static FemModel3d subdivideFem (
      FemModel3d femr, FemModel3d fem0, boolean addMarkers) {

      if (fem0 == null) {
         throw new IllegalArgumentException("fem0 must not be null");
      }
      if (femr == fem0) {
         throw new IllegalArgumentException("femr and fem0 must be different");
      }
      if (femr == null) {
         femr = new FemModel3d();
      } else {
         femr.clear();
      }
      for (FemElement3d e : fem0.myElements) {
         if (!(e instanceof TetElement) && !(e instanceof HexElement)
            && !(e instanceof WedgeElement) && !(e instanceof PyramidElement)) {
            throw new IllegalArgumentException(
               "fem0 must contain only test, hexs, pyramids, or wedges");
         }
      }

      int flags = CopyableComponent.COPY_REFERENCES;
      HashMap<ModelComponent,ModelComponent> copyMap =
         new HashMap<ModelComponent,ModelComponent>();
      HashMap<Edge,FemNode3d> edgeNodeMap = new HashMap<Edge,FemNode3d>();

      for (FemNode3d n : fem0.myNodes) {
         FemNode3d newn = n.copy(flags, copyMap);
         newn.setName(n.getName());
         copyMap.put(n, newn);
         femr.myNodes.add(newn);
      }

      for (FemElement3d e : fem0.myElements) {
         if (e instanceof TetElement) {
            subdivideTet(femr, (TetElement)e, edgeNodeMap, copyMap);
         } else if (e instanceof HexElement) {
            subdivideHex(femr, (HexElement)e, edgeNodeMap, copyMap);
         } else if (e instanceof WedgeElement) {
            subdivideWedge(femr, (WedgeElement)e, edgeNodeMap, copyMap);
         } else if (e instanceof PyramidElement) {
            subdividePyramid(femr, (PyramidElement)e, edgeNodeMap, copyMap);
         }
      }
      
      if (addMarkers) {
         for (FemMarker m : fem0.myMarkers) {
            FemMarker newm = new FemMarker(m.getPosition());
            newm.setName(m.getName());
            fem0.addMarker(newm);
         }
      }
      // Doesn't clone attachments yet. Should do this ...
      // for (DynamicAttachment a : fem1.myAttachments) {
      // DynamicAttachment newa = a.copy (flags, copyMap);
      // newa.setName (a.getName());
      // fem0.myAttachments.add (newa);
      // }
      return femr;
   }

   /**
    * Creates a new model by duplicating nodes and elements
    * 
    * @param out
    * model to fill
    * @param elemList
    * elements to build model from
    */
   public static void createFromElementList(
      FemModel3d out, Collection<FemElement3d> elemList) {

      HashMap<FemNode3d,FemNode3d> nodeMap = new HashMap<FemNode3d,FemNode3d>();

      for (FemElement3d elem : elemList) {
         FemNode3d[] oldNodes = elem.getNodes();
         FemNode3d[] newNodes = new FemNode3d[elem.numNodes()];
         for (int i = 0; i < newNodes.length; i++) {
            newNodes[i] = nodeMap.get(oldNodes[i]);
            if (newNodes[i] == null) {
               newNodes[i] = new FemNode3d(oldNodes[i].getPosition());
               nodeMap.put(oldNodes[i], newNodes[i]);
               out.addNode(newNodes[i]);
            }
         }

         FemElement3d newElem = FemElement3d.createElement(newNodes);
         out.addElement(newElem);
      }
   }
   
   public static void setPlanarNodesFixed (
      FemModel fem, Point3d center, Vector3d normal, boolean fixed) {

      double off = normal.dot(center);
      double tol = RenderableUtils.getRadius (fem)*1e-12;
      for (FemNode n : fem.getNodes()) {
         double d = normal.dot(n.getPosition());
         if (Math.abs (d-off) <= tol) {
            n.setDynamic (!fixed);
         }
      }
   }

}
