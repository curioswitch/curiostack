using System.Collections.Generic;
using Google.Maps.Unity.Intersections;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Handles requests for traversing edges in the road lattice.
  /// </summary>
  [RequireComponent(typeof(MapsService))]
  public class TrafficSystem : MonoBehaviour {
    /// <summary>
    /// Represents a regular, open path.
    /// </summary>
    public class Path {
      /// <summary>
      /// The world start position of this path.
      /// </summary>
      public readonly Vector3 FromLocation;

      /// <summary>
      /// The world end position of this path.
      /// </summary>
      public readonly Vector3 ToLocation;

      /// <summary>
      /// The tickets assigned to this path.
      /// </summary>
      public readonly LinkedList<PathTicket> Tickets;

      /// <summary>
      /// The length of this path.
      /// </summary>
      public readonly float Length;

      /// <summary>
      /// The total length held by tickets assigned to this path.
      /// </summary>
      public float OccupiedLength { get; set; }

      /// <summary>
      /// Constructor.
      /// </summary>
      public Path(Vector2 fromLocation, Vector2 toLocation) {
        Tickets = new LinkedList<PathTicket>();
        Length = Vector2.Distance(fromLocation, toLocation);
        FromLocation = new Vector3(fromLocation.x, 0, fromLocation.y);
        ToLocation = new Vector3(toLocation.x, 0, toLocation.y);
      }

      /// <summary>
      /// Returns true if this path is closed.
      /// </summary>
      public virtual bool IsClosed() {
        return false;
      }
    }

    /// <summary>
    /// Represents an open and closing path.
    /// </summary>
    private class IntersectionPath : Path {
      /// <summary>
      /// The time (in milliseconds) between path closures.
      /// </summary>
      private readonly int CloseFrequencyMs;

      /// <summary>
      /// The time (in milliseconds) this path should remain closed for.
      /// </summary>
      private readonly int CloseDurationMs;

      /// <summary>
      /// An offset applied to the current time to avoid paths with the same frequency
      /// closing at the same time.
      /// </summary>
      private readonly int CloseTimeOffsetMs;

      public IntersectionPath(Vector2 fromLocation, Vector2 toLocation, int closeFrequencyMs,
          int closeDurationMs) : base(fromLocation, toLocation) {
        CloseFrequencyMs = closeFrequencyMs;
        CloseDurationMs = closeDurationMs;
        CloseTimeOffsetMs = Random.Range(0, closeDurationMs);
      }

      /// <inheritdoc />
      public override bool IsClosed() {
        long ms = (long)Time.time * 1000;
        return (ms + CloseTimeOffsetMs) % CloseFrequencyMs < CloseDurationMs;
      }
    }

    /// <summary>
    /// Tracks the current position of a vehicle along a path.
    /// Provided to vehicles given permission to use a path after requesting it via
    /// <see cref="TrafficSystem.Request"/>.
    /// </summary>
    public class PathTicket {
      /// <summary>
      /// Returns the remaining distance along the path from the current position of the ticket.
      /// </summary>
      public float DistanceToTarget {
        get {
          return Path.Length - CurrentDistance;
        }
      }

      /// <summary>
      /// Returns the current world position of the ticket along the path.
      /// </summary>
      public Vector3 CurrentPosition {
        get {
          return Path.FromLocation +
              (Path.ToLocation - Path.FromLocation).normalized * CurrentDistance;
        }
      }

      /// <summary>
      /// Returns the direction of the path this ticket is assigned to.
      /// </summary>
      public Vector3 Heading {
        get {
          return (Path.ToLocation - Path.FromLocation).normalized;
        }
      }

      /// <summary>
      /// The path this ticket has been assigned.
      /// </summary>
      private readonly Path Path;

      /// <summary>
      /// The space on the path assigned to this ticket.
      /// </summary>
      private readonly float Length;

      /// <summary>
      /// THe node of this ticket in the ticket linked list.
      /// </summary>
      private readonly LinkedListNode<PathTicket> Node;

      /// <summary>
      /// The distance along the path this ticket is located at.
      /// </summary>
      private float CurrentDistance;

      /// <summary>
      /// Constructor.
      /// </summary>
      /// <param name="path">The path assigned to this ticket.</param>
      /// <param name="length">The space on the path assigned to this ticket.</param>
      /// <param name="node">The node of this ticket in the ticket linked list.</param>
      public PathTicket(Path path, float length, LinkedListNode<PathTicket> node) {
        Path = path;
        Length = length;
        Node = node;
      }

      /// <summary>
      /// Advance this ticket along the path.
      /// </summary>
      /// <param name="distance">The distance along the path to advance by.</param>
      public void Move(float distance) {
        // Check if there's a ticket (vehicle) in front of this one.
        LinkedListNode<PathTicket> vehicleInFrontNode = Node.Previous;
        if (vehicleInFrontNode != null) {
          // Don't move beyond the ticket in front.
          PathTicket other = vehicleInFrontNode.Value;
          CurrentDistance = Mathf.Clamp(CurrentDistance + distance, 0,
              Mathf.Max(0, other.CurrentDistance - other.Length));
        } else {
          // No ticket in front.
          CurrentDistance = Mathf.Clamp(CurrentDistance + distance, 0, Path.Length);
        }
      }

      /// <summary>
      /// Release the ticket, freeing available space on the path.
      /// </summary>
      public void Release() {
        Node.List.Remove(Node.Value);
        Path.OccupiedLength -= Length;
      }
    }

    /// <summary>
    /// Returns the road lattice this traffic system controls.
    /// </summary>
    public RoadLattice RoadLattice {
      get {
        return MapsService.RoadLattice;
      }
    }

    private readonly Dictionary<string, Path> Paths = new Dictionary<string, Path>();
    private MapsService MapsService;

    void Awake() {
      MapsService = GetComponent<MapsService>();
    }

    /// <summary>
    /// Request a ticket for a particular path.
    /// </summary>
    /// <param name="from">The start location UID.</param>
    /// <param name="to">The target location UID.</param>
    /// <param name="vehicle">The requesting vehicle.</param>
    /// <returns>A ticket for the path, or null if one could not be given.</returns>
    public PathTicket Request(ulong from, ulong to, Vehicle vehicle) {
      string key = GetPathKey(from, to);
      Path path;

      // Check if this is the first time this path has been requested.
      if (!Paths.TryGetValue(key, out path)) {
        // Retrieve the nodes by their UID.
        RoadLatticeNode fromNode = RoadLattice.FindNodeAt(from);
        RoadLatticeNode toNode = RoadLattice.FindNodeAt(to);

        // Check that the nodes exist.
        if (fromNode == null || toNode == null) {
          return null;
        }

        // Check if the edge for this path is flagged as entering an intersection.
        RoadLatticeEdge edge = fromNode.EdgeTo(toNode);
        if ((edge.EdgeFlags & RoadLatticeEdge.Flags.Intersection) != 0) {
          // Generate a path that opens and closes, like a traffic light. Randomize the values of
          // this behaviour.
          int closeDurationMs = Random.Range(3000, 8000);
          int closeFrequencyMs = Random.Range(10000, 30000);
          path = new IntersectionPath(fromNode.Location, toNode.Location, closeFrequencyMs,
              closeDurationMs);
        } else {
          // Not an intersection edge. Generate a regular path.
          path = new Path(fromNode.Location, toNode.Location);
        }

        // Store the path.
        Paths.Add(key, path);
      }

      if (path.IsClosed()) {
        // Path is closed, don't return a ticket. The requesting vehicle should try again later.
        return null;
      }

      // Check if the vehicle can fit on the path given its available length, or if the path is
      // currently empty.
      if (path.Length - path.OccupiedLength >= vehicle.Clearance || path.Tickets.Count == 0) {
        // Add this vehicle's clearance to the path to update its available length.
        path.OccupiedLength += vehicle.Clearance;

        // Generate and return a ticket for the path.
        LinkedListNode<PathTicket> node = new LinkedListNode<PathTicket>(null);
        PathTicket ticket = new PathTicket(path, vehicle.Clearance, node);
        node.Value = ticket;
        path.Tickets.AddLast(node);

        return ticket;
      }

      return null;
    }

    /// <summary>
    /// Returns a unique key for the path between two location UIDs.
    /// </summary>
    private string GetPathKey(ulong from, ulong to) {
      return string.Format("{0}->{1}", from, to);
    }
  }
}
