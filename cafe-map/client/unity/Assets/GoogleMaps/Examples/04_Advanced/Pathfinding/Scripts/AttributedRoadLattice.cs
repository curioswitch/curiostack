using Google.Maps.Coord;
using Google.Maps.Event;
using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// An example that loads a static map and displays a depiction of the associated road lattice
  /// with animated materials chosen based on the priority of each road.
  /// </summary>
  [RequireComponent(typeof(MapsService))]
  public sealed class AttributedRoadLattice : MonoBehaviour {
    [Tooltip("LatLng to load (must be set before hitting play).")]
    public LatLng LatLng = new LatLng(40.6892199, -74.044601);

    [Tooltip("Minimum size of loaded map (must be set before hitting play).")]
    public Bounds Bounds = new Bounds(Vector3.zero, new Vector3(50, 0, 50));

    [Tooltip("If true, indicate objects will be created at each node.")]
    public bool IndicateNodes = false;

    [Tooltip("Materials to apply to roads based on priority. First entry is the default.")]
    public Material[] LatticeMaterials;

    /// <summary>
    /// A reference to the GameObject created to show the road lattice.
    /// </summary>
    private GameObject RoadLatticeDebugObject;

    /// <summary>
    /// Use <see cref="MapsService"/> to load geometry.
    /// </summary>
    private void Start() {
      // Get required Maps Service component on this GameObject.
      MapsService mapsService = GetComponent<MapsService>();

      // Set real-world location to load.
      mapsService.InitFloatingOrigin(LatLng);

      // Load map with default options.
      mapsService.LoadMap(Bounds, ExampleDefaults.DefaultGameObjectOptions);
    }

    /// <summary>
    /// MapLoaded handler that creates a Road Lattice debug object for currently loaded map.
    /// </summary>
    /// <param name="args">Map loaded arguments</param>
    public void ShowRoadLattice(DidModifyRoadLatticeArgs args) {
      if (RoadLatticeDebugObject != null) {
        Destroy(RoadLatticeDebugObject);
      }

      RoadLatticeDebugObject = RoadLatticeTools.MakeAttributedLatticeDebugGameObject(
          args.RoadLattice, LatticeMaterials, IndicateNodes);
      RoadLatticeDebugObject.transform.Translate(Vector3.up);
      RoadLatticeDebugObject.transform.SetParent(transform, false);
    }
  }
}
