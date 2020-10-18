using UnityEngine;

namespace Extensions.Runtime
{
    public static class RendererExtensions
    {
        public static int CountCornersVisibleFrom(this RectTransform rectTransform, Camera camera)
        {
            var screenBounds = new Rect(0f, 0f, Screen.width, Screen.height); // Screen space bounds (assumes camera renders across the entire screen)
            var objectCorners = new Vector3[4];
            rectTransform.GetWorldCorners(objectCorners);
 
            var visibleCorners = 0;

            foreach (var corner in objectCorners)
            {
                var tempScreenSpaceCorner = camera.WorldToScreenPoint(corner); // Cached
                if (screenBounds.Contains(tempScreenSpaceCorner)) // If the corner is inside the screen
                {
                    visibleCorners++;
                }
            }
            
            return visibleCorners;
        }
 
        public static bool IsFullyVisibleFrom(this RectTransform rectTransform, Camera camera)
        {
            return CountCornersVisibleFrom(rectTransform, camera) == 4; // True if all 4 corners are visible
        }

        public static bool IsVisibleFrom(this RectTransform rectTransform, Camera camera)
        {
            return CountCornersVisibleFrom(rectTransform, camera) > 0; // True if any corners are visible
        }
    }
}