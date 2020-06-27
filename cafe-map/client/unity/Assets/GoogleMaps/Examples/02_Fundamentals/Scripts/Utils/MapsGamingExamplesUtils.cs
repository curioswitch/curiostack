using System.Collections.Generic;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Provides a few utility functions to manipulate augmented geometry.
  /// </summary>
  public class MapsGamingExamplesUtils {
    /// <summary>
    /// Places a given marker on top of a given target <see cref="GameObject"/> in world space
    /// coordinates.
    /// </summary>
    /// <remarks>
    /// We use the target's renderer bounds as the base for the placement of the marker.
    /// A raycast gives us the Y-axis value.
    /// </remarks>
    /// <param name="target">The target to place the marker on.</param>
    /// <param name="marker">The transform of the marker to place.</param>
    public static void PlaceUIMarker(GameObject target, Transform marker) {
      Renderer renderer = target.GetComponent<Renderer>();

      if (renderer == null) {
        throw new System.Exception(
            "Can't get the bounds of target object. Object does not have a renderer.");
      }

      Bounds targetBounds = renderer.bounds;

      // Ray-cast down to the target's center to get the height of the target at it's center.
      // This is so we can place the Label on the roof of the building. Note that this is done to
      // ensure the Label is touching the roof at its center (instead of using bounds, where a
      // building arial in its corner would give an invalid building height).
      Vector3 labelOrigin;
      Vector3 targetPosition = target.transform.position;
      Ray centerRay = new Ray(targetPosition + Vector3.up * 1000f, Vector3.down);

      RaycastHit[] centerHits = Physics.RaycastAll(centerRay, 1001f);

      // Note that ray-casting is done from 1km above the building, with a max distance of 1.001km.
      // We do this to ensure the ray-cast starts above the building (in order to hit its roof).

      // If there are no hits, then default to the center of the GameObject's upper bounds.
      if (centerHits.Length == 0) {
        labelOrigin = targetPosition + Vector3.up * targetBounds.size.y;
      } else {
        // Check hits to find the highest hit of this building.
        int? highestHit = null;
        float highestHitDistance = 0f;

        for (int i = 0; i < centerHits.Length; i++) {
          if (centerHits[i].collider.gameObject == target) {
            if (!highestHit.HasValue || centerHits[i].distance < highestHitDistance) {
              highestHit = i;
              highestHitDistance = centerHits[i].distance;
            }
          }
        }

        // If at least one hit was for this building, use this hit as the roof-center point where
        // the label is placed. If no hits were for this building, then default to the center of the
        // building's upper bounds.
        labelOrigin = highestHit.HasValue ? centerHits[highestHit.Value].point
                                          : targetPosition + Vector3.up * targetBounds.max.y;
      }

      marker.transform.position = labelOrigin;
    }

    /// <summary>
    /// Returns the subset of the given <see cref="Transform"/>s collection for which the elements
    /// fall outside the circle identified by center + radius.
    /// </summary>
    /// <param name="source">The collection of <see cref="Transform"/>s to filter.</param>
    /// <param name="center">The center of the circle.</param>
    /// <param name="radius">The radius of the circle.</param>
    public static List<Transform> GetAllObjectsOutsideCircleRegion(
        List<Transform> source, Vector3 center, float radius) {
      List<Transform> results = new List<Transform>();

      foreach (Transform transform in source) {
        if (Vector3.Distance(transform.position, center) > radius) {
          results.Add(transform);
        }
      }

      return results;
    }
  }
}
