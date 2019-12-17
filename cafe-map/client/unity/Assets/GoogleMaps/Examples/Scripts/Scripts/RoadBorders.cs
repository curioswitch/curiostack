using Google.Maps;
using Google.Maps.Coord;
using Google.Maps.Feature.Style;
using UnityEngine;

/// <summary>
/// Blended roads example, demonstrating how to use Maps Unity SDK's two-stroke roads to visually
/// blend roads into surrounding grounds.
/// </summary>
/// <remarks>
/// By default loads the roads around the Statue of Liberty.
/// <para>
/// Uses <see cref="ErrorHandling"/> component to display any errors encountered by the
/// <see cref="MapsService"/> component when loading geometry.
/// </para></remarks>
[RequireComponent(typeof(MapsService), typeof(ErrorHandling))]
public sealed class RoadBorders : MonoBehaviour {
  [Tooltip("LatLng to load (must be set before hitting play).")]
  public LatLng LatLng = new LatLng(40.6892199, -74.044601);

  [Tooltip("Material to use for roads.")]
  public Material Roads;

  [Tooltip("Material to use for road-edges.")]
  public Material RoadEdges;

  /// <summary>
  /// Get <see cref="MapsService"/> and use it to load geometry.
  /// </summary>
  private void Start () {
    // Get required Maps Service component on this GameObject.
    MapsService mapsService = GetComponent<MapsService>();

    // Set location to load.
    mapsService.InitFloatingOrigin(LatLng);

    // Create a roads style that defines a material for roads and for borders of roads. The specific
    // border material used is chosen to look just a little darker than the material of the ground
    // plane (helping the roads to visually blend into the surrounding ground).
    SegmentStyle roadsStyle = new SegmentStyle.Builder {
      Material = Roads,
      BorderMaterial = RoadEdges,
      Width = 7.0f,
      BorderWidth = 1.0f
    }.Build();

    // Get default style options.
    GameObjectOptions renderingStyles = ExampleDefaults.DefaultGameObjectOptions;

    // Replace default roads style with new, just created roads style.
    renderingStyles.SegmentStyle = roadsStyle;

    // Load map with desired options.
    mapsService.LoadMap(ExampleDefaults.DefaultBounds, renderingStyles);
  }
}
