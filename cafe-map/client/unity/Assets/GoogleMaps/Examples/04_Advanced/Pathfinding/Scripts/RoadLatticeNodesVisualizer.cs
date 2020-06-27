using Google.Maps.Event;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// This class reveals all road lattice nodes used by the Maps SDK on the scene.
  /// </summary>
  public class RoadLatticeNodesVisualizer : MonoBehaviour {
    [Tooltip("The Base Map Loader handles the Maps Service initialization and basic event flow.")]
    public BaseMapLoader BaseMapLoader;

    [Tooltip(
        "Materials to apply to disjoint parts of the road lattice debug object." +
        " Applied semi-randomly.")]
    public Material[] LatticeMaterials;

    [Tooltip("If true, indicate objects will be created at each node.")]
    public bool IndicateNodes = false;

    [Tooltip("Whether to show partitioning of road lattice.")]
    public bool ShowPartitioned = false;

    [Tooltip("The container that holds all road lattice nodes visuals.")]
    public GameObject RoadLattice;

    /// <summary>
    /// A local reference to the latest road lattice debug object which regroups all visuals for
    /// each road lattice node on the graph.
    /// </summary>
    private GameObject RoadLatticeDebugObject;

    // Start is called before the first frame update
    void Start() {
      ClearRoadLatticeDebugVisuals();
    }

    /// <summary>
    /// Handle Unity OnDisable event.
    /// </summary>
    void OnDisable() {
      BaseMapLoader.MapsService.Events.RoadLatticeEvents.DidModify.RemoveListener(
          OnModifiedRoadLattice);
      BaseMapLoader.MapsService.Events.MapEvents.Loaded.RemoveListener(OnMapLoaded);
      ClearAndReload();
    }

    /// <summary>
    /// Handle Unity OnEnable event.
    /// </summary>
    void OnEnable() {
      BaseMapLoader.MapsService.Events.RoadLatticeEvents.DidModify.AddListener(
          OnModifiedRoadLattice);
      BaseMapLoader.MapsService.Events.MapEvents.Loaded.AddListener(OnMapLoaded);
      ClearAndReload();
    }

    /// <summary>
    /// RoadLattice handler that creates a Road Lattice debug object for currently loaded map.
    /// </summary>
    /// <param name="args">RoadLattice event data</param>
    void OnModifiedRoadLattice(DidModifyRoadLatticeArgs args) {
      if (RoadLattice != null) {
        // Deletes the previous road lattice debug object
        if (RoadLatticeDebugObject != null)
          Destroy(RoadLatticeDebugObject);

        RoadLatticeDebugObject = RoadLatticeTools.MakeRoadLatticeDebugGameObject(
            args.RoadLattice, LatticeMaterials, IndicateNodes, ShowPartitioned);
        RoadLatticeDebugObject.transform.Translate(Vector3.up);
        RoadLatticeDebugObject.transform.SetParent(RoadLattice.transform, false);
      }
    }

    void OnMapLoaded(MapLoadedArgs args) {
      // Everything was loaded
      // Adjust the scale of our road lattice nodes
      SphereCollider[] nodes = RoadLatticeDebugObject.GetComponentsInChildren<SphereCollider>();

      foreach (SphereCollider sc in nodes) {
        sc.transform.localScale = new Vector3(4f, 4f, 4f);
      }
    }

    /// <summary>
    /// Destroys all node visuals on the map.
    /// </summary>
    private void ClearRoadLatticeDebugVisuals() {
      if (RoadLattice != null) {
        foreach (Transform child in RoadLattice.transform)
          Destroy(child.gameObject);
      }
    }

    /// <summary>
    /// Destroys all augmented data and map features before reloading the map.
    /// When new events are added to the maps service, the area needs to be reloaded for these to
    /// take effect.
    /// This can also have an impact on all augmented objects in the scene, which need to be updated
    /// accordingly.
    /// </summary>
    private void ClearAndReload() {
      // Clear Visuals
      ClearRoadLatticeDebugVisuals();

      // Update the map
      if (BaseMapLoader != null) {
        BaseMapLoader.ClearMap();
        BaseMapLoader.LoadMap();
      }
    }
  }
}
