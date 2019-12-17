using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;

/// <summary>
/// Basic example, demonstrating basic usage of the Maps Unity SDK.
/// </summary>
/// <remarks>
/// By default loads the Statue of Liberty. If a new latitude/longitude is set in Inspector (before
/// pressing start), will load new location instead.
/// </remarks>
[RequireComponent(typeof(MapsService))]
public sealed class BasicExample : MonoBehaviour {
  [Tooltip("LatLng to load (must be set before hitting play).")]
  public LatLng LatLng = new LatLng(40.6892199, -74.044601);

  /// <summary>
  /// Use <see cref="MapsService"/> to load geometry.
  /// </summary>
  private void Start () {
    // Get required Maps Service component on this GameObject.
    MapsService mapsService = GetComponent<MapsService>();

    // Set real-world location to load.
    mapsService.InitFloatingOrigin(LatLng);

    // Load map with default options.
    mapsService.LoadMap(ExampleDefaults.DefaultBounds, ExampleDefaults.DefaultGameObjectOptions);
  }
}
