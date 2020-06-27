using Google.Maps.Coord;
using System;
using System.Collections;
using UnityEngine;
using UnityEngine.Events;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Script for keeping keeping the <see cref="Camera.main"/>'s viewport loaded at all times.
  /// </summary>
  /// <remarks>
  /// By default loads Melbourne, Australia. If a new latitude/longitude is set in Inspector (before
  /// pressing start), will load new location instead.
  /// </remarks>
  [RequireComponent(typeof(MapsService))]
  public sealed class DynamicMapsService : MonoBehaviour {
    ///< summary>Interval in seconds at which unseen geometry is detected and unloaded.</summary>
    private const float UnloadUnseenDelay = 5f;

    [Tooltip("LatLng to load (must be set before hitting play).")]
    public LatLng LatLng = new LatLng(-37.8110057, 144.9601189);

    [Tooltip(
        "Maximum distance to render to (prevents loading massive amount of geometry if looking" +
        "up at the horizon).")]
    public float MaxDistance = 1000f;

    [Tooltip(
        "The ground plane. We keep this centered underneath Camera.main, so as we move around " +
        "the game world the ground plane stays always underneath us. As such the Material " +
        "applied to this ground plane should either be untextured, or textured using worldspace " +
        "coordinates (as opposed to local uv coordinates), so that we cannot actually see the " +
        "ground plane moving around the world, creating the illusion that there is always ground " +
        "beneath us.")]
    public GameObject Ground;

    [Tooltip("Invoked when the map is initialized and has started loading.")]
    public UnityEvent OnMapLoadStarted = new UnityEvent();

    [Header("Read Only"), Tooltip("Is geometry currently being loaded?")]
    public bool Loading;

    /// <summary>The <see cref="GameObjectOptions"/> to use when rendering loaded
    /// geometry.</summary> <remarks> This value must be overriden before this script's <see
    /// cref="Start"/> function is called in order to render loaded geometry with a different set of
    /// <see cref="GameObjectOptions"/>. If no change is made, <see
    /// cref="ExampleDefaults.DefaultGameObjectOptions"/> will be used instead.
    /// </remarks>
    public GameObjectOptions RenderingStyles;

    /// <summary>Required <see cref="MapsService"/> component.</summary>
    /// <remarks>
    /// This component is auto-found on first access (so this component can be accessed by an
    /// external script at any time without a null-reference exception).
    /// </remarks>
    public MapsService MapsService {
      get {
        return _MapsService ?? (_MapsService = GetComponent<MapsService>());
      }
    }

    /// <summary>
    /// Required <see cref="MapsService"/> component.
    /// </summary>
    private MapsService _MapsService;

    /// <summary>
    /// Position of <see cref="Camera.main"/> last frame. We use this to see if the view has moved
    /// this frame.
    /// </summary>
    private Vector3 CameraPosition;

    /// <summary>
    /// Euler angles of <see cref="Camera.main"/> last frame. We use this to see if the view has
    /// rotated this frame.
    /// </summary>
    private Quaternion CameraRotation;

    /// <summary>
    /// Do we need to restart coroutines when the component is next enabled?
    /// </summary>
    private bool RestartCoroutinesOnEnable;

    /// <summary>
    /// Handle to coroutine used to remove unneeded areas of the map.
    /// </summary>
    private Coroutine UnloadUnseenCoroutine;

    /// <summary>
    /// Setup this script if have not done so already.
    /// </summary>
    private void Start() {
      // Verify all required parameters are defined and correctly setup, skipping any further setup
      // if any parameter is missing or invalid.
      if (!VerifyParameters()) {
        // Disable this script to prevent error spamming (where Update will producing one or more
        // errors every frame because one or more parameters are undefined).
        enabled = false;

        return;
      }

      // Move the Ground plane to be directly underneath the main Camera. We do this again whenever
      // the main Camera moves.
      ReCenterGround();

      // Set real-world location to load. Note that the MapsService variable is auto-found on first
      // access.
      MapsService.InitFloatingOrigin(LatLng);

      // Make sure we have a set of GameObjectOptions to render loaded geometry with, using defaults
      // if no specific set of options has been given. This allows a different set of options to be
      // used, e.g. with Road Borders enabled, provided these new options  are set into this
      // parameter before this Start function is called.
      if (RenderingStyles == null) {
        RenderingStyles = ExampleDefaults.DefaultGameObjectOptions;
      }

      // Connect to Maps Service error event so we can be informed if an error occurs while trying
      // to load tiles. However, if this GameObject also contains an Error Handling Component, then
      // we skip handling errors here, leaving it to the Error Handling Component instead.
      if (GetComponent<ErrorHandling>() == null) {
        MapsService.Events.MapEvents.LoadError.AddListener(args => {
          if (args.Retry) {
            Debug.LogWarning(args);
          } else {
            Debug.LogError(args);
          }
        });
      }

      // Revert loading flag to false whenever loading finishes (this flag is set to true whenever
      // loading starts, and so it remain true until the currently requested geometry has finished
      // loading).
      MapsService.Events.MapEvents.Loaded.AddListener(args => Loading = false);

      // Load the current viewport.
      RefreshView();

      // Now load map around the camera.
      MapsService.MakeMapLoadRegion()
          .AddCircle(new Vector3(CameraPosition.x, 0f, CameraPosition.z), MaxDistance)
          .Load(RenderingStyles);

      StartCoroutines();

      if (OnMapLoadStarted != null) {
        OnMapLoadStarted.Invoke();
      }
    }

    /// <summary>
    /// Start any coroutines needed by this component.
    /// </summary>
    private void StartCoroutines() {
      // Run a coroutine to clean up unseen objects.
      UnloadUnseenCoroutine = StartCoroutine(UnloadUnseen());
    }

    /// <summary>
    /// Handle Unity OnDisable event.
    /// </summary>
    private void OnDisable() {
      if (UnloadUnseenCoroutine != null) {
        StopCoroutine(UnloadUnseenCoroutine);
        UnloadUnseenCoroutine = null;
        RestartCoroutinesOnEnable = true;
      }
    }

    /// <summary>
    /// Handle Unity OnEnable event.
    /// </summary>
    private void OnEnable() {
      if (RestartCoroutinesOnEnable) {
        StartCoroutines();
        RestartCoroutinesOnEnable = false;
      }
    }

    /// <summary>
    /// Check if the main Camera has moved or rotated each frame, recentering the ground-plane and
    /// refreshing the viewed area as required.
    /// </summary>
    private void Update() {
      // If the main Camera has moved this frame, we re-center the ground-plane underneath it, and
      // refresh the part of the world the moved main Camera now sees.
      if (Camera.main.transform.position != CameraPosition) {
        ReCenterGround();
        RefreshView();

        return;
      }

      // If the main Camera has not moved, but has rotated this frame, we refresh the part of the
      // world the rotated main Camera now sees.
      if (Camera.main.transform.rotation != CameraRotation) {
        RefreshView();
      }
    }

    /// <summary>
    /// Recenter the Floating Origin based on a given <see cref="Camera"/>'s position.
    /// <para>
    /// This allows the world to be periodically recentered back to the origin, which avoids
    /// geometry being created with increasingly large floating point coordinates, ultimately
    /// resulting in floating point rounding errors.
    /// </para></summary>
    /// <param name="camera">
    /// <see cref="Camera"/> to use to recenter world.
    /// <para>
    /// This <see cref="Camera"/> will be moved until it is over the origin (0f, 0f, 0f). At the
    /// same time all geometry created by the <see cref="MapsService"/> will be moved the same
    /// amount. The end result is that the world is recentered over the origin, with the change
    /// being unnoticeable to the player.
    /// </para></param>
    internal Vector3 RecenterWorld(Camera camera) {
      // The Camera's current position is given to the MoveFloatingOrigin function, along with the
      // Camera itself, so that the world and the Camera can all be moved until the Camera is over
      // the origin again. Note that the MoveFloatingOrigin function automatically moves all loaded
      // geometry, so the only extra geometry that needs moving is the given camera (given as the
      // second, optional parameter of this function).
      return MapsService.MoveFloatingOrigin(camera.transform.position, new[] { camera.gameObject });
    }

    /// <summary>
    /// Move the ground plane directly underneath the <see cref="Camera.main"/>.
    /// </summary>
    private void ReCenterGround() {
      // Store the position of the main Camera, so we can check next frame if the main Camera has
      // moved, and thus if the ground plane needs to be recentered.
      CameraPosition = Camera.main.transform.position;
      Ground.transform.position = new Vector3(CameraPosition.x, 0f, CameraPosition.z);
    }

    /// <summary>
    /// Reload the world, making sure the area <see cref="Camera.main"/> can see it loaded.
    /// </summary>
    private void RefreshView() {
      // Store the rotation of the camera, so we can check next frame if the main Camera has
      // rotated, and thus if the visible world should be refreshed again.
      CameraRotation = Camera.main.transform.rotation;
      float height = Camera.main.transform.position.y;

      // Flag that we are now loading geometry.
      Loading = true;

      // Load the visible map region. The range is increased based on the height of the camera
      // to ensure we have a circle of radius MaxDistance on the ground.
      float maxDistance = (float) Math.Sqrt(Math.Pow(height, 2) + Math.Pow(MaxDistance, 2));
      MapsService.MakeMapLoadRegion()
          .AddViewport(Camera.main, maxDistance)
          .Load(RenderingStyles);
    }

    /// <summary>Periodically remove unneeded areas of the map.</summary>
    private IEnumerator UnloadUnseen() {
      while (true) {
        // Unload map regions that are not in viewport, and are outside a radius around the camera.
        // This is to avoid unloading geometry that may be reloaded again very shortly (as it is
        // right on the edge of the view).
        MapsService.MakeMapLoadRegion()
            .AddViewport(Camera.main, MaxDistance)
            .AddCircle(new Vector3(CameraPosition.x, 0f, CameraPosition.z), MaxDistance)
            .UnloadOutside();

        // Wait for a preset interval before seeing if new geometry needs to be unloaded.
        yield return new WaitForSeconds(UnloadUnseenDelay);
      }
    }

    /// <summary>
    /// Verify that all required parameters have been correctly defined, returning false if not.
    /// </summary>
    private bool VerifyParameters() {
      // TODO(b/149056787): Standardize parameter verification across scripts.
      // Verify that a Ground plane has been given.
      if (Ground == null) {
        Debug.LogError(ExampleErrors.MissingParameter(this, Ground, "Ground"));

        return false;
      }

      // Verify that there is a Camera.main in the scene (i.e. a Camera that is tagged:
      // "MainCamera").
      if (Camera.main == null) {
        Debug.LogError(ExampleErrors.NullMainCamera(this));

        return false;
      }

      // If have reached this point then we have verified all required parameters.
      return true;
    }
  }
}
