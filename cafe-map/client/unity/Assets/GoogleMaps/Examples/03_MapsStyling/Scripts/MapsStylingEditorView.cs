using System.Collections;
using System.Collections.Generic;
using System.Text;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature;
using UnityEngine;
using UnityEngine.UI;
using Google.Maps.Feature.Style;

namespace Google.Maps.Examples {
  /// <summary>
  /// This class handles the user interface for styling the various Map Features prior to loading a
  /// map with <see cref="MapsService"/>.
  /// There are several techniques to apply styles at runtime while loading a map.
  /// One way is to provide a custom configuration at load time as shown below in the
  /// OnApplyStyle() function.
  /// Another way consists of using the WillCreate/DidCreate events associated to each Map Feature.
  /// Example: <see cref="ExtrudedStructureConfigView"/> Init function.
  /// This later is a popular option if you need to apply conditional styles at runtime (like
  /// applying styles based on Maps meta data).
  /// In this example, we illustrate both techniques.
  /// </summary>
  /// <remarks>
  /// Note that a third option is to in keep track of the loaded MapFeature and its related
  /// <see cref="GameObject"/> in a Dictionary.
  /// The <see cref="MapFeature"/> property gives access to the metadata tag for this object.
  /// Then once the map is loaded, developers can provide additional game logic to quickly access
  /// Maps Gameobjects and change their rendering properties.
  /// </remarks>
  public class MapsStylingEditorView : MonoBehaviour {
    [Tooltip("Reference to the base map loader")]
    public BaseMapLoader BaseMapLoader;

    [Tooltip("Reference to all configuration panels (one per map feature)")]
    public List<GameObject> panels;

    [Tooltip("Controls the display of map features configuration panels")]
    public Dropdown mapFeaturesSelector;

    [Tooltip(
        "When using meta data as filters, styles are applied differently through WillCreate /" +
        " DidCreate event listeners.")]
    public Toggle useMetaDataAsFilters;

    // Start is called before the first frame update
    void Start() {
      Debug.Assert(panels != null, "Configuration Panels aren't set!");
      Debug.Assert(mapFeaturesSelector != null, "The Map Features Selector isn't set!");

      Debug.Assert(
          useMetaDataAsFilters != null, "The indicator to use metadata (or not) isn't set!");

      mapFeaturesSelector.value = 1;

      // Load the map with styling options
      StartCoroutine(Init());
    }

    /// <summary>
    /// Applies the style on start.
    /// </summary>
    public IEnumerator Init() {
      // Wait on BaseMapLoader to be setup
      yield return new WaitUntil(() => BaseMapLoader.IsInitialized);

      // Make to create a new GameObjectOptions
      // This is done to preserve the default styleoptions object assigned in BaseMapLoader
      // as we'll reuse it in the Reset function
      BaseMapLoader.RenderingStyles = new GameObjectOptions();
      SetDefaultStyles(BaseMapLoader.RenderingStyles);

      // Initialize all config panels
      for (int i = 0; i < panels.Count; i++) {
        IStyleConfigView viewConfig = panels[i].GetComponent<IStyleConfigView>();

        if (viewConfig != null) {
          viewConfig.InitConfig();
        }
      }

      OnApplyStyle();
    }

    public void OnApplyStyle() {
      // Clear and reload map
      if (BaseMapLoader != null && !BaseMapLoader.IsLoading) {
        Debug.Log("Apply " + useMetaDataAsFilters.isOn);

        if (!useMetaDataAsFilters.isOn) {
          ApplyCustomStyles(BaseMapLoader.RenderingStyles);
        }

        BaseMapLoader.ClearMap();

        // There are several ways to apply styles while loading a map.
        // We illustrate below both global and conditional styling.
        // If the meta data filters is on, we apply style changes through event listeners otherwise
        // we just apply styles globally.
        // Event listeners have previously been added in the specific map features config classes.
        // Example: See ExtrudedStructureConfigView.

        BaseMapLoader.MapsService.LoadMap(
            new Bounds(Vector3.zero, new Vector3(1000f, 0f, 1000f)), BaseMapLoader.RenderingStyles);
      }
    }

    /// <summary>
    /// Resets the map to our default rendering style.
    /// </summary>
    public void OnReset() {
      useMetaDataAsFilters.isOn = false;

      for (int i = 0; i < panels.Count; i++) {
        IStyleConfigView viewConfig = panels[i].GetComponent<IStyleConfigView>();

        if (viewConfig != null) {
          viewConfig.Reset();
        }
      }

      OnApplyStyle();
    }

    /// <summary>
    /// Initializes Map Styling Options to default values.
    /// </summary>
    private void SetDefaultStyles(GameObjectOptions options) {
      if (options == null)
        return;

      options.ExtrudedStructureStyle =
          ExampleDefaults.DefaultGameObjectOptions.ExtrudedStructureStyle;
      options.ModeledStructureStyle =
          ExampleDefaults.DefaultGameObjectOptions.ModeledStructureStyle;
      options.RegionStyle = ExampleDefaults.DefaultGameObjectOptions.RegionStyle;
      options.AreaWaterStyle = ExampleDefaults.DefaultGameObjectOptions.AreaWaterStyle;
      options.LineWaterStyle = ExampleDefaults.DefaultGameObjectOptions.LineWaterStyle;
      options.SegmentStyle = ExampleDefaults.DefaultGameObjectOptions.SegmentStyle;
    }

    private void ApplyCustomStyles(GameObjectOptions options) {
      if (options == null)
        return;

      for (int i = 0; i < panels.Count; i++) {
        IStyleConfigView viewConfig = panels[i].GetComponent<IStyleConfigView>();

        if (viewConfig != null) {
          viewConfig.ApplyStyle(options);
        }
      }
    }

    /// <summary>
    /// A new map feature has been selected, show/hide the appropriate panels.
    /// </summary>
    /// <param name="change"></param>
    public void OnMapFeatureSelected(Dropdown change) {
      // Based on the index of the newly selected map feature,
      // Show/Hide the related configuration panel.
      ShowHidePanel(change.value);
    }

    private void ShowHidePanel(int idx) {
      if (idx < 0 || idx >= panels.Count)
        throw new System.Exception("Invalid map features index!");

      for (int i = 0; i < panels.Count; i++) {
        panels[i].SetActive(i == idx);
      }
    }
  }
}
