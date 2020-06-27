using System;
using System.Collections.Generic;
using Google.Maps.Feature;
using Google.Maps.Feature.Style;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// This base class handles the boiler plate code for the view mechanic, in particular the
  /// handling of both parameters and metadata panels.
  /// Also it populates the metadata collection based on map features.
  /// The interesting bits specific to map loading happen in the overriden methods InitMapConfig()
  /// and ApplyStyles().
  /// InitMapConfig registers WillCreate listeners that will allow us to customize some parts of the
  /// map based on the specified meta data filters.
  /// ApplyStyles assigns the newly configured configuration to the global
  /// <see cref="GameObjectOptions"/>, before it is being passed to the LoadMap apis from
  /// <see cref="MapsService"/>.
  /// </summary>
  public abstract class BaseConfigView : MonoBehaviour, IStyleConfigView {
    [Tooltip("Reference to the parameters configuration panel")]
    public GameObject parametersConfigPanel;

    [Tooltip("Reference to the meta data configuration panel")]
    public GameObject metaDataConfigPanel;

    [Tooltip("Reference to the actual container of meta data toggle elements")]
    public GameObject metaDataContent;

    [Tooltip("Prefab for creating the toggle background of each metadata available selection")]
    public Sprite toggleBGElement;

    [Tooltip("Prefab for creating the toggle checkmark of each metadata available selection")]
    public Sprite checkmarkElement;

    [Tooltip(
        "When using meta data as filters, styles are applied differently through WillCreate / " +
        "DidCreate event listeners.")]
    public Toggle useMetaDataAsFilters;

    /// <summary>
    /// Keeps track of the available extruded structures meta data based on the loaded map.
    /// </summary>
    protected Dictionary<string, Toggle> metaDataCollection;

    protected bool isInitialized;

    public virtual void InitConfig() {
      Debug.Log("SegmentConfigView initialized +++/---");

      if (!isInitialized) {
        InitView();
        InitMapConfig();
        isInitialized = true;
      }
    }

    public virtual void Reset() {
      InitMapConfig();
    }

    public abstract void ApplyStyle(GameObjectOptions options);

    protected virtual void InitView() {
      DefaultControls.Resources uiResources = new DefaultControls.Resources();
      uiResources.background = toggleBGElement;
      uiResources.checkmark = checkmarkElement;

      // Create filters options for extruded structures
      metaDataCollection = new Dictionary<string, Toggle>();

      foreach (string key in Enum.GetNames(GetUsageType())) {
        GameObject uiToggle = DefaultControls.CreateToggle(uiResources);
        uiToggle.transform.SetParent(metaDataContent.transform, false);
        Text t = uiToggle.GetComponentInChildren<Text>();

        if (t != null) {
          t.text = key;
        }

        metaDataCollection.Add(key, uiToggle.GetComponent<Toggle>());
      }
    }

    protected abstract System.Type GetUsageType();

    protected virtual void InitMapConfig() {
      // Disable all metadata entries by default
      foreach (Toggle t in metaDataCollection.Values)
        t.isOn = false;
    }

    protected bool EvaluateFilters(string tag) {
      if (metaDataCollection.ContainsKey(tag)) {
        return metaDataCollection[tag].isOn;
      }

      return false;
    }

    public void OnParametersSelected(Toggle toggle) {
      Debug.Log("OnParametersSelected+++");
      parametersConfigPanel.SetActive(toggle.isOn);
    }

    public void OnMetaDataSelected(Toggle toggle) {
      Debug.Log("OnMetaDataSelected+++");
      metaDataConfigPanel.SetActive(toggle.isOn);
    }
  }
}
