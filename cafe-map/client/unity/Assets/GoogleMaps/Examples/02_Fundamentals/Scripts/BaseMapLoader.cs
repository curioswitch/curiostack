using UnityEngine;
using UnityEngine.Events;
using Google.Maps.Coord;
using Google.Maps.Event;
using Google.Maps.Examples.Shared;

namespace Google.Maps.Examples {
  /// <summary>
  /// This script provides a template for loading a maps area.
  /// It relies on <see cref="MapsService"/> for the communication with the Maps SDK.
  ///
  /// Before loading a map, you need to:
  /// - setup a Latitude/Longitude origin
  /// - provide styling options (which will be applied as the plugin creates map features
  /// <see cref="GameObject"/>s.
  /// - add all event listeners needed to apply additional functionality to the loaded geometry
  /// (augmented geometry, pathfinding, etc...).
  /// - monitor possible loading errors
  ///
  /// This script can be reused as-is or derived to enrich the initial setup.
  ///
  /// We also provide a series of "Updater" scripts that can be build upon this loader, and enrich
  /// the loaded map with more functionality.
  ///
  /// </summary>
  public class BaseMapLoader : MonoBehaviour {
    [Tooltip("LatLng to load (must be set before hitting play).")]
    public LatLng LatLng = new LatLng(40.6892199, -74.044601);

    /// <summary>
    /// The <see cref="MapsService"/> is the entry point to communicate with to the Maps SDK for
    /// Unity. It provides apis to load map regions, and dispatches events throughout the loading
    /// process. These events, in particular WillCreate/DidCreate events provide a finer control on
    /// each loaded map feature.
    /// </summary>
    [Tooltip("Required maps service (used to communicate with the Maps plugin).")]
    public MapsService MapsService;

    [Tooltip(
        "Maximum distance to render to (prevents loading massive amount of geometry if looking" +
        "up at the horizon).")]
    public float MaxDistance = 1000f;

    /// <summary>The <see cref="GameObjectOptions"/> to use when rendering loaded
    /// geometry.</summary> <remarks> This value must be overriden before this script's <see
    /// cref="Start"/> function is called in order to render loaded geometry with a different set of
    /// <see cref="GameObjectOptions"/>. If no change is made, <see
    /// cref="ExampleDefaults.DefaultGameObjectOptions"/> will be used instead.
    /// </remarks>
    [Header("Read Only"), Tooltip("Rendering styles currently set.")]
    public GameObjectOptions RenderingStyles;

    [Header("Read Only"),
     Tooltip("What's the state of the map loader: is geometry currently being loaded?")]
    public bool IsLoading;

    [Header("Read Only"), Tooltip("Is the map service initialized?")]
    public bool IsInitialized;

    [Tooltip("Triggers a loading of the map on start.")]
    public bool LoadOnStart = true;

    /// <summary>
    /// Whether the application has quit;
    /// </summary>
    private bool HasQuit;

    /// <summary>
    /// This event is dispatched before starting the map loading.
    /// It can be used to notify other game systems that we are about to load a new map (or a
    /// portion of it).
    /// </summary>
    [Tooltip("This event is dispatched just before calling the loading function.")]
    public UnityEvent MapLoadingStartedEvent;

    [Tooltip(
        "This event is dispatched when the map is cleared. " +
        "This is useful if the geometry has been dispatched to other parent objects than Maps " +
        "Service.")]
    public UnityEvent MapClearedEvent;

    void Awake() {
      if (MapsService == null) {
        throw new System.Exception("Maps Service is required for loading maps data.");
      }
    }

    /// <summary>
    /// In this example, the map setup and loading are done as soon as the loader becomes active in
    /// the scene.
    /// </summary>
    void Start() {
      InitFloatingOrigin();
      InitStylingOptions();
      InitEventListeners();
      InitErrorHandling();

      IsInitialized = true;

      if (LoadOnStart) {
        LoadMap();
      }
    }

    void OnApplicationQuit() {
      HasQuit = true;
    }

    /// <summary>
    /// This defines the origin of the loaded map.
    /// </summary>
    protected virtual void InitFloatingOrigin() {
      if (MapsService == null) {
        return;
      }

      // Set real-world location to load.
      MapsService.InitFloatingOrigin(LatLng);
    }

    /// <summary>
    /// Make sure we have a set of GameObjectOptions to render the loaded geometry with.
    /// It uses defaults settings if no specific set of options has been given.
    /// This allows a different set of options to be used, e.g. with Road Borders enabled.
    /// Styling options must be set prior to loading the map, but can also be fine-tuned in
    /// WillCreate/DidCreate events.
    /// </summary>
    protected virtual void InitStylingOptions() {
      if (RenderingStyles == null) {
        RenderingStyles = ExampleDefaults.DefaultGameObjectOptions;
      }
    }

    /// <summary>
    /// Provides basic errors monitoring while the map is loaded.
    /// </summary>
    protected virtual void InitErrorHandling() {
      if (MapsService == null) {
        return;
      }

      // Sign up to event called whenever an error occurs. Note that this event must be set now
      // during Awake, so that when Dynamic Maps Service starts loading the map during Start, this
      // event will be triggered on any error.
      MapsService.Events.MapEvents.LoadError.AddListener(args => {
        // Check for the most common errors, showing specific error message in these cases.
        switch (args.DetailedErrorCode) {
          case MapLoadErrorArgs.DetailedErrorEnum.NetworkError:
            // Handle errors caused by a lack of internet connectivity (or other network problems).
            if (Application.internetReachability == NetworkReachability.NotReachable) {
              Debug.LogError("The Maps SDK for Unity must have internet access in order to run.");
            } else {
              Debug.LogErrorFormat(
                  "The Maps SDK for Unity was not able to get a HTTP response after " +
                      "{0} attempts.\nThis suggests an issue with the network, or with the " +
                      "online Semantic Tile API, or that the request exceeded its deadline  " +
                      "(consider using MapLoadErrorArgs.TimeoutSeconds).\n{1}",
                  args.Attempts,
                  string.IsNullOrEmpty(args.Message)
                      ? string.Concat("Specific error message received: ", args.Message)
                      : "No error message received.");
            }

            return;

          // Handle errors caused by the specific version of the Maps SDK for Unity being used.
          case MapLoadErrorArgs.DetailedErrorEnum.UnsupportedClientVersion:
            Debug.LogError(
                "The specific version of the Maps SDK for Unity being used is no longer " +
                "supported (possibly in combination with the specific API key used).");

            return;
        }

        // For all other types of errors, just show the given error message, as this should describe
        // the specific nature of the problem.
        Debug.LogError(args.Message);

        // Note that the Maps SDK for Unity will automatically retry failed attempts, unless
        // args.Retry is specifically set to false during this callback.
      });
    }

    /// <summary>
    /// Typically, Map loading event listeners provide a way to:
    /// - influence the way map features are loaded by listening to WillCreate events;
    /// - extract loaded data atomically to use in gameplay by listening with DidCreate events;
    /// In these Maps examples, we provide a series of "Updater" scripts to enrich or alter our maps
    /// geometry at runtime, by adding or removing different event listeners.
    /// Note that these event listeners can also be added directly to the  component in the Editor.
    /// </summary>
    protected virtual void InitEventListeners() {
      if (MapsService == null) {
        return;
      }

      // Revert loading flag to false whenever loading finishes (this flag is set to true whenever
      // loading starts, and so it remain true until the currently requested geometry has finished
      // loading).
      MapsService.Events.MapEvents.Loaded.AddListener(args => IsLoading = false);
    }

    /// <summary>
    /// Starts the loading of a Map Region defined by the viewport located at MaxDistance from the
    /// main Camera.
    /// </summary>
    public virtual void LoadMap() {
      if (MapsService == null || RenderingStyles == null || !IsInitialized || Camera.main == null) {
        return;
      }

      // Don't load if the application has quit and we're not in edit-time preview mode.
      if (HasQuit && (Application.isPlaying || !MapsService.MapPreviewOptions.Enable)) {
        return;
      }

      // Set the loading flag to true before loading.
      // This flag is really used to let users of the map loader know about its current state.
      IsLoading = true;

      // Notify all listeners that we are starting to load the map
      if (MapLoadingStartedEvent != null) {
        MapLoadingStartedEvent.Invoke();
      }

      // Load the visible map region.
      MapsService.MakeMapLoadRegion()
          .AddViewport(Camera.main, MaxDistance)
          .Load(RenderingStyles);
    }

    /// <summary>
    /// Notifies the sdk that we are destroying geometry so that it can be reloaded from the apis
    /// correctly.
    /// This function also destroy the <see cref="GameObject"/>s in the scene.
    /// The MapsService keeps an internal cache of <see cref="GameObject"/>s, and their
    /// relationships to MapFeatures. When these <see cref="GameObject"/>s are destroyed in the
    /// scene through gameplay for example, it is a best practice to notify the MapsService about
    /// the changes. MapsService will then update its dictionary of MapFeatures, and will trigger
    /// the appropriate WillCreate/DidCreate MapEvents next time a call to MakeMapLoadRegion is
    /// called. It won't invoke these events if the associated map features are still in its cache.
    /// </summary>
    public virtual void ClearMap() {
      // Don't clear if the application has quit and we're not in edit-time preview mode.
      if (HasQuit && (Application.isPlaying || !MapsService.MapPreviewOptions.Enable)) {
        return;
      }

      if (MapsService.GameObjectManager != null) {
        MapsService.GameObjectManager.DestroyAll();
      }

      foreach (Transform child in MapsService.transform) {
        Destroy(child.gameObject);
      }
    }
  }
}
