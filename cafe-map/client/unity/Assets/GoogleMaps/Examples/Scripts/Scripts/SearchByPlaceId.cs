using Google.Maps;
using Google.Maps.Coord;
using Google.Maps.Event;
using UnityEngine;

/// <summary>
/// Place Id example, demonstrating how to use the Maps Unity SDK's DidCreate event to alter
/// elements by Place Id.
/// </summary>
/// <remarks>
/// By default loads Tokyo Station and reduces its height to 4 floors.
/// <para>
/// Also uses <see cref="ErrorHandling"/> component to display any errors encountered by the
/// <see cref="MapsService"/> component when loading geometry.
/// </para></remarks>
[RequireComponent(typeof(MapsService), typeof(ErrorHandling))]
public sealed class SearchByPlaceId : MonoBehaviour {
  [Tooltip("Place Id to search for and set the height of (Tokyo Station by default).")]
  public string PlaceId = "ChIJ92ebZvmLGGARERQIpz9QIp4";

  [Tooltip("New height to set (4 floors of 3.5 meters each by default).")]
  public float NewHeight = 4f * 3.5f;

  [Tooltip("Material to assign (ignored if not defined).")]
  public Material NewMaterial;

  [Tooltip("LatLng to load (must be set before hitting play).")]
  public LatLng LatLng = new LatLng(35.680017804469, 139.767591384361);

  /// <summary>
  /// Get <see cref="MapsService"/> and use it to load geometry, searching for the given Place Id
  /// in all extruded buildings.
  /// </summary>
  private void Start () {
    // Make sure a Place Id has been given.
    if (string.IsNullOrEmpty(PlaceId)) {
      // Note: 'name' and 'GetType()' just give the name of the GameObject this script is on, and
      // the name of this script respectively.
      Debug.LogErrorFormat("No Place Id defined for {0}.{1}, which needs a Place Id to operate!",
          name, GetType());
      return;
    }

    // Get required Maps Service component on this GameObject.
    MapsService mapsService = GetComponent<MapsService>();

    // Set location to load.
    mapsService.InitFloatingOrigin(LatLng);

    // Sign up to event that will be called after each extruded building is created, so can check
    // the Place Id of this building and adjust its height if required.
    mapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(CheckGeometry);

    // Load map with default options.
    mapsService.LoadMap(ExampleDefaults.DefaultBounds, ExampleDefaults.DefaultGameObjectOptions);
  }

  /// <summary>
  /// Check building just made by the <see cref="MapsService"/>, and if its PlaceID matches the one
  /// searched for, adjust building's height to desired level.
  /// </summary>
  private void CheckGeometry(DidCreateExtrudedStructureArgs eventArgs) {
    if (PlaceId.Equals(eventArgs.MapFeature.Metadata.PlaceId)) {
      // Get current height of matching building.
      var building = eventArgs.GameObject;
      float buildingHeight = building.GetComponent<MeshFilter>().sharedMesh.bounds.size.y;

      // Confirm height is not zero. Due to floating point rounding errors this is best done by
      // checking a small range around zero (instead of checking exact equality to zero).
      if (Mathf.Abs(buildingHeight) < 0.0001f) {
        Debug.LogErrorFormat("{0}.{1} received a building with no height, specifically building on "
            + "GameObject named {2}.\nEven though {2} matched searched for PlaceID {3}, cannot "
            + "scale up the height of {2} to the desired {4} meters because, as said, {2}'s height "
            + "is zero meters.",
            name, GetType(), building.name, PlaceId, NewHeight);
        return;
      }

      // Determine the amount by which we need to scale the building vertically to achieve the
      // desired height.
      float heightMultiplier = NewHeight / buildingHeight;
      building.transform.localScale = new Vector3 (
        building.transform.localScale.x,
        heightMultiplier,
        building.transform.localScale.z);

      // Set new material if given (ignored if not defined).
      if (NewMaterial != null) {
        // Note: extruded buildings have two materials, one for the walls and one for the roof, so a
        // 2-value array must be used to replace its materials.
        building.GetComponent<MeshRenderer>().sharedMaterials =
            new [] { NewMaterial, NewMaterial };
      }
    }
    // Note: this building may have multiple parts, so keep searching by Place Id until find them
    // all.
  }
}
