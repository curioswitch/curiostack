using Google.Maps.Coord;
using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Basic example demonstrating how to add the <see cref="MapsService"/> component to this
  /// <see cref="GameObject"/> at runtime, and how to then alter its parameters (like
  /// <see cref="MapsService.ZoomLevel"/>) before its Awake function is called.
  /// </summary>
  /// <remarks>
  /// By default loads the Statue of Liberty. If a new latitude/longitude is set in Inspector
  /// (before pressing start), will load new location instead.
  /// </remarks>
  public sealed class AddingComponent : MonoBehaviour {
    [Tooltip("LatLng to load (must be set before hitting play).")]
    public LatLng LatLng = new LatLng(40.6892199, -74.044601);

    [Tooltip("Zoom level to load.")]
    public int ZoomLevel = 17;

    [Tooltip("Api Key to use on MapsService (must be given).")]
    public string ApiKey;

    [Tooltip(
        "Should an Error Handling component also be added to debug any errors encountered by " +
        "the Maps SDK for Unity when loading geometry?")]
    public bool AddErrorHandling = true;

    /// <summary>
    /// Add a <see cref="MapsService"/> as a component of this <see cref="GameObject"/>.
    /// </summary>
    private void Start() {
      // Verify an Api Key was given.
      if (string.IsNullOrEmpty(ApiKey)) {
        // If no Api Key given, use Api Key checker class to show an error to this effect, and skip
        // the rest of setup.
        ApiKeyChecker.ShowError();

        return;
      }

      // Set this GameObject to be inactive. This is so that when add we MapsService
      // component, its Awake function is not immediately called, giving us a chance
      // to set its parameters.
      gameObject.SetActive(false);

      // Add required Maps Service component to this GameObject.
      MapsService mapsService = gameObject.AddComponent<MapsService>();

      // Set Api Key so MapsService component can download tiles.
      mapsService.ApiKey = ApiKey;

      // Set Zoom Level on MapsService component (usually needs to be set before hitting
      // play, but can also be set in this way before its Awake function is called).
      mapsService.ZoomLevel = ZoomLevel;

      // Re-active this GameObject, which will allow Awake to be called on added
      // MapsService component.
      gameObject.SetActive(true);

      // Set real-world location to load.
      mapsService.InitFloatingOrigin(LatLng);

      // Optionally add a Error Handling component to debug any errors encountered by the Maps SDK
      // for Unity when loading geometry. We must do this now, after the MapsService component has
      // been added but before LoadMap is called.
      if (AddErrorHandling) {
        gameObject.AddComponent<ErrorHandling>();
      }

      // Load map with default options.
      mapsService.LoadMap(ExampleDefaults.DefaultBounds, ExampleDefaults.DefaultGameObjectOptions);
    }
  }
}
