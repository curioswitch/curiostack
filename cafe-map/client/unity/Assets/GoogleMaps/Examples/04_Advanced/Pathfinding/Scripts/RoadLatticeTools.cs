using System;
using System.Collections.Generic;
using Google.Maps.Feature;
using Google.Maps.Unity.Intersections;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Utility functions for visualization and debugging of <see cref="RoadLattice"/> objects.
  /// </summary>
  public class RoadLatticeTools {
    /// <summary>
    /// The maximum number of segments a lattice representation can contain before it will be split
    /// into multiple submeshes to avoid unity mesh vertex limit issues.
    /// </summary>
    private static int SubmeshSegmentLimit = 2000;

    /// <summary>
    /// Maximum size of the gap in the middle of the rectangular geometry representing a road
    /// segment.
    /// </summary>
    private static float MaxSegmentMidGapLength = 2.0f;

    /// <summary>
    /// Collection of Road types ordered by priority from lowest to highest.
    /// </summary>
    private static readonly SegmentMetadata.UsageType[] RoadPriorities = {
      SegmentMetadata.UsageType.LocalRoad,
      SegmentMetadata.UsageType.ArterialRoad,
      SegmentMetadata.UsageType.Highway,
    };

    /// <summary>
    /// Instantiates a GameObject showing connections between <see cref="RoadLatticeNode"/> objects.
    /// </summary>
    /// <param name="lattice">The lattice from which to generate a GameObject</param>
    /// <param name="materials">
    /// The material(s) to apply to the created RoadLattice object</param>
    /// <param name="indicateNodes">Whether to add markers at each node</param>
    /// <param name="showPartitioning">
    /// Whether to create separate debug objects for each independent, contiguous sub-lattice
    /// </param>
    /// <returns>The GameObject representation of the supplied lattice</returns>
    public static GameObject MakeRoadLatticeDebugGameObject(
        RoadLattice lattice, Material[] materials, bool indicateNodes, bool showPartitioning) {
      if (materials == null || materials.Length == 0) {
        materials = new Material[] { null };
      }

      if (!showPartitioning) {
        return MakeUnpartitionedLatticeDebugGameObject(lattice, indicateNodes, materials[0]);
      } else {
        return MakePartitionedRoadLatticeDebugGameObject(lattice, indicateNodes, materials);
      }
    }

    /// <summary>
    /// Instantiates a GameObject showing connections between <see cref="RoadLatticeNode"/>s without
    /// distinguishing disjoint partitions of the road graph.
    /// </summary>
    /// <param name="lattice">The lattice from which to generate a GameObject</param>
    /// <param name="indicateNodes">Whether to add markers at each node</param>
    /// <param name="material">The material to apply to the generated mesh</param>
    /// <returns>The GameObject representation of the supplied lattice</returns>
    private static GameObject MakeUnpartitionedLatticeDebugGameObject(
        RoadLattice lattice, bool indicateNodes, Material material) {
      GameObject latticeParent = new GameObject();
      latticeParent.name = "Road Lattice";
      List<Vector2> vertices = new List<Vector2>();
      List<RoadLatticeNode> nodes = lattice.GetNodes();
      HashSet<RoadLatticeNode> processed = new HashSet<RoadLatticeNode>();

      foreach (RoadLatticeNode node in nodes) {
        if (indicateNodes) {
          MakeRoadLatticeNodeIndicator(node, latticeParent);
        }

        foreach (RoadLatticeNode neighbor in node.Neighbors) {
          if (processed.Contains(neighbor)) {
            continue;
          }

          vertices.Add(node.Location);
          vertices.Add(neighbor.Location);
        }

        processed.Add(node);
      }

      MakeSublatticeMeshes(latticeParent, vertices, material);

      return latticeParent;
    }

    /// <summary>
    /// Instantiates a GameObject showing connections between <see cref="RoadLatticeNode"/>s,
    /// creating a separate mesh for each road priority, and assigning materials based on priority.
    /// </summary>
    /// <param name="lattice">The lattice from which to generate a GameObject</param>
    /// <param name="materials">The materials to apply to the generated meshes</param>
    /// <param name="indicateNodes">Whether to add markers at each node</param>
    /// <returns>The GameObject representation of the supplied lattice</returns>
    public static GameObject MakeAttributedLatticeDebugGameObject(
        RoadLattice lattice, Material[] materials, bool indicateNodes) {
      GameObject latticeParent = new GameObject();
      latticeParent.name = "Road Lattice";

      // One set of mesh vertices per road priority, plus one for the default.
      List<Vector2>[] vertices = new List<Vector2>[RoadPriorities.Length + 1];

      for (int i = 0; i < vertices.Length; i++) {
        vertices[i] = new List<Vector2>();
      }

      List<RoadLatticeNode> nodes = lattice.GetNodes();
      HashSet<RoadLatticeNode> processed = new HashSet<RoadLatticeNode>();

      foreach (RoadLatticeNode node in nodes) {
        if (indicateNodes) {
          MakeRoadLatticeNodeIndicator(node, latticeParent);
        }

        foreach (RoadLatticeEdge edge in node.Edges) {
          if (processed.Contains(edge.Target)) {
            continue;
          }

          // Map the road priority to the supplied materials using the RoadPriorities array.
          int index =
              (Array.IndexOf(RoadPriorities, edge.Segment.Metadata.Usage) + 1) % vertices.Length;
          vertices[index].Add(node.Location);
          vertices[index].Add(edge.Target.Location);
        }

        processed.Add(node);
      }

      for (int i = 0; i < vertices.Length; i++) {
        MakeSublatticeMeshes(latticeParent, vertices[i], materials[i % materials.Length]);
      }

      return latticeParent;
    }

    /// <summary>
    /// Instantiates a GameObject showing connections between <see cref="RoadLatticeNode"/>s using
    /// materials to distinguish disjoint partitions of the road graph.
    /// </summary>
    /// <param name="lattice">The lattice from which to generate a GameObject</param>
    /// <param name="indicateNodes">Whether to add markers at each node</param>
    /// <param name="materials">The materials to apply to the disjoint sub-lattices</param>
    /// <returns>The GameObject representation of the supplied lattice</returns>
    private static GameObject MakePartitionedRoadLatticeDebugGameObject(
        RoadLattice lattice, bool indicateNodes, Material[] materials) {
      // A list for collecting road segment end point locations.
      List<Vector2> vertices = new List<Vector2>();

      // A set of nodes with connections to other nodes that have all been converted into geometry.
      HashSet<RoadLatticeNode> processed = new HashSet<RoadLatticeNode>();

      int materialIndex = 0;
      GameObject latticeParent = new GameObject();
      latticeParent.name = "Road Lattice Parent";
      List<RoadLatticeNode> nodes = lattice.GetNodes();

      foreach (RoadLatticeNode node in nodes) {
        // Skip this node if it has already had all its connections turned into geometry, otherwise
        // use it as the starting node for a new disjoint sub-lattice representation.
        if (processed.Contains(node)) {
          continue;
        }

        vertices.Clear();

        // A set of nodes reachable from the set of nodes that have been processed so far. Note:
        // this list may contain nodes that have already been processed if such ndoes are reachable
        // from more than one neighbouring node.
        Stack<RoadLatticeNode> reachableNodes = new Stack<RoadLatticeNode>();
        reachableNodes.Push(node);

        // Keep processing nodes til we have visited all reachable nodes.
        while (reachableNodes.Count > 0) {
          RoadLatticeNode source = reachableNodes.Pop();

          // Skip nodes we have previously processed by reaching them through an alternative path.
          if (processed.Contains(source)) {
            continue;
          }

          if (indicateNodes) {
            MakeRoadLatticeNodeIndicator(source, latticeParent);
          }

          // Mark this node as processed, add segments for all its connections, and add all its
          // neighbours to the reachable set.
          processed.Add(source);

          foreach (RoadLatticeNode neighbor in source.Neighbors) {
            if (!processed.Contains(neighbor)) {
              vertices.Add(source.Location);
              vertices.Add(neighbor.Location);
              reachableNodes.Push(neighbor);
            }
          }
        }

        GameObject go = new GameObject();
        go.name = "Road Lattice";
        MakeSublatticeMeshes(go, vertices, materials[materialIndex]);
        materialIndex = (materialIndex + 1) % materials.Length;
        go.transform.parent = latticeParent.transform;
      }

      return latticeParent;
    }

    /// <summary>
    /// Creates a maker (a ground plane penetrating sphere of unit radius) at the location of the
    /// supplied <see cref="RoadLatticeNode"/>
    /// </summary>
    /// <param name="node">The node to indicate</param>
    /// <param name="parent">The parent GameObject under which to create the indicator
    /// object</param>
    static void MakeRoadLatticeNodeIndicator(RoadLatticeNode node, GameObject parent) {
      GameObject sphere = GameObject.CreatePrimitive(PrimitiveType.Sphere);
      sphere.transform.position = new Vector3(node.Location.x, 0, node.Location.y);
      sphere.name = "RoadLatticeNode";
      sphere.transform.parent = parent.transform;
    }

    /// <summary>
    /// Instantiate a number of debug meshes from the supplied list of road lattice segments,
    /// dividing the collection of segments into multiple subsets to avoid creating any meshes with
    /// an excessively large number of vertices.
    /// </summary>
    /// <param name="parent">The parent object for sublattice objects.</param>
    /// <param name="segments">
    /// List of vertices for lattice segments. Each segment is represented by its start and end
    /// vertex, with no sharing of vertices between segments
    /// </param>
    /// <param name="material">The material to apply to the generated mesh.</param>
    /// <returns>The constructed mesh.</returns>
    private static void MakeSublatticeMeshes(
        GameObject parent, List<Vector2> segments, Material material) {
      List<Vector3> vertices = new List<Vector3>();
      List<Vector2> uvs = new List<Vector2>();
      List<int> triangles = new List<int>();

      // Keep generating submeshes until we've processed all segments.
      for (int start = 0; start < segments.Count; start += SubmeshSegmentLimit) {
        vertices.Clear();
        triangles.Clear();
        uvs.Clear();
        GameObject go = new GameObject();
        go.name = "Sublattice";
        MeshRenderer mr = go.AddComponent<MeshRenderer>();
        go.transform.parent = parent.transform;
        MeshFilter mf = go.AddComponent<MeshFilter>();

        // Process segments up to the limit for an individual submesh.
        for (int i = start; i < segments.Count && i < start + SubmeshSegmentLimit; i += 2) {
          Vector2 v1 = segments[i];
          Vector2 v2 = segments[i + 1];
          Vector2 midpoint = (v1 + v2) / 2;
          float centerGapSize = (v2 - v1).magnitude / 4.0f;

          // Cap the size of the mid-segment gap.
          float delta = Mathf.Min(centerGapSize, MaxSegmentMidGapLength) / 2.0f;
          Vector2 direction = (v2 - v1).normalized;

          // Create two quads starting/ending either side of the midpoint, leaving the calculated
          // gap.
          Vector2 v1Neighbour = midpoint - direction * delta;
          Vector2 v2Neighbour = midpoint + direction * delta;
          AddLineSegmentQuad(v1, v1Neighbour, 1, vertices, uvs, triangles);
          AddLineSegmentQuad(v2Neighbour, v2, 1, vertices, uvs, triangles);
        }

        Debug.Assert(vertices.Count == uvs.Count);
        Mesh mesh = new Mesh();
        mesh.vertices = vertices.ToArray();
        mesh.triangles = triangles.ToArray();
        mesh.uv = uvs.ToArray();
        mf.sharedMesh = mesh;
        mr.sharedMaterial = material;
      }
    }

    /// <summary>
    /// Instantiates a GameObject representing the path specified by the supplied node locations.
    /// </summary>
    /// <param name="nodeLocations">The ordered set of vertices along the path</param>
    /// <param name="width">Width of the line segments in the path debug object</param>
    /// <param name="material">Material to apply to the path debug object.</param>
    /// <returns>The generated path debug GameObject</returns>
    public static GameObject MakePathDebugObject(
        List<Vector2> nodeLocations, float width, Material material) {
      List<Vector3> vertices = new List<Vector3>();
      List<Vector2> uvs = new List<Vector2>();
      List<int> triangles = new List<int>();

      GameObject pathDebug = new GameObject();
      pathDebug.name = "Path Debug";
      MeshRenderer mr = pathDebug.AddComponent<MeshRenderer>();
      MeshFilter mf = pathDebug.AddComponent<MeshFilter>();

      for (int i = 0; i < nodeLocations.Count - 1; i++) {
        AddLineSegmentQuad(nodeLocations[i], nodeLocations[i + 1], width, vertices, uvs, triangles);
      }

      Mesh mesh = new Mesh();
      mesh.vertices = vertices.ToArray();
      mesh.triangles = triangles.ToArray();
      mesh.uv = uvs.ToArray();
      mf.sharedMesh = mesh;
      mr.sharedMaterial = material;

      return pathDebug;
    }

    /// <summary>
    /// Adds to the supplied vertex and triangle index lists a sky facing rectangle representing the
    /// line segment between the supplied ground plane 2-dimensional points, using the supplied
    /// width.
    /// </summary>
    /// <remarks>
    /// Four vertices are added to the vertices list, and the six indices for two triangles are
    /// added to the triangles list.
    /// </remarks>
    /// <param name="p1">One end of the line segment on the 2D ground plane.</param>
    /// <param name="p2">One end of the line segment on the 2D ground plane.</param>
    /// <param name="width">The width of the generated line segment rectangle</param>
    /// <param name="vertices">Destination list for generated vertices</param>
    /// <param name="triangles">Destination list for generated triangle indices</param>
    private static void AddLineSegmentQuad(
        Vector2 p1, Vector2 p2, float width, List<Vector3> vertices, List<Vector2> uvs,
        List<int> triangles) {
      Debug.Assert(vertices.Count == uvs.Count);
      Vector3 start = new Vector3(p1.x, 0, p1.y);
      Vector3 end = new Vector3(p2.x, 0, p2.y);
      Vector3 dir = end - start;
      float length = dir.magnitude;
      dir = dir.normalized * length;
      Vector3 left = Vector3.Cross(dir, Vector3.down).normalized * width;
      int first = vertices.Count;
      vertices.Add(start + left);
      vertices.Add(end + left);
      vertices.Add(end - left);
      vertices.Add(start - left);
      uvs.Add(new Vector2(0, 0));
      uvs.Add(new Vector2(0, length));
      uvs.Add(new Vector2(1, length));
      uvs.Add(new Vector2(1, 0));
      triangles.Add(first);
      triangles.Add(first + 3);
      triangles.Add(first + 1);
      triangles.Add(first + 1);
      triangles.Add(first + 3);
      triangles.Add(first + 2);
    }
  }
}
