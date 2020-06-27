using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// This class handles the behavior of the SearchById dialog class.
  /// It displays data previously collected by the <see cref="SearchByPlaceIdUpdater" class./>
  /// </summary>
  public class SearchByIdDialog : MonoBehaviour {
    [Tooltip("The Base Map Loader handles the Maps Service initialization and basic event flow.")]
    public BaseMapLoader BaseMapLoader;

    [Tooltip("The list of available PlaceIds based on the loaded map.")]
    public Dropdown PlaceIds;

    [Tooltip("Defines the maximum amount of entries in the dropdown for performance reasons.")]
    public int MaxPlaceIdsInCache = 10;

    /// <summary>
    /// A reference to the script that keeps track of all currently loaded placeIds.
    /// The underlying data structure updates dynamically as we load a new map or region.
    /// </summary>
    private SearchByPlaceIdUpdater updater;

    /// <summary>
    /// Checks initial constraints.
    /// </summary>
    void Awake() {
      Debug.Assert(BaseMapLoader != null, "Reference to the BaseMapLoader missing!");
      Debug.Assert(PlaceIds != null, "Reference to the placeIds dropdown missing!");
    }

    /// <summary>
    /// Triggered when this script is enabled. When this happens, the list of PlaceIds in the
    /// dropdown is updated from the cache provided by the <see cref="SearchByPlaceIdUpdater"/>.
    /// </summary>
    /// <remarks>
    /// For performance reasons, we limit the amount of items in the dropdown to the first
    /// <see cref="MaxPlaceIdsInCache"/>.
    /// The default Unity DropDown widget wasn't created to accommodate large data sets.
    /// </remarks>
    /// <exception cref="Exception">
    /// Throws an exception if a <see cref="SearchByPlaceIdUpdater"/> cannot be found on the
    /// <see cref="GameObject"/>.
    /// </exception>
    void OnEnable() {
      if (BaseMapLoader == null || PlaceIds == null) {
        return;
      }

      PlaceIds.ClearOptions();

      updater = BaseMapLoader.gameObject.GetComponent<SearchByPlaceIdUpdater>();

      if (updater == null) {
        throw new System.Exception(
            "Can't find the Search By Place Id Updater component in the attached maps loader.");
      }

      // Populate the dropdown with data from the search by id updater
      List<Dropdown.OptionData> options = new List<Dropdown.OptionData>();

      int counter = 0;

      foreach (string placeId in updater.PlaceIdToGameObjectDict.Keys) {
        if (counter > MaxPlaceIdsInCache)
          break;
        options.Add(new Dropdown.OptionData(placeId));
        counter++;
      }

      PlaceIds.AddOptions(options);
    }

    /// <summary>
    /// Hides the dialog when the close button is triggered.
    /// </summary>
    public void OnClose() {
      gameObject.SetActive(false);
    }
  }
}
