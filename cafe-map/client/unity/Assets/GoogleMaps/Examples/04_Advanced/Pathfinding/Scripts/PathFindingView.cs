using Google.Maps.Examples.Shared;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// Note: Road Lattice support is a beta feature subject to performance considerations and future
  /// changes
  ///
  /// This class handles the UI of the PathFinding example.
  /// In particular it enables users to:
  /// - Show/Hide nodes
  /// - Pick a destination for the main character
  /// - Activate AI agents to track the main character
  /// - Display the active search path between AI agents and their target
  /// </summary>
  public class PathFindingView : MonoBehaviour {
    [Tooltip("The Base Map Loader handles the Maps Service initialization and basic event flow.")]
    public BaseMapLoader BaseMapLoader;

    [Tooltip("Current latitude of the floating origin.")]
    public Text latValue;

    [Tooltip("Current longitude of the floating origin.")]
    public Text lngValue;

    [Tooltip("Camera controller WSAD + Up/Down")]
    public CameraController cameraController;

    [Tooltip("The PathFinding example")]
    public PathFindingExample PathFindingExample;

    [Tooltip("Reference to the nodes debug visualizer")]
    public RoadLatticeNodesVisualizer RoadLatticeNodesVisualizer;

    [Tooltip("Controls the visibility of the road lattice")]
    public Toggle ShowRoadLatticeToggle;

    [Tooltip("Controls the display of all active paths between AI agents and their targets")]
    public Toggle ShowAIPathsToggle;

    [Tooltip("Activates the search behavior of AI Agents")]
    public Toggle ActivateAIBotsToggle;

    /// <summary>
    /// IsReady is set to true if all required components for the pathfinding view are accounted
    /// for and initialized
    /// </summary>
    private bool IsReady;

    // Start is called before the first frame update
    void Start() {
      IsReady = true;

      if (BaseMapLoader == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, BaseMapLoader, "Base Map Loader", "is required for this script to work."));
        IsReady = false;
      }

      if (PathFindingExample == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this,
            PathFindingExample,
            "Path Finding Example",
            "is required for this script to work."));
        IsReady = false;
      }

      if (RoadLatticeNodesVisualizer == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this,
            RoadLatticeNodesVisualizer,
            "Road Lattice Nodes Visualizer",
            "is required for this script to work."));
        IsReady = false;
      }

      if (ShowRoadLatticeToggle != null)
        ShowRoadLatticeToggle.isOn = RoadLatticeNodesVisualizer.RoadLattice.activeSelf;

      if (ShowAIPathsToggle != null)
        ShowAIPathsToggle.isOn = PathFindingExample.IsDebugPathOn;

      if (ActivateAIBotsToggle != null)
        ActivateAIBotsToggle.isOn = PathFindingExample.IsAISearchActive;
    }

    /// <summary>
    /// Updates the current lat lng
    /// </summary>
    void Update() {
      if (IsReady) {
        // Update maps service values as they change.
        latValue.text = BaseMapLoader.LatLng.Lat.ToString("N5");
        lngValue.text = BaseMapLoader.LatLng.Lng.ToString("N5");
      }
    }

    /// <summary>
    /// Activates or deactivates the visual indicators for the road lattice.
    /// </summary>
    /// <param name="change">State of the toggle.</param>
    public void OnShowRoadLattice(Toggle change) {
      if (!IsReady)
        return;
      RoadLatticeNodesVisualizer.RoadLattice.SetActive(change.isOn);
    }

    /// <summary>
    /// Notifies all AI Agents to start showing their active path to target
    /// </summary>
    /// <param name="v"></param>
    public void OnShowAIRoutes(Toggle change) {
      if (!IsReady)
        return;
      PathFindingExample.ShowAIPaths(change.isOn);
    }

    /// <summary>
    /// Activates the search behaviour of all AI Agents
    /// </summary>
    /// <param name="v"></param>
    public void OnActivateAIBots(Toggle change) {
      if (!IsReady)
        return;
      PathFindingExample.ActiveAllNPCs(change.isOn);
    }
  }
}
