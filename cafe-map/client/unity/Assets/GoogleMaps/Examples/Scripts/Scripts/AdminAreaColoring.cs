using Google.Maps;
using Google.Maps.Coord;
using Google.Maps.Feature;
using Google.Maps.Feature.Style;
using Google.Maps.Util.Material;
using UnityEngine;

/// <summary>Displays a map of Japan's prefectures with a border around each one.</summary>
/// <remarks>
/// Japan is the only country in which we support administrative areas.
/// </remarks>
[RequireComponent(typeof(MapsService))]
public sealed class AdminAreaColoring : MonoBehaviour {
  /// <summary>Roughly the geographic center of Japan.</summary>
  private static readonly LatLng LoadingCenter = new LatLng(36.767858, 138.732025);

  [Tooltip("Fill color of the admin areas.")]
  public Color AdminAreaColor = Color.yellow;

  [Tooltip("Color of the admin area borders.")]
  public Color BorderColor = Color.black;

  [Tooltip("Width in Unity units of the admin area borders.")]
  public float BorderWidth = 6000;

  /// <summary>
  /// Configures the map.
  /// </summary>
  private void Start() {
    MapsService mapsService = GetComponent<MapsService>();
    mapsService.InitFloatingOrigin(LoadingCenter);

    Material adminAreaMaterial = BaseMapMaterialUtils.CreateUniformColoredMaterial(AdminAreaColor);

    // Configure the map to only show prefectures and texture them with a solid color.
    mapsService.Events.RegionEvents.WillCreate.AddListener(args => {
      if (args.MapFeature.Metadata.Usage == RegionMetadata.UsageType.AdministrativeArea1) {
        RegionStyle.Builder style = args.Style.AsBuilder();
        style.FillMaterial = adminAreaMaterial;
        args.Style = style.Build();
      } else {
        args.Cancel = true;
      }
    });

    // When a prefecture GameObject is created, create an extruded outline for it.
    Material borderMaterial = null;
    mapsService.Events.RegionEvents.DidCreate.AddListener(args => {
      if (borderMaterial == null) {
        // Set up the border material based on the prefecture material, but change the render queue
        // so that it renders on top.
        borderMaterial = new Material(args.GameObject.GetComponent<Renderer>().material);
        borderMaterial.color = BorderColor;
        borderMaterial.renderQueue++;
      }
      Extruder.AddAreaExternalOutline(args.GameObject, borderMaterial, args.MapFeature.Shape,
          BorderWidth);
    });

    // Don't display water.
    mapsService.Events.AreaWaterEvents.WillCreate.AddListener(args => {
      args.Cancel = true;
    });

    // Load the map around Japan.
    mapsService.MakeMapLoadRegion()
        .AddCircle(mapsService.Coords.FromLatLngToVector3(LoadingCenter), 2000000)  // 2,000 km.
        .Load(ExampleDefaults.DefaultGameObjectOptions, 5);  // Zoom level 5.
  }
}
