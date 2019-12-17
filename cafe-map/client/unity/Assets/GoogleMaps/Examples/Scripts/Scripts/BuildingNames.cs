using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Building names example, demonstrating how to get the name of a building created by the Maps
/// Unity SDK.
/// </summary>
/// <remarks>
/// The specific LabelPrefab used in this example contains a <see cref="Text"/> element with a
/// custom shader assigned. This shader makes sure this <see cref="Text"/> element is displayed on
/// top of all in-scene geometry (even if it is behind or inside said geometry). Examine the shader
/// on this prefab to find out how this is achieved.
/// <para>
/// By default loads Tokyo Station. If a new latitude/longitude is set in Inspector (before pressing
/// start), that location will be loaded instead.
/// </para>
/// Also uses <see cref="ErrorHandling"/> component to display any errors encountered by the
/// <see cref="MapsService"/> component when loading geometry.
/// </remarks>
[RequireComponent(typeof(MapsService), typeof(BuildingLabeller), typeof(ErrorHandling))]
public sealed class BuildingNames : MonoBehaviour {
  [Tooltip("LatLng to load (must be set before hitting play).")]
  public LatLng LatLng = new LatLng(35.6801154, 139.7633039);

  [Tooltip("Distance around player to load in meters (MapsService's default is 500 meters).")]
  [Range(0f, 2000f)]
  public float LoadDistance = 1000f;

  /// <summary>
  /// Use <see cref="MapsService"/> to load geometry, labelling all created buildings with their
  /// names.
  /// </summary>
  private void Start() {
    // Get required Maps Service component on this GameObject.
    MapsService mapsService = GetComponent<MapsService>();

    // Set real-world location to load.
    mapsService.InitFloatingOrigin(LatLng);

    // Sign up to event called just after any new building (extruded structure or modelled
    // structure) is loaded, so we can show name using required BuildingLabeller component.
    BuildingLabeller buildingLabeller = GetComponent<BuildingLabeller>();
    mapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(args
        => buildingLabeller.NameExtrudedBuilding(args.GameObject, args.MapFeature));
    mapsService.Events.ModeledStructureEvents.DidCreate.AddListener(args
        => buildingLabeller.NameModeledBuilding(args.GameObject, args.MapFeature));

    // Set custom load distance (given as a parameter), which should be larger than the default 500m
    // in order to better demonstrate Level of Detail effect over a large area.
    Bounds loadBounds = new Bounds(Vector3.zero, new Vector3(LoadDistance, 0, LoadDistance));

    // Load map with created load bounds and default options.
    mapsService.LoadMap(loadBounds, ExampleDefaults.DefaultGameObjectOptions);
  }
}
