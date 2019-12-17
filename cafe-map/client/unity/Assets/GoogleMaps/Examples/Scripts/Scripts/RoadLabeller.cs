using System.Collections.Generic;
using Google.Maps.Feature;
using Google.Maps.Feature.Shape;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Component for adding <see cref="Label"/>s to show the names of all roads created by the Maps
/// Unity SDK.
/// </summary>
/// <remarks>
/// The specific LabelPrefab used in this example contains a <see cref="Text"/> element with a
/// custom shader assigned. This shader makes sure this <see cref="Text"/> element is displayed on
/// top of all in-scene geometry (even if it is behind or inside said geometry). Examine the shader
/// on this prefab to find out how this is achieved.
/// </remarks>
public sealed class RoadLabeller : MonoBehaviour {
  [Tooltip("Canvas to create road name-labels under.")]
  public Canvas Canvas;

  [Tooltip("Prefab to show a road's name. Must contain a UI.Text element as a child.")]
  public Label LabelPrefab;

  [Tooltip("Start all Labels faded out?")]
  public bool StartFaded;

  [Tooltip("Should the Label which is the most closely aligned to the Camera be the most visible? "
    + "This helps reduce visual clutter by allowing all Labels not been directly looked at to be "
    + "faded out.")]
  public bool FadeWithView;

  /// <summary>
  /// All created roads, stored by name.
  /// </summary>
  private readonly Dictionary<string, Road> Roads = new Dictionary<string, Road>();

  /// <summary>Make sure all required parameters are given.</summary>
  private void Awake() {
    // Make sure a canvas has been specified.
    if (Canvas == null) {
      Debug.LogError(ExampleErrors.MissingParameter(this, Canvas, "Canvas", "to show road names"));
      return;
    }

    // Make sure a prefab has been specified.
    if (LabelPrefab == null) {
      Debug.LogError(ExampleErrors.MissingParameter(this, LabelPrefab, "LabelPrefab",
          "to use to display names above roads in scene"));
    }
  }

  /// <summary>Show a name for a newly created road.</summary>
  /// <param name="roadGameObject"><see cref="GameObject"/> containing created road.</param>
  /// <param name="roadData">
  /// <see cref="Segment"/> containing roads data, passed as eventArgs.MapFeature in
  /// <see cref="Google.Maps.Event.SegmentEvents.DidCreate"/> event.
  /// </param>
  internal void NameRoad(GameObject roadGameObject, Segment roadData) {
    // Get the details of the specific road the Maps Unity SDK has just created.
    Line roadLine = roadData.Shape.Lines[0];

    // Get the name of this road, and see if have already stored a road with this name.
    string roadName = roadData.Metadata.Name;
    if (Roads.ContainsKey(roadName)) {
      // Add road as a new chunk of this already stored road of the same name.
      Roads[roadName].Add(roadGameObject, roadLine);
      return;
    }

    // Create a label to show this brand new road's name.
    Label roadLabel = Instantiate(LabelPrefab, Canvas.transform);
    roadLabel.StartFadedOut = StartFaded;
    roadLabel.FadeWithView = FadeWithView;
    roadLabel.SetText(roadName);

    // Store this road as the first chunk of a new road.
    Roads.Add(roadName, new Road(roadLabel, roadGameObject, roadLine));
  }

  /// <summary>Fade in all road <see cref="Label"/>s.</summary>
  internal void ShowRoadNames() {
    Label.StartFadingAllIn();
  }

  /// <summary>Fade out all road <see cref="Label"/>s.</summary>
  internal void HideRoadNames() {
    Label.StartFadingAllOut();
  }

  /// <summary>
  /// All chunks making up a single road.
  /// </summary>
  private sealed class Road {
    /// <summary>
    /// Label showing the name of this road.
    /// </summary>
    private readonly Transform Label;

    /// <summary>
    /// The midpoints of all individual lines making up this road.
    /// </summary>
    private readonly List<Vector3> LineMidPoints = new List<Vector3>();

    /// <summary>
    /// Store a new road, starting with a given first chunk.
    /// </summary>
    /// <param name="nameLabel">Label showing road's name.</param>
    /// <param name="firstChunk">First piece of this road.</param>
    /// <param name="firstChunkLine">Line defining this first chunk's shape.</param>
    public Road(Label nameLabel, GameObject firstChunk, Line firstChunkLine) {
      Label = nameLabel.transform;
      Add(firstChunk, firstChunkLine);
    }

    /// <summary>
    /// Add a new chunk to this road, repositioning label to new, collective center.
    /// </summary>
    /// <param name="newChunk">New piece of this road.</param>
    /// <param name="newChunkLine">Line defining this new chunk's shape.</param>
    public void Add(GameObject newChunk, Line newChunkLine) {
      // Store the midpoints of the individual straight lines making up this new chunk of the road.
      for (int i = 0; i < newChunkLine.Vertices.Length - 1; i++) {
        // Line vertices need to be converted from 2D coordinates (x and y) to 3D coordinates
        // (x and z, i.e. the ground plane where y = 0).
        Vector3 lineStart =
            new Vector3(newChunkLine.Vertices[i].x, 0f, newChunkLine.Vertices[i].y);
        Vector3 lineEnd =
            new Vector3(newChunkLine.Vertices[i + 1].x, 0f, newChunkLine.Vertices[i + 1].y);

        // Convert from local space to world space by adding the world space position of the
        // GameObject containing these lines.
        lineStart += newChunk.transform.position;
        lineEnd += newChunk.transform.position;

        // Store the midpoint of this start and end as the midpoint of this line.
        Vector3 lineMidpoint = (lineStart + lineEnd) / 2f;
        LineMidPoints.Add(lineMidpoint);
      }

      // Calculate collective center of all road lines.
      Vector3 center = Vector3.zero;
      foreach (Vector3 lineMidpoint in LineMidPoints) {
        center += lineMidpoint;
      }
      float countAsFloat = LineMidPoints.Count;
      center /= countAsFloat;

      // Determine which line is closest to the collective center. This is so we can place the label
      // over this center-most line (rather than at the exact collective center, which may not be
      // over any individual part of this road).
      int? closestLine = null;
      float closestDistance = 0f;
      for (int i = 0; i < LineMidPoints.Count; i++) {
        float currentDistance = Vector3.Distance(center, LineMidPoints[i]);
        if (!closestLine.HasValue || currentDistance < closestDistance) {
          closestLine = i;
          closestDistance = currentDistance;
        }
      }
      Label.position = LineMidPoints[closestLine.Value];
    }
  }
}
