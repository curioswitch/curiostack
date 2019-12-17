using System.Collections.Generic;
using Google.Maps;
using Google.Maps.Event;
using Google.Maps.Unity.Intersections;
using UnityEngine;

/// <summary>
/// Example displaying the <see cref="RoadLattice"/> of the currently loaded map, and demonstrating
/// pathfinding on the map. Contains the <see cref="CreatePathBetweenObjects"/> Unity event handler
/// that receives two game objects, snaps those objects to the nearest nodes on the current road
/// lattice, and displays the shortest path between them.
/// </summary>
public class RoadLatticePathFinding : MonoBehaviour {
  /// <summary>
  /// The height above the ground plane of the path display object.
  /// </summary>
  private const float PathDisplayY = 0.5f;
  /// <summary>
  /// The height above the ground plane of the road lattice display object.
  /// </summary>
  private const float LatticeDisplayY = 1.0f;
  /// <summary>
  /// The width of the line segments in the path display object.
  /// </summary>
  private const float PathDisplayWidth = 4.0f;

  [Tooltip("Limit on the number of iterations during path findings.")]
  public int PathSearchLimit = 500;

  [Tooltip("If true, indicate objects will be created at each node.")]
  public bool IndicateNodes = false;

  [Tooltip("Whether to show partitioning of road lattice.")]
  public bool ShowPartitioned = false;

  [Tooltip("Material to apply to Road Lattice debug object.")]
  public Material[] LatticeMaterials;

  [Tooltip("Material to apply to displayed path.")]
  public Material PathMaterial;

  [Tooltip("MapsService providing the map on which to perform path finding.")]
  public MapsService MapsService;

  [Tooltip("Camera used to ray cast clicks onto the ground plane.")]
  public Camera Camera;

  /// <summary>
  /// Display object for the underlying road lattice.
  /// </summary>
  private GameObject RoadLatticeDebugObject;

  /// <summary>
  /// Display object for the current path.
  /// </summary>
  private GameObject PathDisplay;

  /// <summary>
  /// Creates a GameObject representing the supplied path.
  /// </summary>
  /// <param name="path">List of nodes in path order</param>
  /// <returns>The generated GameObject.</returns>
  private GameObject MakeRouteDisplay(IList<RoadLatticeNode> path) {
    List<Vector2> vertices = new List<Vector2>();
    foreach (RoadLatticeNode node in path) {
      vertices.Add(node.Location);
    }
    return RoadLatticeTools.MakePathDebugObject(vertices, PathDisplayWidth, PathMaterial);
  }

  /// <summary>
  /// Receives two game objects, snaps them to the current road lattice, and displays the shortest
  /// path between them.
  /// </summary>
  /// <remarks>
  /// Can be attached as a Unity event handler to the
  /// <see cref="RoadLatticePathFindingObjectMover.UpdatedPositions"/> UnityEvent.
  /// </remarks>
  /// <param name="start">The path start object.</param>
  /// <param name="end">The path end object.</param>
  public void CreatePathBetweenObjects(GameObject start, GameObject end) {
    if (PathDisplay != null) {
      Destroy(PathDisplay);
    }

    RoadLatticeNode startNode = MapsService.RoadLattice.SnapToNode(start.transform.position);
    RoadLatticeNode endNode = MapsService.RoadLattice.SnapToNode(end.transform.position);
    start.transform.position = new Vector3(startNode.Location.x, 0, startNode.Location.y);
    end.transform.position = new Vector3(endNode.Location.x, 0, endNode.Location.y);
    List<RoadLatticeNode> path =
        RoadLattice.UncheckedFindPath(startNode, endNode, PathSearchLimit);
    if (path == null) {
      Debug.LogFormat("No path found!");
    } else {
      PathDisplay = MakeRouteDisplay(path);
      PathDisplay.transform.Translate(Vector3.up * PathDisplayY);
    }
  }

  /// <summary>
  /// Creates a Road Lattice debug object for currently loaded map, and clears any partial path
  /// finding display and state.
  /// </summary>
  /// <remarks>
  /// Configured through the Unity editor as a handler for the DidModifyLattice event.
  /// </remarks>
  /// <param name="args">Map loaded arguments</param>
  public void HandleDidModifyLattice(DidModifyRoadLatticeArgs args) {
    if (RoadLatticeDebugObject != null) {
      Destroy(RoadLatticeDebugObject);
    }

    RoadLatticeDebugObject = RoadLatticeTools.MakeRoadLatticeDebugGameObject(
        args.RoadLattice, LatticeMaterials, IndicateNodes, ShowPartitioned);
    RoadLatticeDebugObject.transform.Translate(Vector3.up * LatticeDisplayY);
    RoadLatticeDebugObject.transform.SetParent(transform, false);
  }
}
