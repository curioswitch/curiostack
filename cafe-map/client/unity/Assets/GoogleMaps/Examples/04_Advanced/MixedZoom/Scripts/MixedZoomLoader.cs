using Google.Maps.Coord;
using Google.Maps.Examples.Shared;
using Google.Maps.Loading;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>Example script demonstrating Mixed Zoom in Musk.</summary>
  [RequireComponent(typeof(MapLoader), typeof(MixedZoom))]
  public class MixedZoomLoader : MonoBehaviour {
    [Tooltip("Latitude and longitude to load.")]
    public LatLng LatLng = new LatLng(-33.866, 151.196);

    [Tooltip("Load the map from the current camera position when it has moved this far.")]
    public float LoadDistance = 10;

    [Tooltip("Unload unused parts of the map when the camera position has moved this far.")]
    public float UnloadDistance = 100;

    /// <summary>Camera position when <see cref="MapLoader.Load()"/> was last called.</summary>
    private Vector3? LastLoadPosition;

    /// <summary>
    /// Camera position when <see cref="MapLoader.UnloadUnused()"/> was last called.
    /// </summary>
    private Vector3? LastUnloadPosition;

    /// <summary>MapLoader component.</summary>
    private MapLoader MapLoader;

    /// <summary>Set up this script.</summary>
    public void Start() {
      MapLoader = GetComponent<MapLoader>();
      MapLoader.Init(ExampleDefaults.DefaultGameObjectOptions);
      MapLoader.MapsService.InitFloatingOrigin(LatLng);
    }

    /// <summary>Per-frame update tasks.</summary>
    public void Update() {
      UpdateMap();
    }

    /// <summary>
    /// If the camera has moved far enough, load more of the map, and unload parts of the map that
    /// aren't currently in view.
    /// </summary>
    private void UpdateMap() {
      bool load = (LastLoadPosition == null) ||
                  ((gameObject.transform.position - LastLoadPosition.Value).sqrMagnitude >=
                   LoadDistance * LoadDistance);

      bool unload = (LastUnloadPosition == null) ||
                    ((gameObject.transform.position - LastUnloadPosition.Value).sqrMagnitude >=
                     UnloadDistance * UnloadDistance);

      if (load) {
        // Load the map with mixed zoom, centered on the current camera location.
        MapLoader.Load();
        LastLoadPosition = gameObject.transform.position;
      }

      if (unload) {
        // Unload map GameObjects that have been inactive for longer than
        // MapLoader.UnloadUnusedSeconds.
        MapLoader.UnloadUnused();
        LastUnloadPosition = gameObject.transform.position;
      }
    }
  }
}
