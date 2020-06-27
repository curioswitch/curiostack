using System.Collections.Generic;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature.Shape;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Adjusts the position of the <see cref="Label"/> on this GameObject to be over the center-most
  /// segment of a road whenever a new segment is added.
  /// </summary>
  [RequireComponent(typeof(Label))]
  public class RoadLabelMover : MonoBehaviour {
    /// <summary>
    /// A reference to the required <see cref="Label"/> attached to the GameObject to which this
    /// component is attached.
    /// </summary>
    private Label Label;

    /// <summary>
    /// The midpoints of all individual lines making up this road.
    /// </summary>
    private readonly List<Vector3> LineMidPoints = new List<Vector3>();

    void Awake() {
      Label = GetComponent<Label>();
    }

    /// <summary>
    /// Add a new chunk to this road, repositioning label to new, collective center.
    /// </summary>
    /// <param name="newChunk">New piece of this road.</param>
    /// <param name="newChunkLine">Line defining this new chunk's shape.</param>
    public void Add(GameObject newChunk, Line newChunkLine) {
      // Store the midpoints of the individual straight lines making up this new chunk of the
      // road.
      for (int i = 0; i < newChunkLine.Vertices.Length - 1; i++) {
        // Line vertices need to be converted from 2D coordinates (x and y) to 3D coordinates
        // (x and z, i.e. the ground plane where y = 0).
        Vector3 lineStart = new Vector3(newChunkLine.Vertices[i].x, 0f, newChunkLine.Vertices[i].y);

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

      // Determine which line is closest to the collective center. This is so we can place the
      // label over this center-most line (rather than at the exact collective center, which may
      // not be over any individual part of this road).
      int? closestLine = null;
      float closestDistance = 0f;

      for (int i = 0; i < LineMidPoints.Count; i++) {
        float currentDistance = Vector3.Distance(center, LineMidPoints[i]);

        if (!closestLine.HasValue || currentDistance < closestDistance) {
          closestLine = i;
          closestDistance = currentDistance;
        }
      }

      Label.transform.position = LineMidPoints[closestLine.Value];

      // As the position has changed, fade back in.
      Label.StartFadingIn();
    }
  }
}
