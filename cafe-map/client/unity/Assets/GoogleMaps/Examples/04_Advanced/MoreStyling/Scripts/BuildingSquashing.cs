using System.Collections;
using Google.Maps.Coord;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature.Style;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Example demonstrating how one can squash buildings in the vicinity of a player character
  /// avatar.
  /// </summary>
  /// <remarks>
  /// Uses <see cref="ErrorHandling"/> component to display any errors encountered by the
  /// <see cref="MapsService"/> component when loading geometry.
  /// </remarks>
  [RequireComponent(typeof(MapsService), typeof(ErrorHandling))]
  public sealed class BuildingSquashing : MonoBehaviour {
    [Tooltip("LatLng to load (must be set before hitting play).")]
    public LatLng LatLng = new LatLng(-33.866651, 151.207587);

    /// <summary>
    /// Transform of the GameObject representing the players avatar.
    /// </summary>
    public Transform Avatar;

    /// <summary>
    /// Distance inside which buildings will be completely squashed (<see cref="MaximumSquash"/>)
    /// </summary>
    public float SquashNear = 50;

    /// <summary>
    /// Distance outside which buildings will not be squashed.
    /// </summary>
    public float SquashFar = 200;

    /// <summary>
    /// The vertical scaling factor applied at maximum squashing.
    /// </summary>
    public float MaximumSquash = 0.1f;

    /// <summary>
    /// The minimum random height applied to extruded buildings that have no server provided height.
    /// </summary>
    public float MinRandomBuildingHeight = 60;

    /// <summary>
    /// The maximum random height applied to extruded buildings that have no server provided height.
    /// </summary>
    public float MaxRandomBuildingHeight = 100;

    private MapsService MapsService;
    private Vector3 CameraPosition;
    private Quaternion CameraRotation;

    /// <summary>
    /// Use <see cref="Google.Maps.MapsService"/> to load geometry.
    /// </summary>
    private void Start() {
      // Get required Maps Service component on this GameObject.
      MapsService = GetComponent<MapsService>();

      // Set real-world location to load.
      MapsService.InitFloatingOrigin(LatLng);

      MapsService.LoadMap(ExampleDefaults.DefaultBounds, ExampleDefaults.DefaultGameObjectOptions);

      // Add a pre-creation listener for buildings to apply some randomized heights.
      MapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(
          e => { e.Style = RandomizeBuildingHeight(e.Style); });

      // Apply a post-creation listener that adds the squashing MonoBehaviour to each building.
      MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          e => { AddSquasher(e.GameObject); });
      MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
          e => { AddSquasher(e.GameObject); });

      // Start a coroutine that periodically loads the visible portion of the map.
      StartCoroutine(LoadVisibleMapArea());
    }

    /// <summary>
    /// Returns a copy of the supplied style modified to apply a selected random value to extruded
    /// buildings that do not have a server provided height.
    /// </summary>
    /// <param name="suppliedStyle">Style to use as a template.</param>
    /// <returns>A modified copy of suppliedStyle.</returns>
    private ExtrudedStructureStyle RandomizeBuildingHeight(ExtrudedStructureStyle suppliedStyle) {
      ExtrudedStructureStyle.Builder builder = new ExtrudedStructureStyle.Builder(suppliedStyle);

      builder.ExtrudedBuildingFootprintHeight =
          Random.Range(MinRandomBuildingHeight, MaxRandomBuildingHeight);

      return builder.Build();
    }

    /// <summary>
    /// Adds a Squasher MonoBehaviour to the supplied GameObject.
    /// </summary>
    /// <remarks>
    /// The Squasher MonoBehaviour reduced the vertical scale of the GameObject's transform when a
    /// certain object is nearby.
    /// </remarks>
    /// <param name="go">The GameObject to which to add the Squasher behaviour.</param>
    private void AddSquasher(GameObject go) {
      Squasher squasher = go.AddComponent<Squasher>();
      squasher.Target = Avatar;
      squasher.Near = SquashNear;
      squasher.Far = SquashFar;
      squasher.MaximumSquashing = MaximumSquash;
    }

    /// <summary>
    /// A co-routine to load a map region based on the frustum of the default camera.
    /// </summary>
    /// <remarks>
    /// Tries to load a new map region every 250ms if the camera has moved.
    /// </remarks>
    private IEnumerator LoadVisibleMapArea() {
      while (true) {
        DoLoadVisibleMap();

        yield return new WaitForSeconds(0.25f); // Every 250ms.
      }
    }

    /// <summary>
    /// Calculates a map region based on the frustum of the default camera, and calls
    /// <see cref="MapsService"/> to load the calculated region.
    /// </summary>
    private void DoLoadVisibleMap() {
      if (Camera.main.transform.position == CameraPosition &&
          Camera.main.transform.rotation == CameraRotation) {
        return;
      }

      CameraPosition = Camera.main.transform.position;
      CameraRotation = Camera.main.transform.rotation;

      MapsService.MakeMapLoadRegion()
          .AddViewport(Camera.main)
          .Load(ExampleDefaults.DefaultGameObjectOptions);
    }
  }
}
