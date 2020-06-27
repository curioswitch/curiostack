using Google.Maps.Coord;
using Google.Maps.Event;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature.Style;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Simple example, demonstrating how to use the IsPrivate field in road metadata.
  /// </summary>
  [RequireComponent(typeof(MapsService))]
  public sealed class PrivateRoads : MonoBehaviour {
    [Tooltip("LatLng to load (must be set before hitting play).")]
    public LatLng LatLng = new LatLng(33.566709, -117.721376);

    [Tooltip("Material used on roads that are marked as private.")]
    public Material PrivateRoadMaterial;

    /// <summary>
    /// Use <see cref="MapsService"/> to load geometry.
    /// </summary>
    private void Start() {
      // Get required Maps Service component on this GameObject.
      MapsService mapsService = GetComponent<MapsService>();

      // Set real-world location to load.
      mapsService.InitFloatingOrigin(LatLng);
      mapsService.Events.SegmentEvents.WillCreate.AddListener(WillCreateHandler);

      // Load map with default options.
      mapsService.LoadMap(ExampleDefaults.DefaultBounds, ExampleDefaults.DefaultGameObjectOptions);
    }

    /// <summary>
    /// Checks Metadata, and assigns a new material to any road that is marked as private.
    /// </summary>
    /// <param name="args">Segment creation event arguments.</param>
    private void WillCreateHandler(WillCreateSegmentArgs args) {
      if (args.MapFeature.Metadata.IsPrivate) {
        // If the segment that is being created is marked private, change the material in the style
        // used to display the segment.
        SegmentStyle.Builder style = args.Style.AsBuilder();
        style.Material = PrivateRoadMaterial;
        args.Style = style.Build();
      }
    }
  }
}
