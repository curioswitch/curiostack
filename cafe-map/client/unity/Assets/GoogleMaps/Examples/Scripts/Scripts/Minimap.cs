using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Simple script for automatically connecting <see cref="Camera.targetTexture"/> to a given
/// <see cref="RawImage"/> component, allowing the view from the <see cref="Camera"/> to be
/// displayed as an on screen UI element.
/// </summary>
[RequireComponent(typeof(Camera))]
public class Minimap : MonoBehaviour {
  [Tooltip("UI Image used to display Minimap.")]
  public RawImage Image;

  /// <summary>On start, make sure Minimap is properly setup.</summary>
  private void Awake() {
    // Make sure this Minimap Camera is not tagged as the scene's Main Camera. Calling the static
    // variable Camera.main actually just searches the current scene for all Camera's tagged
    // "MainCamera", returning the first result. Thus if we have multiple Camera's tagged as
    // "MainCamera", using Camera.main can give unexpected results. This is why we make sure this
    // secondary, Minimap Camera is not tagged as the scene's Main Camera.
    Camera minimapCamera = GetComponent<Camera>();
    minimapCamera.tag = "Untagged";

    // Make sure a Render Texture is connected to the Minimap Camera (as its "Target Texture"). We
    // use this Render Texture to save the Camera's rendered view each frame, storing it in a
    // Render Texture that can be displayed in a UI element on screen.
    if (minimapCamera.targetTexture == null) {
      Debug.LogErrorFormat("No {0} defined for {1}.{2}.Target Texture. {1}.{3} needs a {0} in "
          + "order to be able to display its texture in a UI element.",
          typeof(RenderTexture), name, typeof(Camera), GetType());
      return;
    }

    // Make sure an Image component has been given to display the Minimap in the on-screen UI.
    if (Image == null) {
      Debug.LogErrorFormat("No {0} defined for {1}.{2}.Image. {2} needs a {0} in order to "
        + "be able to display its texture in a UI element.",
        typeof(RawImage), name, GetType());
      return;
    }

    // Make sure our Image's texture is set to the Minimap's Render Texture, so that the Minimap
    // Camera's output will be displayed in this Image.
    if (Image.texture == null || Image.texture != minimapCamera.targetTexture) {
      Debug.LogWarningFormat("{0}.{1}.Target Texture was not connected to {2}.{3}.Texture. {0}.{4} "
        + "needs these two to be connected to display the minimap in {2}'s {3}.\n"
        + "Making connection now, but to avoid this warning, drag the {5} set as {0}.{1}."
        + "Target Texture into {2}.{3}.Texture.",
        name, typeof(Camera), Image, Image.GetType(), GetType(), typeof(RenderTexture));
      Image.texture = minimapCamera.targetTexture;
    }
  }
}
