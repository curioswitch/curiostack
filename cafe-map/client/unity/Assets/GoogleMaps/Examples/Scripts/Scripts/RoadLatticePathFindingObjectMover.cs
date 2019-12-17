using System;
using UnityEngine;
using UnityEngine.Events;

/// <summary>
/// A class that manages a pair of objects; handling clicks by alternately positioning the
/// <see cref="StartObject"/> and <see cref="EndObject"/>; and invoking the
/// <see cref="PathUpdateEvent"/> when either is moved.
/// </summary>
public class RoadLatticePathFindingObjectMover : MonoBehaviour {
  /// <summary>
  /// Representation of the current interaction state: positioning the start or end object.
  /// </summary>
  private enum State {
    PositioningStart,
    PositioningEnd
  }

  /// <summary>
  /// UnityEvent type used to communicate the repositioning of path start and end objects.
  /// </summary>
  [Serializable]
  public class PathUpdateEvent : UnityEvent<GameObject, GameObject> {}

  [Tooltip("Unity event invoked when path start or end object is repositioned")]
  public PathUpdateEvent UpdatedPositions = new PathUpdateEvent();

  [Tooltip("Object marking the start position for path finding.")]
  public GameObject StartObject;

  [Tooltip("Object marking the end position for path finding.")]
  public GameObject EndObject;

  [Tooltip("Camera used to project mouse clicks onto the ground plane.")]
  public Camera Camera;

  /// <summary>
  /// The plane used for ground position detection of clicks.
  /// </summary>
  private Plane GroundPlane = new Plane(Vector3.up, 0);

  /// <summary>
  /// Current interactions state. Governs behaviour of clicks.
  /// </summary>
  private State CurrentState = State.PositioningStart;

  /// <summary>
  /// Processes mouse clicks.
  /// </summary>
  public void Update() {
    if (Input.GetMouseButtonDown(0)) {
      Ray ray = Camera.ScreenPointToRay(Input.mousePosition);
      Vector3 groundPlaneIntersect;
      if (GroundPlaneTarget(ray, out groundPlaneIntersect)) {
        ProcessClick(groundPlaneIntersect);
      }
    }
  }

  /// <summary>
  /// Projects the supplied ray onto the ground plane. Throws an exception if the supplied ray
  /// doesn't intersect the ground plane.
  /// </summary>
  /// <param name="ray">The ray to project</param>
  /// <param name="hitPoint">The intersection with the ground plane, or zero</param>
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

  /// <summary>
  /// Processes clicks on the ground plane based on <see cref="CurrentState"/>, alternately
  /// setting the position of the path start and path end objects.
  /// </summary>
  /// <param name="groundPlaneIntersect"></param>
  private void ProcessClick(Vector3 groundPlaneIntersect) {
    switch (CurrentState) {
      case State.PositioningStart:
        StartObject.transform.position = groundPlaneIntersect;
        CurrentState = State.PositioningEnd;
        break;
      case State.PositioningEnd:
        EndObject.transform.position = groundPlaneIntersect;
        CurrentState = State.PositioningStart;
        break;
    }

    UpdatedPositions.Invoke(StartObject, EndObject);
  }
}
