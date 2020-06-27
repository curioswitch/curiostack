using System.Collections.Generic;
using Google.Maps.Unity.Intersections;
using Unity.Collections;
using UnityEngine;
using UnityEngine.EventSystems;

namespace Google.Maps.Examples {
  /// Note: Road Lattice support is a beta feature subject to performance considerations and future
  /// changes.
  /// <summary>
  /// This basic controller allows the user to move a character along waypoints (road lattice nodes)
  /// on the map in the direction defined by the latest touch on the screen.
  /// </summary>
  public class RoadLatticeCharacterController : PathingAgent {
    /// <summary>
    /// The plane used for ground position detection of clicks.
    /// </summary>
    private Plane GroundPlane = new Plane(Vector3.up, 0);

    /// <summary>
    /// Detects if the user has touched the screen.
    /// If so, computes a path to the new destination and starts moving the character in that
    /// direction.
    /// </summary>
    protected override void CheckPath() {
      // If a touch is detected, move the character in that direction on the x,z plane
      // Next step - character should move to next valid closest waypoint
      if (Input.GetMouseButtonUp(0)) {
        // Check if the mouse was clicked over a UI element
        if (!EventSystem.current.IsPointerOverGameObject()) {
          Ray ray = Camera.main.ScreenPointToRay(Input.mousePosition);
          Vector3 groundPlaneIntersect;

          if (GroundPlaneTarget(ray, out groundPlaneIntersect)) {
            // Find a path to the point clicked.
            PathTo(groundPlaneIntersect);
          }
        }
      }
    }

    /// <summary>
    /// Utility function to detect a touch on the targeted plane.
    /// </summary>
    /// <param name="ray"></param>
    /// <param name="hitPoint"></param>
    /// <returns></returns>
    private bool GroundPlaneTarget(Ray ray, out Vector3 hitPoint) {
      float distance;
      bool intersects = GroundPlane.Raycast(ray, out distance);

      if (intersects) {
        hitPoint = ray.origin + ray.direction * distance;
      } else {
        hitPoint = Vector3.zero;
      }

      return intersects;
    }
  }
}
