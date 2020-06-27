using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Events;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Component to update the <see cref="Google.Maps.MapsService"/>'s Floating Origin whenever the
  /// <see cref="Camera.main"/> moves far enough.
  /// <para>
  /// The Floating Origin is used to periodically recenter the world, moving the world until the
  /// player is back at the origin (0f, 0f, 0f). The prevents geometry being created with
  /// increasingly large floating point coordinates, ultimately resulting in floating point rounding
  /// errors.
  /// </para></summary>
  public sealed class FloatingOriginUpdater : MonoBehaviour {
    /// <summary>The last set floating origin.</summary>
    public Vector3 FloatingOrigin { get; private set; }

    /// <summary>
    /// Optional <see cref="UnityEvent"/> called whenever this script updates the world's Floating
    /// Origin.
    /// </summary>
    /// <remarks>Passes in the amount the Floating Origin was moved by.</remarks>
    public OriginEvent OnFloatingOriginUpdate = new OriginEvent();

    [Tooltip("Script for controlling Camera movement. Used to detect when the Camera has moved.")]
    public CameraController CameraController;

    [Tooltip(
        "Distance in meters the Camera should move before the world's Floating Origin is " +
        "reset. This value must be positive.")]
    public float FloatingOriginRange = 200f;

    [Tooltip("Should a debug message be shown whenever the Floating Origin is re-centered?")]
    public bool DebugFloatingOrigin = true;

    /// <summary>
    /// The <see cref="MapsService"/> to update the floating origin of.
    /// </summary>
    public MapsService MapsService;

    /// <summary>
    /// All <see cref="GameObject"/>s to be moved when the world's Floating Origin is moved.
    /// </summary>
    /// <remarks>
    /// If this array is not set by calling <see cref="SetAdditionalGameObjects"/>, then this array
    /// is initialized with <see cref="Camera.main"/> during <see cref="Awake"/>. This is so, by
    /// default, the scene's <see cref="Camera"/> is moved when the Floating Origin is recentered,
    /// resulting in a seamless recentering of the world that should be invisible to the user.
    /// </remarks>
    private GameObject[] AdditionalGameObjects;

    /// <summary>
    /// Use <see cref="CameraController"/>'s OnMove event to detect when the <see cref="Camera"/>
    /// has moved far enough that the Floating Origin needs to be recentered.
    /// </summary>
    private void Awake() {
      if (MapsService == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, MapsService, "Maps Service", "is required for this script to work."));

        return;
      }

      // Verify a Camera Controller has been given.
      if (CameraController == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, CameraController, "Camera Controller", "to tell when the Camera has moved"));

        return;
      }

      // Verify that a valid Floating Origin range was given, i.e. that given distance was not
      // negative nor zero. Comparison is made to float.Epsilon instead of zero to account for float
      // rounding errors.
      if (FloatingOriginRange <= float.Epsilon) {
        Debug.LogError(ExampleErrors.NotGreaterThanZero(
            this,
            FloatingOriginRange,
            "Floating Origin Range",
            "to tell how far the Camera should move before the Floating " + "Origin is reset"));

        return;
      }

      // Store the initial position of the Camera on the ground plane.
      FloatingOrigin = GetCameraPositionOnGroundPlane();

      // If no additional GameObjects have been set (to be moved when the world's Floating Origin is
      // recentered), set this array to be just Camera.main's GameObject. This is so that, by
      // default, the scene's Camera is moved when the world is recentered, resulting in a seamless
      // recentering of the world that should be invisible to the user.
      if (AdditionalGameObjects == null) {
        AdditionalGameObjects = new[] { Camera.main.gameObject };
      }
    }

    private Vector3 GetCameraPositionOnGroundPlane() {
      Vector3 result = Camera.main.transform.position;
      // Ignore the Y value since the floating origin only really makes sense on the ground plane.
      result.y = 0;
      return result;
    }

    /// <summary>
    /// See if <see cref="Camera.main"/> has moved far enough that the world's Floating Origin needs
    /// to be recentered.
    /// </summary>
    /// <param name="moveAmount">
    /// Amount <see cref="Camera.main"/> has moved (not used as this value is recalculated here with
    /// height ignored).
    /// </param>
    private void TryMoveFloatingOrigin(Vector3 moveAmount) {
      if (MapsService == null) {
        return;
      }

      Vector3 newFloatingOrigin = GetCameraPositionOnGroundPlane();
      float distance = Vector3.Distance(FloatingOrigin, newFloatingOrigin);

      // Reset the world's Floating Origin if (and only if) the Camera has moved far enough.
      if (distance < FloatingOriginRange) {
        return;
      }

      // The Camera's current position is given to MapsService's MoveFloatingOrigin function,
      // along with any GameObjects to move along with the world (which will at least be the the
      // Camera itself). This is so that the world, the Camera, and any extra GameObjects can all be
      // moved together, until the Camera is over the origin again. Note that the MoveFloatingOrigin
      // function automatically moves all geometry loaded by the Maps Service.
      Vector3 originOffset =
          MapsService.MoveFloatingOrigin(newFloatingOrigin, AdditionalGameObjects);

      // Use event to inform other classes of change in origin. Note that because this is a Unity
      // Event a null reference exception will not be triggered if no listeners have been added.
      OnFloatingOriginUpdate.Invoke(originOffset);

      // Set the new Camera origin. This ensures that we can accurately tell when the Camera has
      // moved away from this new origin, and the world needs to be recentered again.
      FloatingOrigin = newFloatingOrigin;

      // Optionally print a debug message, saying how much the Floating Origin was moved by.
      if (DebugFloatingOrigin && !Mathf.Approximately(distance, 0)) {
        Debug.LogFormat("Floating Origin moved: world moved by {0}", originOffset);
      }
    }

    /// <summary>
    /// Set an array of <see cref="GameObject"/>s to be moved whenever the world's Floating Origin
    /// is recentered.
    /// </summary>
    /// <remarks>
    /// <see cref="Camera.main"/>'s <see cref="GameObject"/> is automatically added to the given
    /// array of <see cref="GameObject"/>s (if it is not already present), so that by default the
    /// scene's <see cref="Camera"/> is moved when the Floating Origin is recentered, resulting in a
    /// seamless recentering of the world that should be invisible to the user.
    /// </remarks>
    /// <param name="objects">
    /// Array of <see cref="GameObject"/>s to move with the world's Floating Origin.
    /// </param>
    public void SetAdditionalGameObjects(ICollection<GameObject> objects) {
      // Check to see if the main Camera's GameObject is already a part of this given set of
      // GameObjects, adding it if not and storing as the array of GameObjects to move when the
      // world's Floating Origin is recentered.
      GameObject cameraGameObject = Camera.main.gameObject;
      List<GameObject> objectList = new List<GameObject>(objects);

      if (!objects.Contains(cameraGameObject)) {
        objectList.Add(cameraGameObject);
      }

      AdditionalGameObjects = objectList.ToArray();
    }

    void OnEnable() {
      // Whenever the Camera moves, check to see if it has moved far enough that the world's
      // Floating Origin needs to be re-centered.
      CameraController.OnMove.AddListener(TryMoveFloatingOrigin);
    }

    void OnDisable() {
      CameraController.OnMove.RemoveListener(TryMoveFloatingOrigin);
    }

    /// <summary>
    /// Optional <see cref="UnityEvent"/> called every frame this script updates the world's
    /// floating origin.
    /// </summary>
    /// <remarks>Passes in the world's new floating origin.</remarks>
    [Serializable]
    public class OriginEvent : UnityEvent<Vector3> {}
  }
}
