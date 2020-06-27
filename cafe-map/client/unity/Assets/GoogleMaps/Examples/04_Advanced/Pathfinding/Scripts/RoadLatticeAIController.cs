using System.Collections.Generic;
using Unity.Collections;
using UnityEngine;
using Google.Maps.Unity.Intersections;

namespace Google.Maps.Examples {
  /// Note: Road Lattice support is a beta feature subject to performance considerations and future
  /// changes
  /// <summary>
  /// This component provides a basic implementation of a search behaviour based on a waypoints
  /// system.
  /// Road lattice nodes define the waypoints in this case.
  /// </summary>
  [RequireComponent(typeof(Rigidbody))]
  public class RoadLatticeAIController : PathingAgent {
    /// <summary>
    /// Reference to the active target.
    /// </summary>
    public GameObject Target;

    /// <summary>
    /// Requests a new path between the AI agent and its target from the Maps SDK.
    /// Their positions are snapped to the closest road lattice nodes.
    /// If we get a valid path, we start moving the agent.
    /// </summary>
    protected override void CheckPath() {
      // If we have a target but are not moving, find a path to the target.
      if (Target != null && !IsMoving) {
        PathTo(Target.transform.position);
      }
    }
  }
}
