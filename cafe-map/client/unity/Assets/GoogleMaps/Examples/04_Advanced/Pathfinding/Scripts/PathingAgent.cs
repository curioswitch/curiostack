using System.Collections.Generic;
using Google.Maps.Unity.Intersections;
using Unity.Collections;
using UnityEngine;

namespace Google.Maps.Examples {
  /// Note: Road Lattice support is a beta feature subject to performance considerations and future
  /// changes.
  /// <summary>
  /// Base class for agents using the road lattice for pathing. Classes inheriting from this should
  /// override CheckPath and make calls to PathTo to set the trajectory of the agent.
  /// </summary>
  public abstract class PathingAgent : MonoBehaviour {
    /// <summary>
    /// Indicates if the agent is moving.
    /// </summary>
    public bool IsMoving { get; private set; }

    /// <summary>
    /// Reference to the base map loader (which knows about MapsService).
    /// The <see cref="BaseMapLoader"/> handles the basic loading of a map region and provides
    /// default styling parameters and loading errors management.
    /// </summary>
    public BaseMapLoader BaseMapLoader;

    [Tooltip("Limit on the number of iterations during path findings.")]
    public int PathSearchLimit = 500;

    [Tooltip("Material to apply to displayed path.")]
    public Material PathMaterial;

    /// <summary>
    /// The current target position as we are following along waypoints.
    /// </summary>
    [ReadOnly]
    public Vector3 CurrentTargetPosition;

    /// <summary>
    /// Character's speed.
    /// </summary>
    public float Speed = 5f;

    /// <summary>
    /// Minimum distance to reach before the search ends.
    /// </summary>
    public float MinDistance = 3f;

    /// <summary>
    /// Indicates if the active path to target should be highlighted.
    /// </summary>
    public bool ShowPath = false;

    /// <summary>
    /// List of road lattice nodes defining the current search path.
    /// </summary>
    private List<RoadLatticeNode> PathToDestination = new List<RoadLatticeNode>();

    /// <summary>
    /// The width of the line segments in the path display object.
    /// </summary>
    private const float PathDisplayWidth = 4.0f;

    /// <summary>
    /// The y-offset of the path display to avoid z-fighting.
    /// </summary>
    private const float PathDisplayYOffset = 0.01f;

    /// <summary>
    /// Visual display of the active path.
    /// </summary>
    private GameObject PathDisplay;

    /// <summary>
    /// Current location on the path to destination.
    /// </summary>
    private int WaypointIndex;

    /// <summary>
    /// Makes calls to PathTo to set the trajectory of the PathingAgent.
    /// </summary>
    protected abstract void CheckPath();

    /// <summary>
    /// Initializes the path to destination, current waypoint and whether the character is
    /// moving (or not).
    /// </summary>
    void Start() {
      PathToDestination = new List<RoadLatticeNode>();
      WaypointIndex = 0;
      IsMoving = false;
    }

    void Update() {
      // Validate current path
      CheckPath();

      // Move along current path
      MoveAlongWaypoints();

      // Display current path
      DisplayRoute(PathToDestination);
    }

    protected void PathTo(Vector3 endPosition) {
      // Snap start/end positions to road lattice nodes
      RoadLatticeNode startNode =
          BaseMapLoader.MapsService.RoadLattice.SnapToNode(transform.position);
      RoadLatticeNode endNode = BaseMapLoader.MapsService.RoadLattice.SnapToNode(endPosition);

      // Find path between start/end nodes
      PathToDestination = RoadLattice.UncheckedFindPath(startNode, endNode, PathSearchLimit);

      // Reset our pathfinding instructions - waypoint at index 0 is our start position
      WaypointIndex = 0;

      if (PathToDestination != null && PathToDestination.Count > 0) {
        // Init navigation details - our next target is the waypoint after the start position
        CurrentTargetPosition = new Vector3(
            PathToDestination[WaypointIndex].Location.x,
            transform.position.y,
            PathToDestination[WaypointIndex].Location.y);
        IsMoving = true;
      } else {
        IsMoving = false;
      }

      DisplayRoute(PathToDestination);
    }

    /// <summary>
    /// Moves this <see cref="GameObject"/> along waypoints.
    /// </summary>
    private void MoveAlongWaypoints() {
      if (!IsMoving)
        return;

      // Are we close enough to the target position?
      if (Vector3.Distance(transform.position, CurrentTargetPosition) > MinDistance) {
        transform.position =
            Vector3.MoveTowards(transform.position, CurrentTargetPosition, Speed * Time.deltaTime);

        Quaternion rotation = Quaternion.LookRotation(CurrentTargetPosition - transform.position);
        transform.rotation = Quaternion.Slerp(transform.rotation, rotation, Speed * Time.deltaTime);
      } else {
        // If we're close enough to the target position, start moving to the next waypoint. If this
        // is the last waypoint, stop moving.
        WaypointIndex++;
        if (WaypointIndex >= PathToDestination.Count) {
          IsMoving = false;
        } else {
          CurrentTargetPosition = new Vector3(
              PathToDestination[WaypointIndex].Location.x,
              transform.position.y,
              PathToDestination[WaypointIndex].Location.y);
        }
      }
    }

    /// <summary>
    /// Debug feature: highlights the active path between the agent and its target on the Map.
    /// </summary>
    /// <param name="route">The route to display.</param>
    private void DisplayRoute(List<RoadLatticeNode> route) {
      if (PathDisplay != null) {
        Destroy(PathDisplay);
      }

      if (ShowPath && IsMoving && route != null) {
        PathDisplay = MakeRouteDisplay(route);
        PathDisplay.transform.position += Vector3.up * PathDisplayYOffset;
      }
    }

    /// <summary>
    /// Debug feature: used to display the active path between the agent and its target.
    /// </summary>
    /// <param name="path">The path to create a display from.</param>
    /// <returns>The route display object.</returns>
    private GameObject MakeRouteDisplay(IList<RoadLatticeNode> path) {
      List<Vector2> vertices = new List<Vector2>();

      foreach (RoadLatticeNode node in path) {
        vertices.Add(node.Location);
      }

      return RoadLatticeTools.MakePathDebugObject(vertices, PathDisplayWidth, PathMaterial);
    }
  }
}
