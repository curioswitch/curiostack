using System;
using System.Collections.Generic;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature;
using Google.Maps.Feature.Style;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  public class SegmentConfigView : BaseConfigView {
    [Tooltip("Reference to the base map loader")]
    public BaseMapLoader BaseMapLoader;

    public List<Material> SegmentMaterials;
    public List<Material> BorderMaterials;

    public Slider SegmentWidthSlider;
    public Slider BorderWidthSlider;

    // Keeping references to all toggles help us initialize these components even when this
    // GameObject is inactive.
    public List<Toggle> SegmentMaterialsToggles;
    public List<Toggle> BorderMaterialsToggles;

    private Material SegmentMaterial;
    private Material BorderMaterial;

    /// <summary>
    /// Keeps track of this configuration
    /// </summary>
    private SegmentStyle StyleConfiguration;

    public override void ApplyStyle(GameObjectOptions options) {
      Debug.Log("Apply segment styles");

      // The config panel might not have been initialized yet if it was inactive when this call
      // was made.
      if (StyleConfiguration == null)
        InitMapConfig();

      options.SegmentStyle = StyleConfiguration;
    }

    protected override void InitView() {
      base.InitView();

      // This event listener allows us to apply conditional styling.
      // In this case,
      BaseMapLoader.MapsService.Events.SegmentEvents.WillCreate.AddListener(arg0 => {
        // Apply all style parameters directly
        // if this map object verifies filter conditions
        if (useMetaDataAsFilters != null && useMetaDataAsFilters.isOn &&
            EvaluateFilters(arg0.MapFeature.Metadata.Usage.ToString())) {
          arg0.Style = StyleConfiguration;
        }
      });
    }

    protected override System.Type GetUsageType() {
      return typeof(SegmentMetadata.UsageType);
    }

    protected override void InitMapConfig() {
      base.InitMapConfig();

      // Select first wall material by default
      for (int i = 0; i < SegmentMaterialsToggles.Count; i++) {
        SegmentMaterialsToggles[i].isOn = i == 0;
      }

      // Select first roof material by default
      for (int i = 0; i < BorderMaterialsToggles.Count; i++) {
        BorderMaterialsToggles[i].isOn = i == 0;
      }

      // Style configuration has the material from default
      StyleConfiguration = ExampleDefaults.DefaultGameObjectOptions.SegmentStyle;

      // Initialize material to first option
      SegmentMaterial = SegmentMaterials.Count > 0 ? SegmentMaterials[0] : null;
      BorderMaterial = BorderMaterials.Count > 0 ? BorderMaterials[0] : null;

      SegmentWidthSlider.value = StyleConfiguration.Width;
      BorderWidthSlider.value = StyleConfiguration.BorderWidth;
      UpdateStyle();
    }

    private void UpdateStyle() {
      StyleConfiguration =
          new SegmentStyle
              .Builder {
                Material = this.SegmentMaterial,
                Width = SegmentWidthSlider.value,
                BorderMaterial = this.BorderMaterial,
                BorderWidth = BorderWidthSlider.value
              }
              .Build();
    }

    public void OnParametersValueChanged() {
      UpdateStyle();
    }

    public void OnSegmentMaterialSelected(int idx) {
      Debug.Log("OnSegmentMaterialSelected Idx:" + idx);

      if (idx < 0 || idx >= SegmentMaterials.Count)
        throw new System.Exception("Invalid option selected on the segment materials list!");
      SegmentMaterial = SegmentMaterials[idx];
      UpdateStyle();
    }

    public void OnBorderMaterialSelected(int idx) {
      Debug.Log("OnBorderMaterialSelected Idx:" + idx);

      if (idx < 0 || idx >= BorderMaterials.Count)
        throw new System.Exception("Invalid option selected on the border materials list!");
      BorderMaterial = BorderMaterials[idx];
      UpdateStyle();
    }
  }
}
