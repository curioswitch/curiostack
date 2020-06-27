using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Class for showing an in-scene, <see cref="Camera.main"/> aligned Label.
  /// </summary>
  [RequireComponent(typeof(CanvasGroup))]
  public class Label : MonoBehaviour {
    [Tooltip("Text element to show this Label's text in.")]
    public Text Text;

    [Tooltip(
        "Should this Label keep its starting x-rotation (i.e. should it stay at its current " +
        "up/down tilt amount, and just turn in y-axis to face Camera)?")]
    public bool TurnOnly;

    [Tooltip(
        "Should the Label which is the most closely aligned to the Camera be the most visible? " +
        "This helps reduce visual clutter by allowing all Labels not been directly looked at to " +
        "be faded out.")]
    public bool FadeWithView;

    [Tooltip("Total time this Label should take to fade in or out?")]
    public float FadeTime = 1f;

    [Tooltip("Should this Label start hidden?")]
    public bool StartFadedOut;

    [Tooltip("Should this Label automatically start fading in as soon as SetText is called?")]
    public bool AutoFadeIn;

    /// <summary>
    /// Is this <see cref="Label"/> currently tracking <see cref="Camera.main"/>, i.e. is this
    /// <see cref="Label"/> continually making sure it is aligned to <see cref="Camera.main"/>?
    /// </summary>
    /// <remarks>
    /// This flag is used to ensure that the coroutine for <see cref="Camera.main"/>-tracking is not
    /// redundantly started when it is already running.
    /// </remarks>
    private bool IsTrackingCamera;

    /// <summary>Is this <see cref="Label"/> currently fading in or out?</summary>
    /// <remarks>
    /// This flag is used to ensure that the fading-facing coroutine is not redundantly started when
    /// when it is already running.
    /// </remarks>
    private bool IsFading;

    /// <summary>Has a <see cref="Text"/> component been defined?</summary>
    /// <remarks>
    /// This is set to null before this check is performed, allowing this check to only be performed
    /// once, with the result being stored for future re-use.
    /// </remarks>
    private bool? HasText;

    /// <summary>Is this <see cref="Label"/> currently fading in (true) or out (false)?</summary>
    private bool FadingIn;

    /// <summary>Time into current fade.</summary>
    private float FadeTimer;

    /// <summary>
    /// Value to multiply alpha by to achieve desired alpha level? This is used when alpha is
    /// controlled by view angle, to allow manual changes in alpha or fading animations to be
    /// combined with this view-dependant alpha logic.
    /// </summary>
    /// <remarks>
    /// This value starts at 1f to ensure no change in alpha until a new value is set.
    /// </remarks>
    private float AlphaMultiplier = 1f;

    /// <summary>
    /// Required <see cref="CanvasGroup"/>, used for optionally fading all parts of this
    /// <see cref="Label"/> in or out as <see cref="Camera.main"/> looks towards or away from it.
    /// </summary>
    /// <remarks>
    /// This variable is auto-found on first access, allowing this <see cref="CanvasGroup"/> to be
    /// accessed at any time without having to check if it is null.
    /// </remarks>
    private CanvasGroup CanvasGroup {
      get {
        if (_CanvasGroup == null) {
          _CanvasGroup = GetComponent<CanvasGroup>();
        }

        return _CanvasGroup;
      }
    }

    /// <summary>
    /// Actual stored <see cref="CanvasGroup"/>. This is stored in this way to allow
    /// <see cref="Label.CanvasGroup"/> to be used at any time without having to check if it is
    /// null.
    /// </summary>
    private CanvasGroup _CanvasGroup;

    /// <summary>Has this <see cref="Label"/> been setup yet?</summary>
    /// <remarks>Allows setup to be intelligently called when needed?</remarks>
    private bool IsSetup;

    /// <summary>
    /// All <see cref="Label"/>s in the current scene. Used to perform group actions, like starting
    /// all <see cref="Label"/>s fading in or out together, or hiding the <see cref="Text"/> part of
    /// all <see cref="Label"/>s.
    /// </summary>
    private static readonly List<Label> AllLabels = new List<Label>();

    /// <summary>
    /// Setup this <see cref="Label"/> if (and only if) have not already done so.
    /// </summary>
    private void Start() {
      TrySetup();
    }

    /// <summary>
    /// Setup this <see cref="Label"/> if (and only if) have not already done so.
    /// </summary>
    protected void TrySetup() {
      // Skip if have already setup this Label.
      if (IsSetup) {
        return;
      }

      // Add to list of all Labels, so can fade all Labels in/out together.
      AllLabels.Add(this);

      // If this Label is meant to start faded out, make invisible now.
      if (StartFadedOut) {
        AlphaMultiplier = 0f;
        CanvasGroup.alpha = 0f;
      }

      // Flag this label as now set up.
      IsSetup = true;
    }

    /// <summary>Set the specific text to display on this <see cref="Label"/>.</summary>
    /// <param name="text">Text to show on this <see cref="Label"/>.</param>
    internal void SetText(string text) {
      // Make sure this label is setup.
      TrySetup();

      // Print an error if no Text element has been given.
      if (!CanFindText()) {
        // Note: 'name' and 'GetType()' just give the name of the GameObject this script is on, and
        // the name of this script respectively.
        Debug.LogErrorFormat(
            "No Text element set for {0}.{1}, which requires a UI.Text element to " +
                "show given text \"{2}\".",
            name,
            GetType(),
            text);

        return;
      }

      // Name this GameObject based on given text (to make debugging easier) and display the text.
      gameObject.name = string.Concat("Label: ", text);
      Text.text = text;

      // Start this Label tracking the Camera (unless already doing so).
      if (!IsTrackingCamera) {
        StartCoroutine(TrackCamera());
      }

      // Optionally start fading in.
      if (AutoFadeIn) {
        StartFadingIn();
      }
    }

    /// <summary>Set a new alpha value for this <see cref="Label"/>.</summary>
    /// <param name="newAlpha">
    /// New alpha value to set, assumed to be a valid value (within the range of 0f to 1f).
    /// </param>
    internal void SetAlpha(float newAlpha) {
      // Make sure this label is setup.
      TrySetup();

      // Print an error if no Text element has been given.
      if (!CanFindText()) {
        Debug.LogErrorFormat(
            "No Text element set for {0}.{1}, which requires a UI.Text element to " +
                "apply new alpha value of {2:N2} to.",
            name,
            GetType(),
            newAlpha);

        return;
      }

      // Set desired alpha level.
      ApplyAlpha(newAlpha);

      // Start this Label tracking the Camera (unless already doing so).
      if (!IsTrackingCamera) {
        StartCoroutine(TrackCamera());
      }
    }

    /// <summary>Start this <see cref="Label"/> fading in smoothly over time.</summary>
    internal void StartFadingIn() {
      StartFading(true);
    }

    /// <summary>Start this <see cref="Label"/> fading in smoothly over time.</summary>
    internal void StartFadingOut() {
      StartFading(false);
    }

    /// <summary>Start all <see cref="Label"/>s fading in smoothly over time.</summary>
    internal static void StartFadingAllIn() {
      StartFadingAll(true);
    }

    /// <summary>Start all <see cref="Label"/>s fading in smoothly over time.</summary>
    internal static void StartFadingAllOut() {
      StartFadingAll(false);
    }

    /// <summary>Shrink/grow all <see cref="Label"/>s by a given multiplier.</summary>
    /// <remarks>
    /// This is a class member. Calling <see cref="Label.ScaleAll"/> iterates over all in-scene
    /// <see cref="Label"/>s, scaling each.
    /// </remarks>
    /// <param name="multiplier">
    /// Value to multiply current scale by for all <see cref="Label"/>s.
    /// </param>
    internal static void ScaleAll(float multiplier) {
      foreach (Label label in AllLabels) {
        label.Scale(multiplier);
      }
    }

    /// <summary>Hide all <see cref="Label"/>s.</summary>
    /// <remarks>
    /// This is a class member. Calling <see cref="Label.HideAll"/> iterates over all in-scene
    /// <see cref="Label"/>s, hiding each.
    /// </remarks>
    /// <param name="hide">Optionally set to true to hide, false to show.</param>
    internal static void HideAll(bool hide = true) {
      HideOrShowAll(hide);
    }

    /// <summary>Hide all <see cref="Label"/>s.</summary>
    /// <remarks>
    /// This is a class member. Calling the static method <see cref="Label.ShowAll"/> iterates over
    /// all in-scene <see cref="Label"/>s, showing each.
    /// </remarks>
    /// <param name="show">Optionally set to true to show, false to hide.</param>
    internal static void ShowAll(bool show = true) {
      HideOrShowAll(!show);
    }

    /// <summary>Hide the <see cref="Text"/> part of all <see cref="Label"/>s.</summary>
    /// <remarks>
    /// This is a class member. Calling the static method <see cref="Label.HideAllText"/> iterates
    /// over all in-scene <see cref="Label"/>s, hiding the <see cref="Text"/> part of each.
    /// </remarks>
    /// <param name="hide">Optionally set to true to hide, false to show.</param>
    internal static void HideAllText(bool hide = true) {
      HideOrShowAllText(hide);
    }

    /// <summary>Hide the <see cref="Text"/> part of all <see cref="Label"/>s.</summary>
    /// <remarks>
    /// This is a class member. Calling the static method <see cref="Label.ShowAllText"/> iterates
    /// over all in-scene <see cref="Label"/>s, showing the <see cref="Text"/> part of each.
    /// </remarks>
    /// <param name="show">Optionally set to true to hide, false to show.</param>
    internal static void ShowAllText(bool show = true) {
      HideOrShowAllText(!show);
    }

    /// <summary>Turn to face the <see cref="Camera.main"/> every frame.</summary>
    /// <remarks>
    /// If <see cref="FadeWithView"/> is enabled, this <see cref="Label"/> will also fade in or out
    /// as <see cref="Camera.main"/> looks towards or away from it.
    /// </remarks>
    private IEnumerator TrackCamera() {
      // Flag that this coroutine has started (so it will not be redundantly restarted later).
      IsTrackingCamera = true;

      // Start facing Camera.
      while (true) {
        // Get the current rotation of the Camera. If Labels are meant to turn only (y-rotation
        // only, ignoring x-rotation) then remove the x-component of this rotation.
        Quaternion cameraRotation;

        if (TurnOnly) {
          Vector3 cameraEuler = Camera.main.transform.eulerAngles;
          cameraRotation = Quaternion.Euler(0f, cameraEuler.y, 0f);
        } else {
          cameraRotation = Camera.main.transform.rotation;
        }

        // Match Label's rotation to that of the Camera. This is so all Labels will be parallel to
        // each other, yet all be readable to the Camera. Also note that, for any other kind of
        // GameObject, the inverse of the Camera's rotation should be used, so that this Label is
        // facing the opposite direction of the Camera (i.e. towards the Camera). But Unity creates
        // all UI elements and quads with their textured side facing backwards. So to make any UI
        // element look at the Camera, we have to make it face the same direction as the Camera.
        transform.rotation = cameraRotation;

        // Optionally fade in/out based on view angle, so that the Label closest to the view center
        // is the clearest, and all other Labels are semi-transparent.
        if (FadeWithView) {
          FadeWithViewAngle();
        }

        // Wait for next frame.
        yield return null;
      }
    }

    /// <summary>Fade this <see cref="Label"/> in or out over time.</summary>
    private IEnumerator Fade() {
      // Flag that this coroutine has started (so it will not be redundantly restarted later).
      IsFading = true;

      // Start fading in/out.
      while (true) {
        // Count up until end of fading animation.
        FadeTimer += Time.smoothDeltaTime;

        // If have reached end, apply final alpha value (fully faded in or out).
        if (FadeTimer >= FadeTime) {
          ApplyAlpha(FadingIn ? 1f : 0f);
          IsFading = false;

          break;
        }

        // If still fading, determine current fade value.
        float fadePercent = FadeTimer / FadeTime;

        // Convert to alpha based on whether fading in or out, smoothing result towards 1f so will
        // smoothly approach/leave fully opaque and apply.
        float alpha = FadingIn ? fadePercent : 1f - fadePercent;
        alpha = Smooth(alpha);
        ApplyAlpha(alpha);

        // Wait for next frame.
        yield return null;
      }
    }

    /// <summary>Start this <see cref="Label"/> fading in or out smoothly over time.</summary>
    /// <params name="fadeIn">True to start fading in, false to start fading out.</params>
    private void StartFading(bool fadeIn) {
      // Make sure this label is setup.
      TrySetup();

      // Print an error if no Text element has been given.
      if (!CanFindText()) {
        Debug.LogErrorFormat(
            "No Text element set for {0}.{1}, which requires a UI.Text element to " +
                "start fading {2}.",
            name,
            GetType(),
            fadeIn ? "in" : "out");

        return;
      }

      // Setup start of fade.
      AlphaMultiplier = 0f;
      FadeTimer = 0f;
      FadingIn = fadeIn;

      // Start this Label fading (unless already fading). Note that if already fading, then the
      // change in variables above will set up the new fade with the existing coroutine.
      if (!IsFading) {
        StartCoroutine(Fade());
      }

      // Make this Label always face the Camera (unless already facing the Camera).
      if (!IsTrackingCamera) {
        StartCoroutine(TrackCamera());
      }
    }

    /// <summary>Start all <see cref="Label"/>s fading in or out smoothly over time.</summary>
    /// <remarks>
    /// This is a class member. Calling the static method <see cref="Label.StartFadingAll"/>
    /// iterates over all in-scene <see cref="Label"/>s, calling <see cref="StartFading"/> on each.
    /// </remarks>
    /// <params name="fadeIn">True to start fading in, false to start fading out.</params>
    private static void StartFadingAll(bool fadeIn) {
      // Print an error if there are no Labels to start fading.
      if (AllLabels.Count == 0) {
        Debug.LogErrorFormat(
            "No {0} found in the current scene, so cannot start fading them all " + "{1}.",
            typeof(Label),
            fadeIn ? "in" : "out");

        return;
      }

      // Traverse list of all Labels in the scene, starting fading them all in.out. Labels are
      // traversed in reverse order so that any null/deleted Labels can be removed without affecting
      // future list-indices.
      for (int i = AllLabels.Count - 1; i >= 0; i--) {
        if (AllLabels[i] == null) {
          AllLabels.RemoveAt(i);
        } else {
          AllLabels[i].StartFading(fadeIn);
        }
      }
    }

    ///< summary>
    /// Set desired alpha level. If alpha is controlled by view angle, apply alpha through the use
    /// of a multiplier, otherwise apply alpha directly to <see cref="CanvasGroup"/> now.
    /// </summary>
    private void ApplyAlpha(float newAlpha) {
      if (FadeWithView) {
        AlphaMultiplier = newAlpha;
      } else {
        CanvasGroup.alpha = newAlpha;
      }
    }

    /// <summary>
    /// Fade all parts of this <see cref="Label"/> in or out as <see cref="Camera.main"/> faces
    /// towards or away from this <see cref="Label"/>.
    /// </summary>
    /// <remarks>
    /// This is to avoid the screen becoming too visually cluttered with fully-opaque
    /// <see cref="Label"/>s, naturally highlighting the <see cref="Label"/>s
    /// <see cref="Camera.main"/> is looking at and fading out the rest.
    /// </remarks>
    private void FadeWithViewAngle() {
      // Determine how close the Camera is to directly looking at this Label.
      Vector3 direction = (Text.transform.position - Camera.main.transform.position).normalized;
      float dotProduct = Vector3.Dot(Camera.main.transform.forward, direction);
      float angle = Mathf.Acos(dotProduct) * Mathf.Rad2Deg;

      // Convert angle to an alpha value of 1f when looking directly at this Label, and nearly
      // transparent (0.1f) when looking 45 degrees or more away from this Label.
      float alpha;

      if (angle > 45f) {
        alpha = 0.1f;
      } else {
        alpha = (1f - angle / 45f) * 0.9f + 0.1f;

        // Smooth alpha towards 1f, so Label will stay at nearly 100% alpha for longer when looking
        // roughly near Label.
        alpha = Smooth(alpha);
      }

      // Apply alpha to Canvas Group to fade Label in/out. Multiply by alpha multiplier to allow
      // alpha to be influenced by manual changes or by fading in/out.
      CanvasGroup.alpha = alpha * AlphaMultiplier;
    }

    /// <summary>Shrink/grow this <see cref="Label"/> by a given multiplier.</summary>
    /// <param name="multiplier">Value to multiply current scale by.</param>
    private void Scale(float multiplier) {
      // Make sure this label is setup before adjusting its scale
      TrySetup();

      gameObject.transform.localScale *= multiplier;
    }

    /// <summary>Hide/show all <see cref="Label"/>s.</summary>
    /// This is a class member. Calling the static method <see cref="Label.HideOrShowAll"/> iterates
    /// over all in-scene <see cref="Label"/>s, calling <see cref="HideOrShow"/> on each.
    /// <param name="hide">True to hide, false to show if hidden.</param>
    private static void HideOrShowAll(bool hide) {
      foreach (Label label in AllLabels) {
        label.HideOrShow(hide);
      }
    }

    /// <summary>Hide/show this <see cref="Label"/>.</summary>
    /// <param name="hide">True to hide, false to show if hidden.</param>
    private void HideOrShow(bool hide) {
      // Make sure this label is setup, then activate/deactivate it's GameObject to show/hide it.
      TrySetup();
      gameObject.SetActive(!hide);
    }

    /// <summary>Hide/show the <see cref="Text"/> part of all <see cref="Label"/>s.</summary>
    /// <remarks>
    /// This is a class member. Calling the static method <see cref="Label.HideOrShowAllText"/>
    /// iterates over all in-scene <see cref="Label"/>s, calling <see cref="HideOrShowText"/> on
    /// each.
    /// </remarks>
    /// <param name="hide">True to hide text, false to show if hidden.</param>
    private static void HideOrShowAllText(bool hide) {
      foreach (Label label in AllLabels) {
        label.HideOrShowText(hide);
      }
    }

    /// <summary>Hide/show the <see cref="Text"/> part of this <see cref="Label"/>.</summary>
    /// <param name="hide">True to hide text, false to show if hidden.</param>
    private void HideOrShowText(bool hide) {
      // Make sure this label is setup, then if a text element can be found, hide/show it.
      TrySetup();

      if (CanFindText()) {
        Text.gameObject.SetActive(!hide);
      }
    }

    /// <summary>See if a <see cref="Text"/> element has been given.</summary>
    private bool CanFindText() {
      // If have not already done so, check for Text element, storing result for later re-use if
      // required.
      if (!HasText.HasValue) {
        HasText = Text != null;
      }

      return HasText.Value;
    }

    /// <summary>Return a given value smoothed towards 1f.</summary>
    private static float Smooth(float value) {
      return Mathf.Sin(value * Mathf.PI / 2f);
    }
  }
}
