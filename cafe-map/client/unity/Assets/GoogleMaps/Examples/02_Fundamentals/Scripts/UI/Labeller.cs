using System.Collections.Generic;
using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Component for adding <see cref="Label"/>s to show the names of particular objects.
  /// </summary>
  /// <remarks>
  /// The specific LabelPrefab used in this example contains a <see cref="Text"/> element with a
  /// custom shader assigned. This shader makes sure this <see cref="Text"/> element is displayed on
  /// top of all in-scene geometry (even if it is behind or inside said geometry). Examine the
  /// shader on this prefab to find out how this is achieved.
  /// </remarks>
  public class Labeller : MonoBehaviour {
    [Tooltip("Canvas on which to create name-labels.")]
    public Canvas Canvas;

    [Tooltip("Prefab to show an object's name. Must contain a UI.Text element as a child.")]
    public Label LabelPrefab;

    [Tooltip("Start all Labels faded out?")]
    public bool StartFaded;

    [Tooltip(
        "Should the Label which is the most closely aligned to the Camera be the most visible? " +
        "This helps reduce visual clutter by allowing all Labels not been directly looked at to " +
        "be faded out.")]
    public bool FadeWithView;

    /// <summary>All created object <see cref="Label"/>s, stored by a key.</summary>
    /// <remarks>
    /// We use this to ensure only one <see cref="Label"/> is shown per key, i.e. if
    /// multiple objects with the same key are loaded, only one will have a <see cref="Label"/>
    /// created for it.
    /// </remarks>
    protected readonly Dictionary<string, Label> LabelsByKey = new Dictionary<string, Label>();

    /// <summary>Make sure all required parameters are given.</summary>
    protected virtual void Awake() {
      // Make sure a canvas has been specified.
      if (Canvas == null) {
        Debug.LogError(ExampleErrors.MissingParameter(this, Canvas, "Canvas", "to show labels"));

        return;
      }

      // Make sure a prefab has been specified.
      if (LabelPrefab == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, LabelPrefab, "Label", "to use to display labels for objects in scene"));
      }
    }

    /// <summary>
    /// Deletes all name tags from the scene.
    /// Also clears local references.
    /// </summary>
    public void ClearNames() {
      LabelsByKey.Clear();

      if (Canvas != null) {
        foreach (Transform child in Canvas.transform)
          Destroy(child.gameObject);
      }
    }

    /// <summary>Show a name for a newly created created object.</summary>
    /// <param name="objectGameObject"><see cref="GameObject"/> containing created object.
    /// </param>
    /// <param name="objectKey">Key used to uniquely identify this object.</param>
    /// <param name="objectName">Name to display on this object (skipped if null/empty).</param>
    public Label NameObject(GameObject objectGameObject, string objectKey, string objectName) {
      // Skip showing name if it is null.
      if (string.IsNullOrEmpty(objectName)) {
        return null;
      }

      // See if a Label has already been created for this object, re-using it if so. This is to
      // ensure that when new objects are added to an existing key, only one label is created
      // rather than one for each object.
      Label objectLabel;

      if (LabelsByKey.ContainsKey(objectKey)) {
        objectLabel = LabelsByKey[objectKey];
      } else {
        // Create a Label to show this brand new object's name.
        objectLabel = Instantiate(LabelPrefab, Canvas.transform);
        objectLabel.StartFadedOut = StartFaded;
        objectLabel.FadeWithView = FadeWithView;
        objectLabel.SetText(objectName);

        // Add this new label to the Dictionary of all stored object Labels.
        LabelsByKey.Add(objectKey, objectLabel);
      }

      objectLabel.transform.position = objectGameObject.transform.position;

      // Start fading the label in if it began faded out.
      if (StartFaded) {
        objectLabel.StartFadingIn();
      }

      return objectLabel;
    }
  }
}
