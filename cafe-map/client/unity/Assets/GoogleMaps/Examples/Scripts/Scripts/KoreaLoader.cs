using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;

/// <summary>Script for loading Korea at zoom 6.</summary>
/// <remarks>
/// This is a demonstration of multiple zoom levels in the same MapService. Google Maps does not
/// provide any data past zoom level 6 for Korea, so we load this at zoom 6 to avoid holes in the
/// map.
/// <para>
/// Uses <see cref="DynamicMapsService"/> component to allow navigation around the world, with the
/// <see cref="MapsService"/> component keeping only the viewed part of the world loaded at all
/// times.
/// </para>
/// Uses <see cref="ErrorHandling"/> component to display any errors encountered by the
/// <see cref="MapsService"/> component when loading geometry.
/// </remarks>
[RequireComponent(typeof(DynamicMapsService), typeof(ErrorHandling))]
public sealed class KoreaLoader : MonoBehaviour {
  /// <summary>Roughly the geographic center of South Korea.</summary>
  private static readonly LatLng KoreaCenter = new LatLng(36.346346, 127.950037);

  /// <summary>
  /// Get the required <see cref="DynamicMapsService"/> on this <see cref="GameObject"/>, waiting
  /// until is has initialized before loading a 300km circle around the center of Korea at zoom
  /// level 6.
  /// </summary>
  private void Awake() {
    DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();
    dynamicMapsService.OnMapLoadStarted.AddListener(() => {
        MapsService mapsService = dynamicMapsService.MapsService;
        mapsService.MakeMapLoadRegion()
            .AddCircle(mapsService.Coords.FromLatLngToVector3(KoreaCenter), 300000)
            .Load(dynamicMapsService.RenderingStyles, 6);
    });
  }
}
