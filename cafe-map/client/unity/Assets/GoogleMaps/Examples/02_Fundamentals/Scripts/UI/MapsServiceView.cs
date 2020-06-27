using System;
using Google.Maps.Coord;
using Google.Maps.Examples.Shared;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// This component handles the UI mechanics of the MapsService101 example.
  /// It relies on the <see cref="BaseMapLoader"/> to interact with maps service, load maps
  /// data, register maps events.
  /// The component enables/disables the following features:
  /// <list type="bullet">
  /// <item><description>Dynamic Loading</description></item>
  /// <item><description>Floating Origin Updates</description></item>
  /// <item><description>Road Names</description></item>
  /// <item><description>Building Names</description></item>
  /// <item><description>Loading Progress</description></item>
  /// <item><description>Search by PlaceId</description></item>
  /// </list>
  /// </summary>
  public class MapsServiceView : MonoBehaviour {
    [Tooltip("The Base Map Loader handles the Maps Service initialization and basic event flow.")]
    public BaseMapLoader BaseMapLoader;

    [Tooltip("Current latitude of the floating origin.")]
    public Text LatValue;

    [Tooltip("Current longitude of the floating origin.")]
    public Text LngValue;

    [Tooltip("Current zoom level of the maps service")]
    public Text CurrentZoomLevel;

    [Tooltip("Search by PlaceId dialog box. Displays all placeIds available in the scene.")]
    public SearchByIdDialog SearchByPlaceIdDialog;

    [Tooltip("Camera controller WSAD + Up/Down")]
    public CameraController CameraController;

    [Tooltip("Reference to the script handling Dynamic Maps Loading.")]
    public DynamicMapsUpdater DynamicMapsUpdater;

    [Tooltip("Reference to the script handling the relocation of the floating origin.")]
    public FloatingOriginUpdater FloatingOriginUpdater;

    [Tooltip("Reference to the script handling road names.")]
    public Labeller RoadLabeller;

    [Tooltip("Reference to the script handling building names.")]
    public Labeller BuildingLabeller;

    [Tooltip("Reference to the script handling the loading progress bar.")]
    public ProgressBarUpdater ProgressBarUpdater;

    [Tooltip("Reference to the script handling the Search by PlaceId example.")]
    public SearchByPlaceIdUpdater SearchByPlaceIdUpdater;

    [Tooltip("Enable/Disable Dynamic Loading.")]
    public Toggle DynamicMapsToggle;

    [Tooltip("Enable/Disable Floating Origin Updates.")]
    public Toggle FloatingLatLngOriginToggle;

    [Tooltip("Enable/Disable the display of road names.")]
    public Toggle RoadNamesToggle;

    [Tooltip("Enable/Disable the display of building names.")]
    public Toggle BuildingNamesToggle;

    [Tooltip("Enable/Disable the display of the loading progress bar.")]
    public Toggle ProgressBarToggle;

    [Tooltip(
        "Arrow prefab. " +
        "The arrow is used as augmented information on the map to highlight a selection.")]
    public GameObject ArrowGameObject;

    /// <summary>
    /// Y-offset for displaying the arrow pointer when Search by PlaceId is used.
    /// </summary>
    private float ArrowOffsetY = 65f;

    private void Start() {
      // Check expected references.
      Debug.Assert(DynamicMapsUpdater, "Missing DynamicMapsUpdater!");
      Debug.Assert(FloatingOriginUpdater, "Missing FloatingOriginUpdater!");
      Debug.Assert(RoadLabeller, "Missing RoadLabeller!");
      Debug.Assert(BuildingLabeller, "Missing BuildingLabeller!");
      Debug.Assert(ProgressBarUpdater, "Missing ProgressBarUpdater!");
      Debug.Assert(SearchByPlaceIdUpdater, "Missing SearchByPlaceIdUpdater!");
      Debug.Assert(LatValue, "Missing latitude UI element!");
      Debug.Assert(LngValue, "Missing longitude UI element!");
      Debug.Assert(CurrentZoomLevel, "Missing currentZoomLevel UI element!");

      // Hide the SearchByPlaceId dialog.
      if (SearchByPlaceIdDialog != null) {
        SearchByPlaceIdDialog.gameObject.SetActive(false);
      }

      // Init toggles.
      if (DynamicMapsToggle != null && DynamicMapsUpdater != null) {
        DynamicMapsToggle.isOn = DynamicMapsUpdater.enabled;
      }

      if (FloatingLatLngOriginToggle != null && FloatingOriginUpdater != null) {
        FloatingLatLngOriginToggle.isOn = FloatingOriginUpdater.enabled;
      }

      if (RoadNamesToggle != null && RoadLabeller != null) {
        RoadNamesToggle.isOn = RoadLabeller.enabled;
      }

      if (BuildingNamesToggle != null && BuildingLabeller != null) {
        BuildingNamesToggle.isOn = BuildingLabeller.enabled;
      }

      if (ProgressBarToggle != null && ProgressBarUpdater != null) {
        ProgressBarToggle.isOn = ProgressBarUpdater.enabled;
      }

      // Hide the pointer to selected PlaceId.
      HidePointer();
    }

    private void Update() {
      // Update maps service values as they change.
      if (LatValue != null && LngValue != null && CurrentZoomLevel != null) {
        LatLng latLng = BaseMapLoader.MapsService.Coords.FromVector3ToLatLng(
            FloatingOriginUpdater.FloatingOrigin);
        LatValue.text = latLng.Lat.ToString("N5");
        LngValue.text = latLng.Lng.ToString("N5");
        CurrentZoomLevel.text = BaseMapLoader.MapsService.ZoomLevel.ToString();
      }
    }

    /// <summary>
    /// Enables / Disables the <see cref="DynamicMapsUpdater"/> script.
    /// </summary>
    public void OnDynamicMapsSelected(Toggle change) {
      DynamicMapsUpdater.enabled = change.isOn;
    }

    /// <summary>
    /// Enables / Disables the <see cref="FloatingOriginUpdater"/> script.
    /// </summary>
    /// <param name="change">The checkbox element</param>
    public void OnUpdateFloatingOriginSelected(Toggle change) {
      if (change.isOn) {
        FloatingOriginUpdater.OnFloatingOriginUpdate.AddListener(OnFloatingOriginUpdated);
      } else {
        FloatingOriginUpdater.OnFloatingOriginUpdate.RemoveListener(OnFloatingOriginUpdated);
      }

      FloatingOriginUpdater.enabled = change.isOn;
    }

    /// <summary>
    /// The floating origin has changed: Move all augmented elements in the scene.
    /// </summary>
    /// <param name="originOffset">The offset the floating origin was moved by.</param>
    public void OnFloatingOriginUpdated(Vector3 originOffset) {
      // If we have augmented elements on the map, move them
      if (ArrowGameObject != null) {
        ArrowGameObject.transform.position += originOffset;
      }

      Debug.Log("Origin was moved by " + originOffset);

      // TODO: Place an arrow at the new location
      // Should be displayed on top of any geometry at that location
    }

    /// <summary>
    /// Enables / Disables the <see cref="RoadLabeller"/> script.
    /// </summary>
    /// <param name="change">The checkbox element</param>
    public void OnRoadNamesSelected(Toggle change) {
      RoadLabeller.enabled = change.isOn;
    }

    /// <summary>
    /// Enables / Disables the <see cref="BuildingLabeller"/> script.
    /// </summary>
    /// <param name="change">The checkbox element</param>
    public void OnBuildingNamesSelected(Toggle change) {
      BuildingLabeller.enabled = change.isOn;
    }

    /// <summary>
    /// Enables / Disables the <see cref="ProgressBarUpdater"/> script.
    /// </summary>
    /// <param name="change">The checkbox element</param>
    public void OnProgressBarSelected(Toggle change) {
      ProgressBarUpdater.enabled = change.isOn;
    }

    /// <summary>
    /// Triggered to show the Search by placeId dialog box.
    /// </summary>
    public void OnShowSearchByPlaceIDDialog() {
      SearchByPlaceIdDialog.gameObject.SetActive(true);
    }

    /// <summary>
    /// Triggered when a place Id has been selected in the search by place Id dialog.
    /// We use the selected place id as a key to retrieve the actual map feature
    /// <see cref="GameObject"/> from a previously updated dictionary.
    /// The references between placeId and <see cref="GameObject"/> are initialized as the map is
    /// loaded, and handled in <see cref="SearchByPlaceIdUpdater"/>.
    /// </summary>
    /// <exception cref="Exception">The provided PlaceId is invalid</exception>
    public void OnSearchByPlaceId() {
      // Do we have a valid placeId?
      if (SearchByPlaceIdDialog.PlaceIds.options.Count > 0) {
        string placeId =
            SearchByPlaceIdDialog.PlaceIds.options[SearchByPlaceIdDialog.PlaceIds.value].text;

        if (!SearchByPlaceIdUpdater.PlaceIdToGameObjectDict.ContainsKey(placeId)) {
          throw new System.Exception(
              "Can't find the place Id in our cache. Inconsistent data state.");
        }

        GameObject target = SearchByPlaceIdUpdater.PlaceIdToGameObjectDict[placeId];

        if (target != null) {
          // We found our map feature: let's highlight it on the map.
          ShowPointer(target);

          // Move the camera to the selected location
          // Look at the GameObject identified by that place Id
          // Point the arrow to the game object - face arrow towards the camera
          if (Camera.main != null) {
            Camera.main.transform.position = new Vector3(
                target.transform.position.x + 100f,
                Camera.main.transform.position.y,
                target.transform.position.z + 100f);

            Camera.main.transform.LookAt(target.transform);

            // If the camera controller is set, update azimuth and inclination to have a smooth
            // transition when retaking control of the camera with the keyboard.
            if (CameraController != null) {
              CameraController.InitializeAzimuthAndInclination();
            }
          }
        }
      } else {
        Debug.Log("Empty dropdown!"); // Ignore
      }
    }

    /// <summary>
    /// Hides the arrow <see cref="GameObject"/> used to highlight a specific map feature.
    /// </summary>
    private void HidePointer() {
      if (ArrowGameObject != null) {
        ArrowGameObject.gameObject.SetActive(false);
      }
    }

    /// <summary>
    /// Positions and shows the arrow object used to highlight a specific map feature.
    /// The target feature is passed as an input.
    /// </summary>
    private void ShowPointer(GameObject target) {
      if (ArrowGameObject != null && target != null) {
        ArrowGameObject.gameObject.SetActive(true);

        MapsGamingExamplesUtils.PlaceUIMarker(target, ArrowGameObject.transform);

        RectTransform rectTransform = ArrowGameObject.gameObject.GetComponent<RectTransform>();

        if (rectTransform == null) {
          throw new System.Exception("Arrow GameObject does not have a renderer!");
        }

        ArrowOffsetY = rectTransform.rect.height / 2;
        ArrowGameObject.transform.position += new Vector3(0, ArrowOffsetY, 0);
      }
    }
  }
}
