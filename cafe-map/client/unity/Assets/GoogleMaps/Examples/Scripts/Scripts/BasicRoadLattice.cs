using Google.Maps;
using Google.Maps.Coord;
using Google.Maps.Event;
using UnityEngine;

/// <summary>
/// An exmaple that loads a static map and displays a depiction of the associated road lattice.
/// </summary>
[RequireComponent(typeof(MapsService))]
public sealed class BasicRoadLattice : MonoBehaviour {
  [Tooltip("LatLng to load (must be set before hitting play).")]
  public LatLng LatLng = new LatLng(40.6892199, -74.044601);

  [Tooltip("Minimum size of loaded map (must be set before hitting play).")]
  public Bounds Bounds = new Bounds(Vector3.zero, new Vector3(50, 0, 50));

  [Tooltip("If true, indicate objects will be created at each node.")]
  public bool IndicateNodes = false;

  [Tooltip("Whether to show partitioning of road lattice.")]
  public bool ShowPartitioned = false;

  [Tooltip("Materials to apply to disjoint parts of the road lattice debug object."
           + " Applied semi-randomly.")]
  public Material[] LatticeMaterials;

  private MapsService MapsService;
  private GameObject RoadLatticeDebugObject;

  /// <summary>
  /// Use <see cref="MapsService"/> to load geometry.
  /// </summary>
  private void Start() {
    // Get required Maps Service component on this GameObject.
    MapsService = GetComponent<MapsService>();

    // Set real-world location to load.
    MapsService.InitFloatingOrigin(LatLng);

    // Load map with default options.
    MapsService.LoadMap(Bounds, ExampleDefaults.DefaultGameObjectOptions);
  }

  /// <summary>
  /// MapLoaded handler that creates a Road Lattice debug object for currently loaded map.
  /// </summary>
  /// <param name="args">Map loaded arguments</param>
  internal void ShowRoadLattice(DidModifyRoadLatticeArgs args) {
    if (RoadLatticeDebugObject != null) {
      Destroy(RoadLatticeDebugObject);
    }

    RoadLatticeDebugObject = RoadLatticeTools.MakeRoadLatticeDebugGameObject(
            args.RoadLattice, LatticeMaterials, IndicateNodes, ShowPartitioned);
    RoadLatticeDebugObject.transform.Translate(Vector3.up);
    RoadLatticeDebugObject.transform.SetParent(transform, false);
  }
}
