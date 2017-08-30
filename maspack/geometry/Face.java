/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.ArraySupport;
import maspack.util.InternalErrorException;
import maspack.util.DoubleHolder;

public class Face extends Feature implements Boundable {
   HalfEdge he0; // half edge associated with first vertex
   public int idx; // index into the face array

   private Vector3d myNormal; // face normal, allocated on demand
   //private Point3d myCentroid; // face centroid, allocated on demand
   private Vector3d myRenderNormal;

   // Flag to indicate that this face is the first triangle of a triangulated
   // quad. This enables rendering software to combine the rendering of
   // this face and the one following it to create a smoothly rendered quad.
   public static int FIRST_QUAD_TRIANGLE = 0x100;

   //private Vector3d myWorldNormal; // cached value of normal in world coords
   //public int myWorldCoordCnt = -1;

   private static double DOUBLE_PREC = 2e-16;
   
   public static final int EDGE_01 = 0x01;
   public static final int EDGE_12 = 0x02;
   public static final int EDGE_20 = 0x04;
   public static final int VERTEX_0 = (EDGE_01 | EDGE_20);
   public static final int VERTEX_1 = (EDGE_01 | EDGE_12);
   public static final int VERTEX_2 = (EDGE_12 | EDGE_20);

   /*
    * Length of edge0 cross edge1. Used for calculating barycentric coordinates
    * of a point relative to the triangle. Cached for efficiency. Must be
    * recalculated if the face is deformed.
    */
   private double referenceArea;

   /**
    * Creates an empty face with a specified index value.
    * 
    * @param idx
    * desired index value
    */
   public Face (int idx) {
      super (FACE);
      this.he0 = null;
      this.idx = idx;
   }

   /**
    * Returns the index value for this faces.
    * 
    * @return index value
    */
   public int getIndex() {
      return idx;
   }

   /**
    * Sets the index value for this face.
    * 
    * @param i new index value
    */
   public void setIndex (int i) {
      idx = i;
   }

   public boolean isTriangle () {
      return he0 == he0.next.next.next;
   }

   /** 
    * Check to see if this face is the first triangle of a triangulated quad.
    *
    * @return true if this face is the first triangle of a
    * triangulated quad.
    * @see #setFirstQuadTriangle
    */
   public boolean isFirstQuadTriangle () {
      return (myFlags & FIRST_QUAD_TRIANGLE) != 0;
   }

   /** 
    * Sets a flag indicating that this face is the first triangle of a
    * triangulated quad. This enables rendering software to combine the
    * rendering of this face and the one following it to create a smoothly
    * rendered quad.
    *
    * @param firstQuad True if this face is the first triangle of a
    * triangulated quad.
    */
   public void setFirstQuadTriangle (boolean firstQuad) {
      if (firstQuad) {
         myFlags |= FIRST_QUAD_TRIANGLE;
      }
      else {
         myFlags &= ~FIRST_QUAD_TRIANGLE;
      }
   }

   /**
    * Returns an array of the vertex indices associated with this face.
    * 
    * @return array of vertex indices
    */
   public int[] getVertexIndices() {
      int[] idxs = new int[numEdges()];
      HalfEdge he = he0;
      int k = 0;
      do {
         idxs[k++] = he.head.getIndex();
         he = he.next;
      }
      while (he != he0);
      return idxs;
   }
   
   private class VertexIterator implements Iterator<Vertex3d> {

      HalfEdge he;
      VertexIterator() {
         // dummy pointing to starting vertex
         he = new HalfEdge();  
         he.head = he0.head;
         he.next = he0.next; 
      }
      
      @Override
      public boolean hasNext() {
         return (he != he0);
      }

      @Override
      public Vertex3d next() {
         Vertex3d out = he.head;
         he = he.next;
         return out;
      }

      @Override
      public void remove() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }
   }
   
   private class EdgeIterator implements Iterator<HalfEdge> {
      HalfEdge he;
      EdgeIterator() {
         he = null;
      }
      
      @Override
      public boolean hasNext() {
         return (he != he0);
      }
      @Override
      public HalfEdge next() {
         HalfEdge out = he;
         if (he == null) {
            out = he0;
         }
         he = out.next;
         return out;
      }

      @Override
      public void remove() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }
   }
   
   /**
    * Iterator for looping over vertices
    * @return vertex iterator
    */
   public Iterator<Vertex3d> vertexIterator() {
      return new VertexIterator();
   }
   
   /**
    * Iterator for looping over edges
    * @return edge iterator
    */
   public Iterator<HalfEdge> edgeIterator() {
      return new EdgeIterator();
   }

   public Vector3d getWorldNormal() {
      MeshBase mesh = he0.head.myMesh;
      if (mesh == null || mesh.myXMeshToWorldIsIdentity) {
         return getNormal();
      }
      else {
         Vector3d wnrm = new Vector3d();
         wnrm.transform (mesh.XMeshToWorld.R, getNormal());
         return wnrm;
      }
   }

   public void getWorldNormal (Vector3d nrm) {
      nrm.set(getNormal());
      MeshBase mesh = he0.head.myMesh;
      if (mesh != null) {
         mesh.transformToWorld (nrm);
      }
   }

   // public Vector3d getWorldNormal() {
   // updateWorldCoordinates();
   // return myWorldNormal;
   // }

   public double getPoint0DotNormal() {
      return getVertex (0).getWorldPoint().dot (getWorldNormal());
   }

   //   /**
   //    * Returns the number of redundant half-edges associated with this face. A
   //    * half-edge is redundant if its opposite half-edge is connected to a
   //    * <i>different</i> half-edge. Opposite half-edges are found inspecting the
   //    * incident half-edges on the vertices associated with this face.
   //    * 
   //    * @return number of redundant half-edges.
   //    */
   //   public int numRedundantHalfEdges() {
   //      int numRedundant = 0;
   //      HalfEdge he = he0;
   //      do {
   //         HalfEdge heNext = he.next;
   //         HalfEdge oppHe = he.head.findOppositeHalfEdge (heNext.head);
   //         if (oppHe != null && oppHe.opposite != null &&
   //             oppHe.opposite != heNext) {
   //            numRedundant++;
   //         }
   //         he = heNext;
   //      }
   //      while (he != he0);
   //      return numRedundant;
   //   }

   public static Face create (Vertex3d... vtxs) {
      Face face = new Face (0);
      face.set (vtxs, vtxs.length, /* connect= */false);
      return face;
   }
   
   
   /**
    * Flips the face, disconnecting HEdges as required
    */
   public void flip(boolean connect) {
      disconnect();
      Vertex3d[] vtxs = getVertices();
      int hi = vtxs.length-1;
      for (int i=0; i<vtxs.length/2; i++) {
         Vertex3d tmp = vtxs[i];
         vtxs[i] = vtxs[hi-i];
         vtxs[hi-i] = tmp;
      }
      set(vtxs,vtxs.length, connect);
   }

   /**
    * Creates a face from a counter-clockwise list of vertices. This involves
    * creating a list of half-edges which link the vertices together.
    * 
    * <p>
    * If connect is true, then these half-edges will also be added to the list
    * of half-edges incident on each vertex, and connected, when possible, to
    * half-edges pointing in the opposite direction. Opposite half-edges are
    * found by searhing the existing half-edges which are incident on each
    * vertex. If an opposite half-edge is already connected to another
    * half-edge, then the current half-edge is considered redundant, no
    * connection is made and the method returns false. A redundant half edge
    * implies that the mesh structure containing this face is non-manifold.
    * 
    * @param vtxs
    * vertices to connect
    * @param numVtxs
    * number of vertices to connect
    * @param connect
    * connect to opposite half-edges
    * @return false if this face contains redundant half edges.
    */
   public boolean set (Vertex3d[] vtxs, int numVtxs, boolean connect) {
      boolean noRedundantHalfEdges = true;
      Vertex3d vtxPrev = vtxs[numVtxs - 1];
      HalfEdge prevHe = null;
      HalfEdge he = null;

      for (int i = 0; i < numVtxs; i++) {
         prevHe = he;
         HalfEdge oppHe = null;
         Vertex3d vtx = vtxs[i];
         if (connect) {
            oppHe = vtxPrev.findOppositeHalfEdge (vtx);
         }
         if (oppHe == null) {
            he = new HalfEdge (vtx, vtxPrev, this);
         }
         else if (oppHe.opposite != null) {
            noRedundantHalfEdges = false;
            he = new HalfEdge (vtx, vtxPrev, this);
         }
         else {
            he = new HalfEdge (vtx, oppHe, this);
         }
         if (prevHe != null) {
            prevHe.setNext (he);
         }
         else {
            he0 = he;
         }
         if (connect) {
            he.head.addIncidentHalfEdge (he);
         }
         vtxPrev = vtx;
      }
      he.setNext (he0);

      clearNormal();
      //clearCentroid();
      return noRedundantHalfEdges;
   }

   /**
    * Disconnects this face from any mesh to which it is attached, and returns
    * the number of half edges which are redundant.
    * 
    * @return number of redundant half-edges
    */
   void disconnect() {
      HalfEdge he = he0;
      do {
         HalfEdge heOpp = he.opposite;
         if (heOpp != null) {
            heOpp.opposite = null;
            if (heOpp.isHard()) {
               // single edges not allowed to be hard
               heOpp.setHard(false);
            }
            if (heOpp.uOppositeP) {
               // move directon vector over
               //heOpp.u.negate();
               heOpp.uOppositeP = false; // makes heOpp primary
            }
         }
         he.head.removeIncidentHalfEdge (he);
         he = he.next;
      }
      while (he != he0);
   }

   // /**
   //  * Disconnects this face from any mesh to which it is attached, and returns
   //  * the number of half edges which are redundant.
   //  * 
   //  * @return number of redundant half-edges
   //  */
   // int oldDisconnect() {
   //    int numRedundant = 0;
   //    HalfEdge he = he0;
   //    do {
   //       HalfEdge heNext = he.next;
   //       HalfEdge oppHe = he.head.findOppositeHalfEdge (heNext.head);
   //       if (oppHe != null && oppHe.opposite != null) {
   //          if (oppHe.opposite != heNext) {
   //             numRedundant++;
   //          }
   //          else {
   //             oppHe.opposite = null;
   //             heNext.opposite = null;
   //          }
   //       }
   //       he.head.removeIncidentHalfEdge (he);
   //       he = heNext;
   //    }
   //    while (he != he0);
   //    return numRedundant;
   // }

   /**
    * Looks for a half edge, with the given tail and head, contained within this
    * face.
    */
   HalfEdge findHalfEdge (Vertex3d tail, Vertex3d head) {
      HalfEdge he = he0;
      do {
         HalfEdge heNext = he.next;
         if (he.head == tail && heNext.head == head) {
            return heNext;
         }
         he = heNext;
      }
      while (he != he0);
      return null;
   }


   /**
    * Computes centroid of this face.
    * 
    * @param centroid
    * returns the centroid
    */
   public void computeCentroid (Vector3d centroid) {
      // Hey! This wasn't the centroid of the face.
      // It was the center of mass (or centroid) of the vertices ...
      // int nverts = 1;
      // HalfEdge he = he0;
      // centroid.set (he.head.pnt);
      // he = he.next;
      // while (he != he0)
      // { centroid.add (he.head.pnt);
      // he = he.next;
      // nverts++;
      // }
      // centroid.scale (1/(double)nverts);

      // for now, compute a "poor man's" centroid, weighted by the edge
      // lengths instead of triangle areas

      // double length = 0;
      // centroid.setZero();
      // HalfEdge ha = he0;
      // do {
      //    HalfEdge hb = ha.next;
      //    Point3d pa = ha.head.pnt;
      //    Point3d pb = hb.head.pnt;
      //    // compute edge length
      //    double ux = pb.x - pa.x;
      //    double uy = pb.y - pa.y;
      //    double uz = pb.z - pa.z;
      //    double l = Math.sqrt (ux * ux + uy * uy + uz * uz);
      //    centroid.scaledAdd (l, pa);
      //    centroid.scaledAdd (l, pb);
      //    length += l;
      //    ha = hb;
      // }
      // while (ha != he0);
      // centroid.scale (1 / (2 * length));

      HalfEdge he = he0;
      Point3d p0 = he.head.pnt;
      he = he.next;
      Point3d p1 = he.head.pnt;
      he = he.next;
      Point3d p2 = he.head.pnt;

      if (he.next == he0) {
         // triangle; calculation is easy
         centroid.add (p0, p1);
         centroid.add (p2);
         centroid.scale (1/3.0);
      }
      else {
         centroid.setZero();
         double area = 0;

         double d2x = p1.x - p0.x;
         double d2y = p1.y - p0.y;
         double d2z = p1.z - p0.z;

         do {
            double d1x = d2x;
            double d1y = d2y;
            double d1z = d2z;
            d2x = p2.x - p0.x;
            d2y = p2.y - p0.y;
            d2z = p2.z - p0.z;
            double nx = d1y * d2z - d1z * d2y;
            double ny = d1z * d2x - d1x * d2z;
            double nz = d1x * d2y - d1y * d2x;
            double a = Math.sqrt (nx*nx + ny*ny + nz*nz)/2;
            area += a;
            a /= 3;

            centroid.scaledAdd (a, p0);
            centroid.scaledAdd (a, p1);
            centroid.scaledAdd (a, p2);

            p1 = p2;
            he = he.next;
            p2 = he.head.pnt;
         }
         while (he != he0);
         centroid.scale (1/area);
      }
   }
   
   public void computeWorldCentroid(Point3d pnt) {
      computeCentroid(pnt);
      RigidTransform3d trans = getMesh().getMeshToWorld();
      if (trans != RigidTransform3d.IDENTITY ) {
         pnt.transform(trans);
      }
   }

   /**
    * Computes covariance of this face and returns its area. This is done by
    * subdividing the face into a triangular fan centered on the first vertex,
    * and adding the covariance and area for all the triangles.
    * 
    * @param C 
    * returns the covariance
    * @return area of the face
    */
   public double computeCovariance (Matrix3d C) {

      double area = 0;
      C.setZero();

      HalfEdge he = he0;
      Point3d p0 = he.head.pnt;
      he = he.next;
      Point3d p1 = he.head.pnt;
      he = he.next;
      Point3d p2 = he.head.pnt;

      // double d2x = p1.x - p0.x;
      // double d2y = p1.y - p0.y;
      // double d2z = p1.z - p0.z;

      do {
         double a = CovarianceUtils.addTriangleCovariance (C, p0, p1, p2);
         area += a;

         // // compute and add triangle area
         // double d1x = d2x;
         // double d1y = d2y;
         // double d1z = d2z;
         // d2x = p2.x - p0.x;
         // d2y = p2.y - p0.y;
         // d2z = p2.z - p0.z;
         // double nx = d1y * d2z - d1z * d2y;
         // double ny = d1z * d2x - d1x * d2z;
         // double nz = d1x * d2y - d1y * d2x;
         // double a = Math.sqrt (nx*nx + ny*ny + nz*nz)/2;
         // area += a;

         // // compute and add covariance for triangle
         // double pcx = (p0.x + p1.x + p2.x) / 3;
         // double pcy = (p0.y + p1.y + p2.y) / 3;
         // double pcz = (p0.z + p1.z + p2.z) / 3;

         // C.m00 += a * (9*pcx*pcx + p0.x*p0.x + p1.x*p1.x + p2.x*p2.x);
         // C.m11 += a * (9*pcy*pcy + p0.y*p0.y + p1.y*p1.y + p2.y*p2.y);
         // C.m22 += a * (9*pcz*pcz + p0.z*p0.z + p1.z*p1.z + p2.z*p2.z);

         // C.m01 += a * (9*pcx*pcy + p0.x*p0.y + p1.x*p1.y + p2.x*p2.y);
         // C.m02 += a * (9*pcx*pcz + p0.x*p0.z + p1.x*p1.z + p2.x*p2.z);
         // C.m12 += a * (9*pcy*pcz + p0.y*p0.z + p1.y*p1.z + p2.y*p2.z);

         p1 = p2;
         he = he.next;
         p2 = he.head.pnt;
      }
      while (he != he0);
      C.scale (1 / (12.0));

      // C is symmetric, so set symmetric components
      C.m10 = C.m01;
      C.m20 = C.m02;
      C.m21 = C.m12;

      return area;
   }

   /**
    * Computes a point on this face as described by barycentric
    * coordinates. Specifically, if p0, p1 and p2 are the points associated
    * with the first three vertices of this face, and s1 and s2 are
    * the x and y values of <code>coords</code>, then the
    * point is computed from
    * <pre>
    * pnt = (1-s1-s2)*p0 + s1*p1 + s2*p2
    * </pre>
    * This method is most often used for triangular faces, but that
    * does not have to be the case.
    * @param pnt returns the computed point
    * @param coords specifies s0 and s0
    */
   public void computePoint (Point3d pnt, Vector2d coords) {
      double s1 = coords.x;
      double s2 = coords.y;

      HalfEdge he = he0;
      pnt.scale (1-s1-s2, he.head.pnt);
      he = he.next;
      pnt.scaledAdd (s1, he.head.pnt);
      he = he.next;
      pnt.scaledAdd (s2, he.head.pnt);
   }

   /**
    * Computes the barycentric coordinates of a point to the plane
    * @param pnt the point to consider
    * @param coords the returned coordinates
    */
   public void computeCoords(Point3d pnt, Vector2d coords) {
      Vector3d v0 = new Vector3d();
      Vector3d v1 = new Vector3d();
      Vector3d v2 = new Vector3d();

      Point3d p0, p1, p2;
      HalfEdge he = he0;
      p0 = he.head.pnt;
      he = he.next;
      p1 = he.head.pnt;
      he = he.next;
      p2 = he.head.pnt;
      v0.sub(p1, p0);
      v1.sub(p2, p0);
      v2.sub(pnt, p0);

      double d00 = v0.dot(v0);
      double d01 = v0.dot(v1);
      double d11 = v1.dot(v1);
      double d20 = v2.dot(v0);
      double d21 = v2.dot(v1);
      double denom = d00 * d11 - d01 * d01;

      coords.x = (d11 * d20 - d01 * d21) / denom;
      coords.y = (d00 * d21 - d01 * d20) / denom;
      
   }

   //   public void computeWorldCentroid (Point3d centroid) {
   //      double length = 0;
   //      centroid.setZero();
   //      HalfEdge ha = he0;
   //      do {
   //         HalfEdge hb = ha.next;
   //         Point3d pa = ha.head.getWorldPoint();
   //         Point3d pb = hb.head.getWorldPoint();
   //         // compute edge length
   //         double ux = pb.x - pa.x;
   //         double uy = pb.y - pa.y;
   //         double uz = pb.z - pa.z;
   //         double l = Math.sqrt (ux * ux + uy * uy + uz * uz);
   //         centroid.scaledAdd (l, pa);
   //         centroid.scaledAdd (l, pb);
   //         length += l;
   //         ha = hb;
   //      }
   //      while (ha != he0);
   //      centroid.scale (1 / (2 * length));
   //   }

   /**
    * Computes the normal for this face.
    */
   public void computeNormal() {
      if (myNormal == null) {
         myNormal = new Vector3d();
      }
      computeNormal (myNormal);
   }

   /**
    * Computes the normal for this face.
    * 
    * @param normal
    * returns the normal
    * @see #getNormal
    */
   public void computeNormal (Vector3d normal) {
      Vertex3d v0 = he0.head;
      Vertex3d v2 = he0.next.head;

      double d2x = v2.pnt.x - v0.pnt.x;
      double d2y = v2.pnt.y - v0.pnt.y;
      double d2z = v2.pnt.z - v0.pnt.z;

      normal.setZero();

      HalfEdge he = he0.next.next;
      do {
         double d1x = d2x;
         double d1y = d2y;
         double d1z = d2z;

         v2 = he.head;

         d2x = v2.pnt.x - v0.pnt.x;
         d2y = v2.pnt.y - v0.pnt.y;
         d2z = v2.pnt.z - v0.pnt.z;

         normal.x += d1y * d2z - d1z * d2y;
         normal.y += d1z * d2x - d1x * d2z;
         normal.z += d1x * d2y - d1y * d2x;

         he = he.next;
      }
      while (he != he0);

      referenceArea = normal.norm();
      normal.scale (1 / referenceArea);
      // John Lloyd, Jul 29, 2013: seems to be old debugging code
      // if (v0.myMesh.myXMeshToWorldIsIdentity) {
      //    Point3d tst = new Point3d();
      //    tst.sub (v0.pnt, v0.getWorldPoint());
      //    double tstn = tst.norm();
      //    if (tstn > 1e-8)
      //       throw new InternalErrorException ("bad world point" + tstn);
      // }
   }

   /**
    * Computes the area of this face.
    */
   public double computeArea () {
      double area = 0;

      Vertex3d v0 = he0.head;
      Vertex3d v1 = he0.next.head;

      double d2x = v1.pnt.x - v0.pnt.x;
      double d2y = v1.pnt.y - v0.pnt.y;
      double d2z = v1.pnt.z - v0.pnt.z;

      HalfEdge he = he0.next.next;
      do {
         Vertex3d v2 = he.head;

         double d1x = d2x;
         double d1y = d2y;
         double d1z = d2z;

         d2x = v2.pnt.x - v0.pnt.x;
         d2y = v2.pnt.y - v0.pnt.y;
         d2z = v2.pnt.z - v0.pnt.z;

         double nx = d1y * d2z - d1z * d2y;
         double ny = d1z * d2x - d1x * d2z;
         double nz = d1x * d2y - d1y * d2x;

         area += Math.sqrt (nx*nx + ny*ny + nz*nz);
         he = he.next;
      }
      while (he != he0);

      area /= 2;
      return area;
   }

   /**
    * Computes the circumference of this face.
    */
   public double computeCircumference() {
      double circ = 0;
      HalfEdge he0 = firstHalfEdge();
      HalfEdge he = he0;
      do {
         circ += he.length();
         he = he.next;
      }
      while (he != he0);
      return circ;
   }

   public Vector3d getRenderNormal() {
      return myRenderNormal;
   }

   public void computeRenderNormal() {
      if (myRenderNormal == null) {
         myRenderNormal = new Vector3d();
      }
      Vertex3d v0 = he0.head;
      Vertex3d v2 = he0.next.head;

      double d2x = v2.myRenderPnt.x - v0.myRenderPnt.x;
      double d2y = v2.myRenderPnt.y - v0.myRenderPnt.y;
      double d2z = v2.myRenderPnt.z - v0.myRenderPnt.z;

      myRenderNormal.setZero();

      HalfEdge he = he0.next.next;
      do {
         double d1x = d2x;
         double d1y = d2y;
         double d1z = d2z;

         v2 = he.head;

         d2x = v2.myRenderPnt.x - v0.myRenderPnt.x;
         d2y = v2.myRenderPnt.y - v0.myRenderPnt.y;
         d2z = v2.myRenderPnt.z - v0.myRenderPnt.z;

         myRenderNormal.x += d1y * d2z - d1z * d2y;
         myRenderNormal.y += d1z * d2x - d1x * d2z;
         myRenderNormal.z += d1x * d2y - d1y * d2x;

         he = he.next;
      }
      while (he != he0);

      myRenderNormal.normalize();
   }

   // void flipEdgeDirs()
   // {
   // HalfEdge he = he0;
   // do
   // { he.uLength *= -1;
   // he.u.negate();
   // he = he.next;
   // }
   // while (he != he0);
   // }

   // /**
   // * Computes the distance from this face to a vertex.
   // * Associated information, such as the closest point
   // * on the face or the nearest features, is returned
   // * in a supplied distance record.
   // *
   // * @param rec returns associated distance information
   // * @param vtx vertex to compute closest point to
   // * @return distance from the vertex to the plane
   // */
   // public double distance (DistanceRecord rec, Vertex3d vtx)
   // {
   // HalfEdge he = he0;
   // Vector3d dv = rec.pnt0; // use rec.pnt0 as scratch space
   // Point3d p1 = vtx.pnt;
   // Vector3d nrml = getNormal();
   // do
   // { HalfEdge heNext = he.next;
   // dv.sub (p1, he.head.pnt);
   //
   // double dotNext = heNext.dot(dv);
   // double dotPrev = he.dot(dv);
   //
   // if (dotNext <= 0 && dotPrev >= 0)
   // { // then the closest point is he.head
   // rec.setFeatures (he.head, vtx);
   // rec.setPoints (he.head.pnt, p1);
   // rec.computeDistanceAndNormal();
   // return rec.dist;
   // }
   // else if (dotNext > 0 && dotNext < heNext.length() &&
   // heNext.sideProduct (dv, nrml) >= 0)
   // { // then the closest point is on the edge heNext
   // rec.setFeatures (heNext, vtx);
   // heNext.extrapolate (rec.pnt0, dotNext, he.head.pnt);
   // rec.pnt1.set (p1);
   // rec.computeDistanceAndNormal();
   // return rec.dist;
   // }
   // he = heNext;
   // }
   // while (he != he0);
   //
   // // the closest point is on the face
   // dv.sub (p1, he0.head.pnt);
   // double d = dv.dot(nrml);
   // rec.pnt0.scaledAdd (-d, nrml, p1);
   // rec.pnt1.set (p1);
   // if (d >= 0)
   // { rec.dist = d;
   // rec.nrml.set (nrml);
   // }
   // else
   // { rec.dist = -d;
   // rec.nrml.negate (nrml);
   // }
   // rec.setFeatures (this, vtx);
   // return rec.dist;
   // }

   /**
    * Computes the closest point on this face to a specified point.
    * 
    * @param pc
    * returns the closest point
    * @param p1
    * point for which closest point is computed
    */
   public void nearestPoint (Point3d pc, Point3d p1) {
      if (isTriangle()) {
         nearestPointTriangle (pc, p1);
      }
      else {
         nearestPointFace (pc, p1);
      }
   }

   public int nearestPointTriangle (Point3d pn, Point3d p1) {
      //long time = System.nanoTime();
      Point3d pa = he0.head.pnt;
      Point3d pb = he0.next.head.pnt;
      Point3d pc = he0.tail.pnt;
      return nearestPointTriangle (pn, pa, pb, pc, p1);
   }

   public int nearestWorldPointTriangle (Point3d pn, Point3d p1) {
      //long time = System.nanoTime();
      Point3d pa = new Point3d();
      Point3d pb = new Point3d();
      Point3d pc = new Point3d();
      he0.head.getWorldPoint (pa);
      he0.next.head.getWorldPoint (pb);
      he0.tail.getWorldPoint (pc);
      return nearestPointTriangle (pn, pa, pb, pc, p1);
   }

   private int nearestPointTriangle (
      Point3d pn, Point3d pa, Point3d pb, Point3d pc, Point3d p1) {
      //long time = System.nanoTime();

      double abx = pb.x - pa.x;// b-a
      double aby = pb.y - pa.y;
      double abz = pb.z - pa.z;
      double acx = pc.x - pa.x;// c-a
      double acy = pc.y - pa.y;
      double acz = pc.z - pa.z;
      double apx = p1.x - pa.x;// p-a
      double apy = p1.y - pa.y;
      double apz = p1.z - pa.z;

      // Check if P in vertex region outside A
      double d1 = abx*apx + aby*apy + abz*apz; //d1 = ab.dot (ap);
      double d2 = acx*apx + acy*apy + acz*apz; //d2 = ac.dot (ap);
      if (d1 <= 0.0f && d2 <= 0.0f) {
         pn.x = pa.x;// closest = a
         pn.y = pa.y;
         pn.z = pa.z;
         //time = System.nanoTime() - time;
         return VERTEX_0;
      }

      // Check if P in vertex region outside B
      double bpx = p1.x - pb.x;// p-b
      double bpy = p1.y - pb.y;
      double bpz = p1.z - pb.z;
      double d3 = abx*bpx + aby*bpy + abz*bpz;// ab.bp;
      double d4 = acx*bpx + acy*bpy + acz*bpz;// ac.bp;
      if (d3 >= 0.0f && d4 <= d3) {
         pn.x = pb.x;// closest = b
         pn.y = pb.y;
         pn.z = pb.z;
         //         time = System.nanoTime() - time;
         return VERTEX_1;
      }

      // Check if P in edge region of AB, if so return projection of P onto AB
      double vc = d1*d4 - d3*d2;
      if (vc <= 0.0f && d1 >= 0.0f && d3 <= 0.0f) {
         double v = d1 / (d1 - d3);
         pn.x = abx*v + pa.x;// closest = (b-a)*v + a
         pn.y = aby*v + pa.y;
         pn.z = abz*v + pa.z;
         //         time = System.nanoTime() - time;
         return EDGE_01;
      }

      // Check if P in vertex region outside C
      double cpx = p1.x - pc.x;// p-c
      double cpy = p1.y - pc.y;
      double cpz = p1.z - pc.z;
      double d5 = abx*cpx + aby*cpy + abz*cpz;// ab.cp;
      double d6 = acx*cpx + acy*cpy + acz*cpz;// ac.cp;
      if (d6 >= 0.0f && d5 <= d6) {
         pn.x = pc.x;// closest = c
         pn.y = pc.y;
         pn.z = pc.z;
         //         time = System.nanoTime() - time;
         return VERTEX_2;
      }

      // Check if P in edge region of AC, if so return projection of P onto AC
      double vb = d5*d2 - d1*d6;
      if (vb <= 0.0f && d2 >= 0.0f && d6 <= 0.0f) {
         double w = d2 / (d2 - d6);
         pn.x = acx*w + pa.x;// closest = (c-a)*w + a;
         pn.y = acy*w + pa.y;
         pn.z = acz*w + pa.z;
         //         time = System.nanoTime() - time;
         return EDGE_20;
      }

      // Check if P in edge region of BC, if so return projection of P onto BC
      double va = d3*d6 - d5*d4;
      if (va <= 0.0f && (d4 - d3) >= 0.0f && (d5 - d6) >= 0.0f) {
         double w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
         // closest = (c - b)*w + b;
         pn.x = (pc.x - pb.x)*w + pb.x;
         pn.y = (pc.y - pb.y)*w + pb.y;
         pn.z = (pc.z - pb.z)*w + pb.z;
         //         time = System.nanoTime() - time;
         return EDGE_12;
      }

      // P inside face region. Compute Q through its barycentric coordinates (u,v,w)
      double denom = 1.0f / (va + vb + vc);
      double v = vb * denom;
      double w = vc * denom;
      // closest = (c-a)*w (b-a)*v + a;
      pn.x = acx*w + abx*v + pa.x;
      pn.y = acy*w + aby*v + pa.y;
      pn.z = acz*w + abz*v + pa.z;
      //      time = System.nanoTime() - time;
      return 0;
   }

   private void nearestPointFace (Point3d pc, Point3d p1) {
      //long time = System.nanoTime();
      HalfEdge he = he0;
      Vector3d dv = new Vector3d();
      if (myNormal == null) {
         myNormal = new Vector3d();
      }
      computeNormal (myNormal);
      do {
         HalfEdge heNext = he.next;
         dv.sub (p1, he.head.pnt);
         double dotNext = heNext.dotDirection (dv); // /
         double dotPrev = he.dotDirection (dv); // /

         if (dotNext <= 0 && dotPrev >= 0) {
            // then the closest point is he.head
            pc.set (he.head.pnt);
            //            time = System.nanoTime() - time;
            return;// time;
         }
         double lenNextSqr = heNext.lengthSquared();
         if (dotNext > 0 && dotNext < lenNextSqr &&
            heNext.sideProductDirection (dv, myNormal) >= 0) {
            // then the closest point is on the edge heNext
            dv.sub (heNext.head.pnt, heNext.tail.pnt);
            pc.scaledAdd (dotNext / lenNextSqr, dv, heNext.tail.pnt);
            //time = System.nanoTime() - time;
            return;// time;
         }
         he = heNext;
      }
      while (he != he0);

      // the closest point is on the face
      dv.sub (p1, he0.head.pnt);
      double d = dv.dot (myNormal);
      pc.scaledAdd (-d, myNormal, p1);
      //time = System.nanoTime() - time;
      return;// time;
   }

   // /**
   // * Tests to see if the head of the half edge indicated by seg is
   // * closest to either the head or the edge of the half-edge he.
   // */
   // private boolean testHalfEdgeToVertex (
   // DistanceRecord drec, HalfEdge he, HalfEdge seg)
   // {
   // Vector3d dvh = drec.pnt0; // use drec.pnt0 as scratch space
   // Vector3d tmp = drec.pnt1; // use drec.pnt1 as scratch space
   // Vector3d nrml = getNormal();
   // HalfEdge heNext = he.next;
   //
   // dvh.sub (seg.head.pnt, he.head.pnt);
   //
   // double dotNext = heNext.dot(dvh);
   // double dotPrev = he.dot(dvh);
   //
   // if (dotNext <= 0 && dotPrev >= 0 && seg.dot (dvh) <= 0)
   // { // then he.head is in Voronoi(seg.head) and
   // // seg.head is in Voronoi(he.head)
   // drec.setFeatures (he.head, seg.head);
   // drec.setPoints (he.head.pnt, seg.head.pnt);
   // drec.computeDistanceAndNormal();
   // return true;
   // }
   // else if (dotPrev < 0 && dotPrev > -he.length() &&
   // he.sideProduct (dvh, nrml) >= 0)
   // { // then he.head is in Voronoi(seg)
   // he.extrapolate (tmp, -dotPrev, dvh);
   // if (seg.dot (tmp) <= 0)
   // { // the seg is in Voronoi(he.head)
   // drec.setFeatures (he, seg.head);
   // he.extrapolate (drec.pnt0, dotPrev);
   // drec.pnt1.set (seg.head.pnt);
   // drec.computeDistanceAndNormal();
   // return true;
   // }
   // }
   // return false;
   // }

   /**
    * Computes the (signed) distance of a point from the plane corresponding to
    * this face.
    * 
    * @param pnt
    * point
    * @return signed distance
    */
   double distanceToPlane (Point3d pnt) {
      Point3d head = he0.head.pnt;
      Vector3d nrml = getNormal();

      double x = pnt.x - head.x;
      double y = pnt.y - head.y;
      double z = pnt.z - head.z;

      return nrml.x * x + nrml.y * y + nrml.z * z;
   }

   // /**
   // * Tests to see if the edge of seg is closest to either the head or the
   // * edge of he.
   // */
   // private boolean testHalfEdgeToHalfEdge (
   // DistanceRecord drec, HalfEdge he, HalfEdge seg)
   // {
   // Vector3d dvh = drec.pnt0; // use drec.pnt0 as scratch space
   // Vector3d tmp = drec.pnt1; // use drec.pnt1 as scratch space
   // HalfEdge heNext = he.next;
   // Vector3d nrml = getNormal();
   //
   // dvh.sub (seg.head.pnt, he.head.pnt);
   //         
   // double dvhU1 = seg.dot(dvh);
   // if (dvhU1 >= 0 && dvhU1 <= seg.length())
   // { // then he.head is in Voronoi(seg)
   // seg.extrapolate (tmp, -dvhU1, dvh);
   // if (heNext.dot(tmp) <= 0 && he.dot(tmp) >= 0)
   // { // tmp is in Voronoi(he.head)
   // drec.setFeatures (he.head, seg);
   // drec.pnt0.set (he.head.pnt);
   // seg.extrapolate (drec.pnt1, -dvhU1);
   // drec.computeDistanceAndNormal();
   // return true;
   // }
   // }
   // // test edge-edge
   // if (he.lineDistance (drec, seg, /*forceOntoSegment=*/false))
   // { // just need to check that drec.pnt0 is outside the edge
   // if (!he.isInside (drec.pnt1, nrml))
   // { return true;
   // }
   // }
   // return false;
   // }

   // public boolean culledDistance (
   // DistanceRecord drec, HalfEdge seg, double dcull)
   // {
   // distance (drec, seg);
   // if (dcull > 0 && drec.dist > dcull)
   // { return false;
   // }
   // if (drec.feature0 != null)
   // { if (!drec.feature0.voronoiCheck (drec.pnt1))
   // { return false;
   // }
   // }
   // return true;
   // }

   // public boolean culledDistance (
   // DistanceRecord drec, Vertex3d vtx, double dcull)
   // {
   // distance (drec, vtx);
   // if (dcull > 0 && drec.dist > dcull)
   // { return false;
   // }
   // if (drec.feature0 != null)
   // { if (!drec.feature0.voronoiCheck (drec.pnt1))
   // { return false;
   // }
   // }
   // return true;
   // }

   // /**
   // * Compute the nearest points between this face and a line segment
   // * represented by the HalfEdge seg, and returns the information in a
   // * distance record.
   // * seg must point to two opposite half-edges, linked
   // * by their next fields.
   // *
   // * @param drec returns nearest point information
   // * @param seg represents the line segment
   // * @return distance between the face and the line segment
   // */
   // public double distance (DistanceRecord drec, HalfEdge seg)
   // {
   // HalfEdge he = he0;
   // Vertex3d headVtx = seg.head;
   // Vertex3d tailVtx = seg.next.head;
   // Vector3d tmp = drec.pnt0; // use pnt0 as scratch space
   // Vector3d nrml = getNormal();
   //
   // boolean headInside = true;
   // boolean tailInside = true;
   //
   // do
   // { // test if the head or tail of the line segment is
   // // closest to the edge or head of the half edge he.
   // if (testHalfEdgeToVertex (drec, he, seg) ||
   // testHalfEdgeToVertex (drec, he, seg.next))
   // { return drec.dist;
   // }
   // // test if the edge of the line segment is closest
   // // to the edge or head of the half edge he.
   // if (testHalfEdgeToHalfEdge (drec, he, seg))
   // { return drec.dist;
   // }
   //
   // if (headInside)
   // { if (!he.isInside (headVtx.pnt, nrml))
   // { headInside = false;
   // }
   // }
   // if (tailInside)
   // { if (!he.isInside (tailVtx.pnt, nrml))
   // { tailInside = false;
   // }
   // }
   // he = he.next;
   // }
   // while (he != he0);
   //
   // double dh = distanceToPlane (headVtx.pnt);
   // double dt = distanceToPlane (tailVtx.pnt);
   //         
   // if (headInside)
   // { tmp.scale (dh, nrml);
   // if (seg.dot (tmp) <= 0)
   // { drec.pnt0.sub (headVtx.pnt, tmp);
   // drec.pnt1.set (headVtx.pnt);
   // drec.nrml.scale (dh < 0 ? -1 : 1, nrml);
   // drec.setFeatures (this, headVtx);
   // drec.computeDistance();
   // return drec.dist;
   // }
   // }
   //
   // if (tailInside)
   // { tmp.scale (dt, nrml);
   // if (seg.dot (tmp) >= 0)
   // { drec.pnt0.sub (tailVtx.pnt, tmp);
   // drec.pnt1.set (tailVtx.pnt);
   // drec.nrml.scale (dt < 0 ? -1 : 1, nrml);
   // drec.setFeatures (this, tailVtx);
   // drec.computeDistance();
   // return drec.dist;
   // }
   // }
   //
   // // the only remaining possibilities are face-edge contact
   // // and interection. Face-edge contact should actually
   // // have appeared earlier as a lower-DOF contact condition,
   // // so we will not test for this now. Instead, we will assume
   // // intersection
   //
   // drec.setFeatures (null, null);
   // drec.dist = 0;
   // if (dh == 0)
   // { drec.setPoints (headVtx.pnt, headVtx.pnt);
   // }
   // else if (dt == 0)
   // { drec.setPoints (tailVtx.pnt, tailVtx.pnt);
   // }
   // else if (dh*dt < 0)
   // { drec.pnt0.interpolate (tailVtx.pnt,
   // Math.abs(dh/(dh-dt)), headVtx.pnt);
   // drec.pnt1.set (drec.pnt0);
   // }
   // else
   // { // ??? Oh well!
   // }
   // return drec.dist;
   // }

   /**
    * Returns a normal vector for this face. The normal vector is allocated
    * on-demand and computed, upon initialization, using {@link #computeNormal
    * computeNormal}. In order to have the normal vector recomputed, one should
    * first clear it using {@link #clearNormal clearNormal}.
    * 
    * @return normal vector
    */
   public Vector3d getNormal() {
      if (myNormal == null) {
         myNormal = new Vector3d();
         computeNormal (myNormal);
      }
      return myNormal;
   }

   /**
    * Clears the normal vector for this face. A subsequent call to {@link
    * #getNormal getNormal} will cause the normal vector to be reallocated and
    * recomputed.
    */
   public void clearNormal() {
      myNormal = null;
   }


   //   public Point3d getCentroid() {
   //      if (myCentroid == null) {
   //         myCentroid = new Point3d();
   //         computeCentroid (myCentroid);
   //      }
   //      return myCentroid;
   //   }
   //
   //   /**
   //    * Clears the centroid for this face. A subsequent call to {@link
   //    * #getCentroid getCentroid} will cause the centroid to be recomputed.
   //    */
   //   public void clearCentroid() {
   //      myCentroid = null;
   //   }

   /**
    * Returns the number of edges associated with the face.
    * 
    * @return number of edges
    */
   public int numEdges() {
      int num = 0;
      HalfEdge he = he0;
      do {
         num++;
         he = he.next;
      }
      while (he != he0);
      return num;
   }

   /**
    * Returns the HalfEdge previous to a given half edge in this face.
    *
    */
   HalfEdge getPreviousEdge (HalfEdge he) {
      HalfEdge heNext = he.next;
      while (heNext.next != he) {
         heNext = heNext.next;
      }
      return heNext;
   }   

   /**
    * Returns a specific edge associated with this face, or null is the edge
    * does not exist.
    * 
    * @param idx
    * index of the desired edge
    * @return a specific edge
    */
   public HalfEdge getEdge (int idx) {
      if (idx == 0) {
         return he0;
      }
      else if (idx > 0) {
         int i = 0;
         HalfEdge he = he0;
         do {
            he = he.next;
            i++;
         }
         while (he != he0 && i < idx);
         if (he != he0) {
            return he;
         }
      }
      return null;
   }

   /**
    * Returns the index of a specified HalfEdge, or -1 if the half edge
    * does not belong to this face.
    */
   public int indexOfEdge (HalfEdge halfEdge) {
      int idx = 0;
      HalfEdge he = he0;
      do {
         if (he == halfEdge) {
            return idx;
         }
         idx++;
         he = he.next;
      }
      while (he != he0);
      return -1;
   }

   /**
    * Returns the first half-edge associated with this face. Starting with this
    * half-edge, an application can find sucessive half-edges, by following
    * their next pointers, which are arranged in a circular linked link that
    * runs around the face in counter-clockwise order.
    * 
    * @return first half edge
    */
   public HalfEdge firstHalfEdge() {
      return he0;
   }

   // public void addPoints (IndexedPointSet set)
   // {
   // HalfEdge he = he0;
   // do
   // { Vertex3d vtx = he.head;
   // set.add (vtx.pnt, vtx.idx);
   // he = he.next;
   // }
   // while (he != he0);
   // }

   // public int addPoints (Point3d[] points, int indices[], int off)
   // {
   // HalfEdge he = he0;
   // int idx = off;
   // if (indices != null)
   // { do
   // { Vertex3d vtx = he.head;
   // points[idx] = vtx.pnt;
   // indices[idx++] = vtx.idx;
   // he = he.next;
   // }
   // while (he != he0);
   // }
   // else
   // { do
   // { Vertex3d vtx = he.head;
   // points[idx++] = vtx.pnt;
   // he = he.next;
   // }
   // while (he != he0);
   // }
   // return idx;
   // }

   public void updateBounds (Vector3d min, Vector3d max) {
      HalfEdge he = he0;
      do {
         Vertex3d vtx = he.head;
         vtx.pnt.updateBounds (min, max);
         he = he.next;
      }
      while (he != he0);
   }

   public int numVertices() {
      int num = 0;
      HalfEdge he = he0;
      do {
         he = he.next;
         num++;
      }
      while (he != he0);
      return num;
   }

   public PolygonalMesh getMesh() {
      return (PolygonalMesh)he0.head.getMesh();
   }

   public Vertex3d[] getVertices() {
      Vertex3d[] vtxs = new Vertex3d[numVertices()];
      HalfEdge he = he0;
      int num = 0;
      do {
         vtxs[num] = he.head;
         he = he.next;
         num++;
      } while (he != he0);
      return vtxs;
   }

   /**
    * Returns the vertices of this Face, using a priori knowledge that the
    * the face is a triangle and therefore has three vertices.
    */
   public Vertex3d[] getTriVertices() {
      Vertex3d[] vtxs = new Vertex3d[3];
      HalfEdge he = he0;
      vtxs[0] = he.head;
      he = he.next;
      vtxs[1] = he.head;
      he = he.next;
      vtxs[2] = he.head;
      return vtxs;
   }

   public Vertex3d getVertex (int idx) {
      int num = 0;
      HalfEdge he = he0;
      do {
         if (num == idx) {
            return he.head;
         }
         he = he.next;
         num++;
      }
      while (he != he0);
      throw new IllegalArgumentException ("index " + idx + " out of bounds");
   }

   /**
    * Returns the vertex associated with a specified vertex, or -1
    * if the vertex is not found in this face.
    **/
   public int indexOfVertex (Vertex3d vtx) {
      int idx = 0;
      HalfEdge he = he0;
      do {
         if (vtx == he.head) {
            return idx;
         }
         he = he.next;
         idx++;
      }
      while (he != he0);
      return -1;
   }

   /**
    * Called to update normal and edge data when vertices have been transformed
    */
   public void updateNormalAndEdges() {
      computeNormal();
      //      computeCentroid();
      //      HalfEdge he = he0;
      //      do {
      //         he.updateU();
      //         he = he.next;
      //      }
      //      while (he != he0);
   }

   // implementation of IndexedPointSet
   public int numPoints() {
      return numVertices();
   }

   public Point3d getPoint (int idx) {
      return getVertex(idx).pnt;
   }
   
   public static boolean debugIntersect = false;
   
//   private static double orient3d (Vector3d r0, Vector3d r1, Vector3d r2) {
//      Vector3d xprod = new Vector3d();
//      xprod.cross (r0, r1);
//      return r2.dot(xprod);
//   }
   
   private static double ORIENT_EPS = (7+56*DOUBLE_PREC)*DOUBLE_PREC;

//   public static double orient3dx (
//      Vector3d a, Vector3d b, Vector3d c, DoubleHolder err) {
//
//      double cybx = c.y*b.x;
//      double cxby = c.x*b.y;
//      double cxay = c.x*a.y;
//      double cyax = c.y*a.x;
//      double axby = a.x*b.y;
//      double aybx = a.y*b.x;
//
//      double az = a.z;
//      double bz = b.z;
//      double cz = c.z;
//
//      double res = az*(cybx-cxby) + bz*(cxay-cyax) + cz*(axby-aybx);
//
//      if (cybx < 0) {
//         cybx = -cybx;
//      }
//      if (cxby < 0) {
//         cxby = -cxby;
//      }
//      if (cxay < 0) {
//         cxay = -cxay;
//      }
//      if (cyax < 0) {
//         cyax = -cyax;
//      }
//      if (axby < 0) {
//         axby = -axby;
//      }
//      if (aybx < 0) {
//         aybx = -aybx;
//      }
//      if (az < 0) {
//         az = -az;
//      }
//      if (bz < 0) {
//         bz = -bz;
//      }
//      if (cz < 0) {
//         cz = -cz;
//      }
//
//      double e = ORIENT_EPS*(az*(cybx+cxby) + bz*(cxay+cyax) + cz*(axby+aybx));
//
//      if (err != null) {
//         err.value = e;
//         return res;
//      }
//      else {
//         if (res <= e && res >= -e) {
//            return 0;
//         }
//         else {
//            return res;
//         }
//      }
//   }

   static boolean orientDebug = false;
   
   
   /*
    * x, y, z are world coordinates of a point already determined to be on the
    * plane of this face. Return true if the point is inside the face's
    * triangle. Calculate barycentric coordinates of x, y, z relative to each
    * vertex. The bc relative to a vertex is the area of the triangle formed by
    * the point and the other two vertices. If the bcs sum to 1 (or 1 - epsilon
    * due to rounding errors) the point is inside. If the sum is > 1 or (1 +
    * epsilon) the point is outside.
    */
   public static double insideTriangleTolerance = 1e-13;

   public boolean isPointInside (double x, double y, double z) {
      if (myNormal == null)
         computeNormal(); // Make sure referenceArea is current.

      double tol = referenceArea*insideTriangleTolerance;
      
      Point3d p0 = he0.tail.getWorldPoint();
      double xp0 = x - p0.x;
      double yp0 = y - p0.y;
      double zp0 = z - p0.z;
      Point3d p1 = he0.head.getWorldPoint();
      double xp1 = x - p1.x;
      double yp1 = y - p1.y;
      double zp1 = z - p1.z;
      double xa = yp0 * zp1 - zp0 * yp1;
      double ya = zp0 * xp1 - xp0 * zp1;
      double za = xp0 * yp1 - yp0 * xp1;
      double q0 = Math.sqrt (xa * xa + ya * ya + za * za);
      double q = referenceArea - q0;
      if (q < -tol) {
         if (debugIntersect) System.out.println (" fail 1 q=" + q + " ra=" + referenceArea);
         return false;
      }

      Point3d p2 = he0.next.head.getWorldPoint();
      double xp2 = x - p2.x;
      double yp2 = y - p2.y;
      double zp2 = z - p2.z;
      xa = yp0 * zp2 - zp0 * yp2;
      ya = zp0 * xp2 - xp0 * zp2;
      za = xp0 * yp2 - yp0 * xp2;
      q0 = Math.sqrt (xa * xa + ya * ya + za * za);
      q = q - q0;
      if (q < -tol) {
         if (debugIntersect) System.out.println (" fail 2 q=" + q + " ra=" + referenceArea);
         return false;
      }

      xa = yp1 * zp2 - zp1 * yp2;
      ya = zp1 * xp2 - xp1 * zp2;
      za = xp1 * yp2 - yp1 * xp2;
      q0 = Math.sqrt (xa * xa + ya * ya + za * za);
      q = q - q0;
      if (q < 0) { // q can only be > 0 due to rounding errors. If it is >= 0,
         // the point is inside.
         if (q < -tol) {
            if (debugIntersect) System.out.println (" fail 3 q=" + q + " ra=" + referenceArea);
            return false;
         }
      }
      return true;
   }

   /**
    * Returns a list of Vertex3d[3] representing the 
    * triangulated faces.  Note that this does not actually
    * affect the current mesh.
    */
   public void triangulate(ArrayList<Vertex3d[]> tris) {
      // estimated number of triangles
      Vertex3d[] verts = getVertices();
      if (numVertices() == 3) {
         tris.add(verts);
         return;
      }

      int nVerts = verts.length;
      while (nVerts > 3) {
         // find the indices of the best chord triangle, add the
         // corresponding face to the new face list, and remove
         // the vertex
         Vertex3d[] chord = bestChord (verts);
         tris.add(chord);

         int j = 0;
         for (int i=0; i<nVerts; i++) {
            verts[j] = verts[i];
            if (chord[1] != verts[i]) {
               j++;
            }
         }
         nVerts--;
      }
      tris.add(new Vertex3d[]{verts[0], verts[1], verts[2]});
   }

   /**
    * Returns the maximum cosine of the triangle formed from a set of three
    * vertices.
    */
   private double maxCosine (Vertex3d vtx0, Vertex3d vtx1, Vertex3d vtx2) {
      Vector3d u01 = new Vector3d();
      Vector3d u12 = new Vector3d();
      Vector3d u20 = new Vector3d();

      u01.sub (vtx1.pnt, vtx0.pnt);
      u01.normalize();
      u12.sub (vtx2.pnt, vtx1.pnt);
      u12.normalize();
      u20.sub (vtx0.pnt, vtx2.pnt);
      u20.normalize();

      double maxCos = u20.dot (u01);
      double c = u01.dot (u12);
      if (c > maxCos) {
         maxCos = c;
      }
      c = u12.dot (u20);
      if (c > maxCos) {
         maxCos = c;
      }
      return maxCos;
   }


   private Vertex3d[] bestChord (Vertex3d[] vtxs) {
      if (vtxs.length < 3) {
         throw new InternalErrorException ("less than three indices specified");
      }
      else if (vtxs.length == 3) {
         return new Vertex3d[] { vtxs[0], vtxs[1], vtxs[2] };
      }
      else if (vtxs.length == 4) {
         double cos301 = maxCosine (vtxs[3], vtxs[0], vtxs[1]);
         double cos012 = maxCosine (vtxs[0], vtxs[1], vtxs[2]);
         if (cos301 < cos012) {
            return new Vertex3d[] { vtxs[3], vtxs[0], vtxs[1] };
         }
         else {
            return new Vertex3d[] { vtxs[0], vtxs[1], vtxs[2] };
         }
      }
      else {
         double minCos = Double.POSITIVE_INFINITY;
         int i_min = 0;
         int i_prev, i_next;
         for (int i = 0; i < vtxs.length; i++) {

            i_prev = (i == 0 ? vtxs.length - 1 : i - 1);
            i_next = (i == vtxs.length - 1 ? 0 : i + 1);
            double cos = maxCosine (vtxs[i_prev], vtxs[i], vtxs[i_next]);
            if (cos < minCos) {
               i_min = i;
               minCos = cos;
            }
         }
         i_prev = (i_min == 0 ? vtxs.length - 1 : i_min - 1);
         i_next = (i_min == vtxs.length - 1 ? 0 : i_min + 1);
         return new Vertex3d[] { vtxs[i_prev], vtxs[i_min], vtxs[i_next] };
      }
   }

   public int getFlags() {
      return myFlags;
   }
   
   public void setFlags(int flags) {
      myFlags = flags;
   }
   
   public static int[] getIndices (Collection<Face> faces) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (Face f : faces) {
         list.add (f.getIndex());
      }
      return ArraySupport.toIntArray (list);      
   }
   
   /**
    * Returns a string identifying this face using the indices of
    * its vertices.
    * 
    * @return vertex-based identifying string
    */
   public String vertexStr() {
      StringBuilder sbuild = new StringBuilder();
      sbuild.append ("[");
      HalfEdge he = he0;
      do {
         sbuild.append (" ");
         sbuild.append (he.head.getIndex());
         he = he.next;
      }
      while (he != he0);
      sbuild.append (" ]");
      return sbuild.toString();
   }
}
