using System;
using UnityEngine;

/// <summary>
/// A primitive camera controller using a multi-touch interface.
/// <para>
/// Supports dragging with a single finger, rotating with two fingers and zooming using pinch
/// gestures.
/// </para>
/// <para>
/// Rotation and zooming are focused on the intersection of the camera line with the ground plane,
/// with rotation occuring around the Vector3.Up axis from this point. Zooming is asymptotic, so it
/// is not possible to zoom through the ground plane.
/// </para>
/// <para>
/// The controlled camera is selected through GetComponent{Camera} on the GameObject to which this
/// script component is attached.
/// </para>
/// </summary>
public class TouchCameraController : MonoBehaviour {
  /// <summary>
  /// Cached version of controlled camera reference, to avoid multiple GetComponent calls.
  /// </summary>
  private Camera MainCamera;

  /// <summary>
  /// The plane used for intersection calculations to determine rotation and zoom points.
  /// </summary>
  private Plane GroundPlane;

  #region Single finger drag state fields.
  /// <summary>
  /// Indicates if a drag action in currently in progress.
  /// </summary>
  private bool Dragging;

  /// <summary>
  /// The previous drag touch position as projected onto the ground plane.
  /// </summary>
  private Vector3 PrevGroundPos;
  #endregion

  #region Zoom state fields.
  /// <summary>
  /// Indicates a zoom action is in progress.
  /// </summary>
  private bool Zooming;

  /// <summary>
  /// The previous distance between pinch gesture fingers in screen coordinates.
  /// </summary>
  private float PrevSeparation;
  #endregion

  #region Rotation state fields.
  /// <summary>
  /// Indicates a rotation gesture is in progress.
  /// </summary>
  private bool Rotating;

  /// <summary>
  /// The previous angle subtended by the two fingers in the active rotation gesture, in screen
  /// coordinates from the screen horizontal axis.
  /// </summary>
  private float PrevAngle;
  #endregion

  #region Shared fields for rotation and zooming.
  /// <summary>
  /// Finger ID of the first finger reported by the touch system on the previous frame. Used to
  /// track if different fingers have been placed on the screen, and prevent odd jumps in camera
  /// control.
  /// </summary>
  private int PrevFingerId0;

  /// <summary>
  /// Finger ID of the second finger reported by the touch system on the previous frame. Used to
  /// track if different fingers have been placed on the screen, and prevent odd jumps in camera
  /// control.
  /// </summary>
  private int PrevFingerId1;
  #endregion

  void Start () {
    MainCamera = GetComponent<Camera>();
    GroundPlane = new Plane(Vector3.up, 0);
  }

  /// <summary>
  /// Returns the angle subtended from the screen horizontal axis by the line joining the first
  /// and second touch points.
  /// </summary>
  /// <returns>The angle.</returns>
  /// <param name="touches">Touches.</param>
  private float TouchAngle(Touch[] touches) {
    if (touches.Length != 2) {
      throw new InvalidOperationException("TouchAngle must be called with exactly 2 touch points");
    }
    Vector2 p0 = touches[0].position;
    Vector2 p1 = touches[1].position;
    Vector2 delta = p1 - p0;
    return Mathf.Atan2(delta.y, delta.x);
  }

  /// <summary>
  /// Process touch state: if a single finger is touching the screen, try to process a drag; if two
  /// fingers, try to process zoom and rotation gestures.
  /// </summary>
  void Update () {
    Touch[] touches = Input.touches;
    if (touches.Length == 1) {
      // Cancel any in progress zoom or rotate as we only have one finger down.
      Zooming = false;
      Rotating = false;
      UpdateSingleTouch(touches);
    }
    if (touches.Length == 2) {
      // Any two finger gesture cancels any in progress drag.
      Dragging = false;
      UpdateTwoFingerZoom(touches);
      UpdateTwoFingerRotate(touches);
    }
  }

  /// <summary>
  /// Returns the projection of the camera line of sight onto the ground plane.
  /// </summary>
  private Vector3 GroundPlaneCameraTarget() {
    Ray ray = new Ray(MainCamera.transform.position, MainCamera.transform.forward);
    return GroundPlaneTarget(ray);
  }

  /// <summary>
  /// Projects the supplied ray onto the ground plane. Throws an exception if the supplied ray
  /// doesn't intersect the ground plane.
  /// </summary>
  private Vector3 GroundPlaneTarget(Ray ray) {
    float distance;
    bool intersects = GroundPlane.Raycast(ray, out distance);
    Debug.Assert(intersects);
    return ray.origin + ray.direction * distance;
  }

  /// <summary>
  /// Process the supplied touch state as a zoom gesture.
  /// </summary>
  private void UpdateTwoFingerZoom(Touch[] touches) {
    if (touches.Length != 2) {
      throw new InvalidOperationException();
    }

    // Reset the zoom state if: a finger has just touched the screen, or if different fingers are
    // now touching the screen from those in the previous invocation.
    if (touches[0].phase == TouchPhase.Began || touches[1].phase == TouchPhase.Began
      || !Zooming || touches[0].fingerId != PrevFingerId0 || touches[1].fingerId != PrevFingerId1) {
      PrevSeparation = Vector2.Distance(touches[0].position, touches[1].position);
      PrevFingerId0 = touches[0].fingerId;
      PrevFingerId1 = touches[1].fingerId;
      Zooming = true;
      return;
    }

    // If either finger has moved, use the inverse ratio of the finger separation distance from this
    // invocation and the previous invocation to scale the camera distance from the ground plane.
    // This achieves asymptotic zooming towards the ground plane.
    if (touches[0].phase == TouchPhase.Moved && touches[1].phase == TouchPhase.Moved) {
      float separation = Vector2.Distance(touches[0].position, touches[1].position);
      float scale = separation / PrevSeparation;
      Vector3 groundPos = GroundPlaneCameraTarget();
      float currentGroundPlaneDistance = Vector3.Distance(MainCamera.transform.position, groundPos);
      float delta = currentGroundPlaneDistance  * (1.0f - 1.0f /scale);
      MainCamera.transform.parent.Translate(MainCamera.transform.forward * delta, Space.World);
      PrevSeparation = separation;
    }

    Zooming = false;
    return;
  }

  /// <summary>
  /// Processes the supplied touches as a rotate gesture.
  /// </summary>
  private void UpdateTwoFingerRotate(Touch[] touches) {
    if (touches.Length != 2) {
      throw new InvalidOperationException();
    }

    // Reset the rotation state if: a finger has just touched the screen, or if different fingers
    // are now touching the screen from those in the previous invocation.
    if (touches[0].phase == TouchPhase.Began || touches[1].phase == TouchPhase.Began || !Rotating
        || touches[0].fingerId != PrevFingerId0 || touches[1].fingerId != PrevFingerId1) {
      PrevAngle = TouchAngle(touches);
      PrevFingerId0 = touches[0].fingerId;
      PrevFingerId1 = touches[1].fingerId;
      Rotating = true;
      return;
    }

    // If either finger has moved, calculate the screen space angle change of the line joining the
    // touch points and use this as the angle of rotation around the vertical axis intersecting the
    // ground plane target point of the camera line of sight.
    if (touches[0].phase == TouchPhase.Moved && touches[1].phase == TouchPhase.Moved) {
      float angle = TouchAngle(touches);
      float angleDelta = Mathf.Rad2Deg * (angle - PrevAngle);
      PrevAngle = angle;
      Vector3 groundPlaneTarget = GroundPlaneCameraTarget();
      MainCamera.transform.parent.RotateAround(groundPlaneTarget, Vector3.up, angleDelta);
    }

    Rotating = false;
    return;
  }

  private void UpdateSingleTouch(Touch[] touches) {
    Vector2 pos = touches[0].position;
    Vector3 touchPoint =
        MainCamera.ScreenToWorldPoint(new Vector3(pos.x, pos.y, MainCamera.nearClipPlane));
    Ray worldRay =
        new Ray(MainCamera.transform.position, touchPoint - MainCamera.transform.position);
    Vector3 groundPos = GroundPlaneTarget(worldRay);

    // Start drag if the screen was just touched, or a different finger is used.
    if (touches[0].phase == TouchPhase.Began  || touches[0].fingerId != PrevFingerId0
        || !Dragging) {
      PrevFingerId0 = touches[0].fingerId;
      PrevGroundPos = groundPos;
      Dragging = true;
      return;
    }

    if (touches[0].phase == TouchPhase.Ended) {
      Dragging = false;
      return;
    }

    // Move the camera in the opposite direction to the change in contact point, to effect a
    // dragging-the-world gesture.
    if (touches[0].phase == TouchPhase.Moved) {
      Debug.Assert(Dragging);
      Vector3 delta = groundPos - PrevGroundPos;
      MainCamera.transform.parent.Translate(-delta, Space.World);

      // We need to recalculate the current contact point to accomodate the camera move we just
      // performed. Failure to do this leads to incorrect delta calculation, as weird stuttering.
      touchPoint =
        MainCamera.ScreenToWorldPoint(new Vector3(pos.x, pos.y, MainCamera.nearClipPlane));
      worldRay = new Ray(MainCamera.transform.position, touchPoint - MainCamera.transform.position);
      PrevGroundPos = GroundPlaneTarget(worldRay);
    }
  }
}
