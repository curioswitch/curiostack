using System;
using System.Collections.Generic;
using System.IO;
using Google.Maps.Feature;
using Google.Maps.Unity.Intersections;
using UnityEditor;
using UnityEngine;
using Random = UnityEngine.Random;

namespace Google.Maps.Examples {
  /// <summary>
  /// An agent that queries a <see cref="TrafficSystem"/> to move along a road lattice.
  /// </summary>
  /// <remarks>
  /// All values are in Unity worldspace units.
  /// </remarks>
  public class Vehicle : MonoBehaviour {
    /// <summary>
    /// The distance from the road centerline the vehicle should travel along.
    /// </summary>
    public float DistanceFromRoadCenter = 3;

    /// <summary>
    /// The size of this vehicle used for determining where other vehicles should stop.
    /// </summary>
    public float Clearance = 2;

    /// <summary>
    /// The speed this vehicle in worldspace units per second.
    /// </summary>
    public float Speed = 10;

    /// <summary>
    /// The maximum distance the vehicle should be from its target position until choosing the next
    /// target position.
    /// </summary>
    private const float MaxDistanceUntilNextTarget = 1;

    /// <summary>
    /// The traffic system.
    /// </summary>
    private TrafficSystem TrafficSystem;

    /// <summary>
    /// The ticket this vehicle holds (if it holds one).
    /// </summary>
    private TrafficSystem.PathTicket Ticket;

    /// <summary>
    /// The location UID of the last node this vehicle visited.
    /// </summary>
    private ulong CurrentNodeLocationUID;

    /// <summary>
    /// The location UID of the target node for this vehicle to path to.
    /// </summary>
    private ulong TargetNodeLocationUID;

    /// <summary>
    /// Returns true if the given segment is a (traversable) road.
    /// </summary>
    /// <param name="segment">The segment to test.</param>
    /// <returns>True if the segment is a traversable road.</returns>
    public static bool IsTraversableRoad(Segment segment) {
      return segment.Metadata.Usage != SegmentMetadata.UsageType.Ferry &&
          segment.Metadata.Usage != SegmentMetadata.UsageType.Footpath &&
          segment.Metadata.Usage != SegmentMetadata.UsageType.Rail;
    }

    /// <summary>
    /// Initializes the vehicle.
    /// </summary>
    /// <param name="trafficSystem">The traffic system.</param>
    /// <param name="startNode">The node the vehicle should start at.</param>
    public void Initialize(TrafficSystem trafficSystem, RoadLatticeNode startNode) {
      TrafficSystem = trafficSystem;

      // Find a target node from the start node.
      RoadLatticeNode targetNode = FindNextTarget(startNode, null);
      CurrentNodeLocationUID = startNode.LocationUID;
      TargetNodeLocationUID = targetNode.LocationUID;

      // Set the vehicle's position and rotation.
      Vector2 heading = (targetNode.Location - startNode.Location).normalized;
      transform.position = new Vector3(startNode.Location.x, 0, startNode.Location.y) +
          new Vector3(-heading.y, 0, heading.x) * DistanceFromRoadCenter;
      transform.forward = new Vector3(heading.x, 0, heading.y);
    }

    /// <summary>
    /// Queries <see cref="TrafficSystem"/> and moves the vehicle.
    /// </summary>
    void Update() {
      // Get the current and target node objects from the road lattice.
      RoadLatticeNode currentNode = TrafficSystem.RoadLattice.FindNodeAt(CurrentNodeLocationUID);
      RoadLatticeNode targetNode = TrafficSystem.RoadLattice.FindNodeAt(TargetNodeLocationUID);

      // Check that our current and target nodes exist in the road lattice.
      if (targetNode == null || currentNode == null) {
        if (currentNode != null && currentNode.NeighborCount > 0) {
          // Target node doesn't exist but the current node does; try to find a new target.
          FindNextTarget(currentNode, null);
        } else {
          // Both the current and target nodes no longer exist or the current node has no
          // neighbors. Destroy the vehicle.
          if (Ticket != null) {
            Ticket.Release();
          }
          Destroy(gameObject);
        }
        return;
      }

      // Check if we have a ticket to the target node.
      if (Ticket == null) {
        // Don't have a ticket. Request one.
        Ticket = TrafficSystem.Request(CurrentNodeLocationUID, TargetNodeLocationUID, this);
        if (Ticket == null) {
          // Didn't get a ticket. Either the path is at capacity or is closed. Try again later.
          return;
        }
      }

      // Get the target position of the vehicle. This is the position offset from the centreline
      // by `DistanceFromRoadCenter`.
      Vector3 perpendicular = new Vector3(-Ticket.Heading.z, 0, Ticket.Heading.x);
      Vector3 targetPosition = Ticket.CurrentPosition + perpendicular * DistanceFromRoadCenter;

      // Face the target position.
      Vector3 heading = (targetPosition - transform.position).normalized;
      if (heading.sqrMagnitude > 0) {
        transform.forward = heading;
      }

      // Move the vehicle.
      transform.position = Vector3.MoveTowards(transform.position, targetPosition,
          Speed * Time.deltaTime);

      // Advance the ticket position if we've reached `targetPosition`.
      if ((transform.position - targetPosition).sqrMagnitude < 0.1f) {
        Ticket.Move(Speed * Time.deltaTime);
      }

      // Check if we've reached the end of the path.
      if (Ticket.DistanceToTarget < MaxDistanceUntilNextTarget) {
        // Find the next target node.
        RoadLatticeNode nextTargetNode = FindNextTarget(targetNode, currentNode);

        // Try acquiring a ticket to the new node.
        TrafficSystem.PathTicket nextTicket =
            TrafficSystem.Request(TargetNodeLocationUID, nextTargetNode.LocationUID, this);
        if (nextTicket == null) {
          // Couldn't acquire a ticket, try again next frame. Don't release the current ticket
          // until we can acquire the next one.
          return;
        }

        // Acquired a ticket. Release the old one and set the new current and target nodes.
        Ticket.Release();
        Ticket = nextTicket;
        CurrentNodeLocationUID = TargetNodeLocationUID;
        TargetNodeLocationUID = nextTargetNode.LocationUID;
      }
    }

    /// <summary>
    /// Returns the next target node after traversing the edge between two nodes.
    /// </summary>
    /// <param name="endNode">The previous end node.</param>
    /// <param name="startNode">
    /// The previous start node. This node won't be returned unless it's the only neighbor of
    /// <see cref="endNode"/>.
    /// </param>
    /// <returns>The next target node to path to.</returns>
    private RoadLatticeNode FindNextTarget(RoadLatticeNode endNode, RoadLatticeNode startNode) {
      // Find all traversable neighbor nodes.
      List<RoadLatticeNode> neighbors = new List<RoadLatticeNode>();
      foreach (RoadLatticeEdge edge in endNode.Edges) {
        if (IsTraversableRoad(edge.Segment)) {
          neighbors.Add(edge.Target);
        }
      }

      if (neighbors.Count > 1) {
        // Remove the start node so we don't return it.
        neighbors.Remove(startNode);
      }

      // Return a random neighbor node.
      return neighbors[Random.Range(0, neighbors.Count)];
    }
  }
}
