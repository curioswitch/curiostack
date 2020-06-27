using System;
using System.Collections;
using Google.Maps.Examples.Shared;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// Class for connecting a <see cref="Slider"/> to an <see cref="Action"/>, animating the
  /// <see cref="Slider"/> moving up and down until player input is detected.
  /// </summary>
  public sealed class SliderController : MonoBehaviour {
    [Tooltip("Actual UI Slider.")]
    public Slider Slider;

    [Tooltip(
        "Time in seconds to animate slider moving up and down. Animation continues until input " +
        "is received from the player.")]
    public float AnimationTime = 6f;

    [Tooltip(
        "Apply smoothing to ends of slider animation (true) or leave ends as linear/sharp " +
        "(false)?")]
    public bool Smooth = true;

    [Tooltip("Move up, then down, then up (true), or up, then reset and up again (false)")]
    public bool BackAndForth = true;

    /// <summary>Is this <see cref="Slider"/> current animating?</summary>
    internal bool IsAnimating { get; private set; }

    /// <summary>
    /// Action called when this <see cref="Slider"/>'s value is changed, either as part of animation
    /// or in response to player input.
    /// </summary>
    internal Action<float> OnChange;

    /// <summary>Current animation value.</summary>
    private float AnimationCycle;

    /// <summary>
    /// Has 'no action defined' error message been debugged yet? This flag is used to prevent
    /// printing an error every single frame <see cref="Slider"/>'s value is changed (printing an
    /// error on the first frame only).
    /// </summary>
    private bool NoOnSliderErrorShown;

    /// <summary>Optional value when animation should stop (null if no stop time set).</summary>
    private float? StopValue;

    /// <summary>
    /// Flag used to make sure <see cref="Slider"/> is currently approaching <see
    /// cref="StopValue"/>.
    /// </summary>
    /// <remarks>
    /// This is used to make sure the <see cref="Slider"/> does not instantly stop if receives a
    /// <see cref="StopValue"/> that is less than the current value. Instead, the <see
    /// cref="Slider"/> must be less than the <see cref="StopValue"/> for at least one frame before
    /// can be considered to have passed the <see cref="StopValue"/>, and trigger a stop.
    /// </remarks>
    private bool CanStop;

    /// <summary>Is a starting value currently being set for <see cref="Slider"/>.</summary>
    /// <remarks>
    /// This flag is used to allow <see cref="TryOnChange"/> to skip trying to call
    /// <see cref="OnChange"/> while this value is set.
    /// </remarks>
    private bool SettingStartingValue;

    /// <summary>
    /// Setup <see cref="Slider"/>.
    /// </summary>
    private void Awake() {
      // Make sure we have a UI Slider to work with, printing an error if not.
      if (Slider == null) {
        Debug.LogError(ExampleErrors.MissingParameter(this, Slider, "Slider"));

        return;
      }

      // Connect changes in Slider's value to given action.
      Slider.onValueChanged.AddListener(TryOnChange);
    }

    /// <summary>Set the starting value to use for this <see cref="Slider"/>.</summary>
    /// <remarks>
    /// This will not trigger <see cref="OnChange"/> to be called (nor an error to be shown if
    /// <see cref="OnChange"/> has not yet been defined).
    /// </remarks>
    /// <param name="value">
    /// Starting value of <see cref="Slider"/> (assumed to be a valid 0f to 1f value).
    /// </param>
    internal void SetStartingValue(float value) {
      // Use a flag to prevent any Actions being called while the Slider's value is changed.
      SettingStartingValue = true;
      Slider.value = value;
      SettingStartingValue = false;
    }

    /// <summary>Start animating <see cref="Slider"/>.</summary>
    internal void StartAnimating() {
      // Only start animation coroutine if it has not already been started.
      AnimationCycle = 0f;

      if (!IsAnimating) {
        StartCoroutine(Animate());
      }
    }

    /// <summary>Start animating <see cref="Slider"/> at a given value.</summary>
    /// <param name="startValue">
    /// Value to start animation at (error printed if not between 0f and 1f inclusive).
    /// </param>
    internal void StartAnimatingAt(float startValue) {
      // Verify value is between 0f and 1f.
      if (startValue < 0f || startValue > 1f) {
        Debug.LogErrorFormat(
            "Invalid value of {0:N2} given to {1}.{2}.StartAnimatingAt function\n" +
                "Valid values are within the range 0f to 1f inclusive.\nDefaulting to 0f.",
            startValue,
            name,
            GetType());
        startValue = 0f;
      }

      // Set current value, and start animation coroutine if it has not already been started.
      AnimationCycle = startValue;

      if (!IsAnimating) {
        StartCoroutine(Animate());
      }
    }

    /// <summary>Stop animating <see cref="Slider"/>.</summary>
    internal void StopAnimating() {
      // Use flag to immediately stop Animate coroutine (if it is currently in progress).
      IsAnimating = false;
    }

    /// <summary>Stop animating <see cref="Slider"/> when reach a given value.</summary>
    /// <param name="stopValue">
    /// Value to stop animation at (error printed if not between 0f and 1f inclusive).
    /// </param>
    internal void StopAnimatingAt(float stopValue) {
      // Verify value is between 0f and 1f.
      if (stopValue < 0f || stopValue > 1f) {
        Debug.LogErrorFormat(
            "Invalid value of {0:N2} given to {1}.{2}.StopAnimatingAt function.\n" +
                "Valid values are within the range 0f to 1f inclusive.\nDefaulting to 1f.",
            stopValue,
            name,
            GetType());
        stopValue = 1f;
      }

      // Store value to stop animation at, and reset flag used to ensure animation does not
      // immediately stop if it has already passed this stop value.
      StopValue = stopValue;
      CanStop = false;
    }

    /// <summary>Animate slider moving.</summary>
    private IEnumerator Animate() {
      // Flag that animation is now in progress (prevents this Coroutine being redundant started
      // while it is already in progress).
      IsAnimating = true;
      float animationDirection = 1f;
      bool travellingUp = true;

      while (IsAnimating && !Input.anyKey) {
        // Use Time.smoothDeltaTime for slider movement to automatically smooth out changes in frame
        // rate.
        AnimationCycle += Time.smoothDeltaTime * animationDirection / AnimationTime;

        // If using back and forth animation, then when we reach top of slider, start moving towards
        // bottom (and vice versa).
        float animationPercent;

        if (BackAndForth) {
          if (AnimationCycle >= 1f) {
            animationDirection = -1f;
            animationPercent = 1f;
            travellingUp = false;
          } else if (AnimationCycle <= 0f) {
            animationDirection = 1f;
            animationPercent = 0f;
            travellingUp = true;
          } else {
            // When between top and bottom, optionally apply smoothing to ease the transition
            // between going up and down (and vice versa).
            animationPercent = TrySmooth(AnimationCycle);
          }

          // See if animation should be stopped this frame, adjusting animation value if so. Exactly
          // how this is measured is based on the direction the slider is currently travelling (up
          // or down).
          animationPercent =
              travellingUp ? TryStopIfAbove(animationPercent) : TryStopIfBelow(animationPercent);
        } else {
          // For regular (non back and forth) animation, optionally smoothing in between.
          if (AnimationCycle > 1f) {
            AnimationCycle %= 1f;
          }

          animationPercent = TrySmooth(AnimationCycle);

          // See if animation should be stopped this frame, adjusting animation value if so.
          animationPercent = TryStopIfAbove(animationPercent);
        }

        Slider.value = animationPercent;

        // Wait for next frame, at which point animation will stop if any input is received from the
        // player.
        yield return null;
      }
    }

    /// <summary>
    /// See if should stop animation this frame, adjusting given value if have just past stop value
    /// by going downwards, leaving value as is otherwise.
    /// </summary>
    /// <remarks>This version is used when the <see cref="Slider"/>'s value is ascending.</remarks>
    private float TryStopIfAbove(float value) {
      // If a stop value has been given, see if have passed it yet.
      if (StopValue.HasValue) {
        // Make sure that animation has been below Stop Value for at least one frame. This check
        // prevents the animation from stopping immediately if a Stop Value is given that is less
        // than the current value, waiting instead until the moment the value passes the Stop Value
        // before stopping the animation.
        if (CanStop) {
          // If have just past Stop Value, clamp to exactly this value and stop animation.
          if (value >= StopValue.Value) {
            value = StopValue.Value;
            StopValue = null;
            CanStop = false;
            IsAnimating = false;
          }
        } else {
          CanStop = value < StopValue.Value;
        }
      }

      return value;
    }

    /// <summary>
    /// See if should stop animation this frame, adjusting given value if have just past stop value
    /// by going upwards, leaving value as is otherwise.
    /// </summary>
    /// <remarks>This version is used when the <see cref="Slider"/>'s value is descending.</remarks>
    private float TryStopIfBelow(float value) {
      // If a stop value has been given, see if have passed it yet.
      if (StopValue.HasValue) {
        // Make sure that animation has been above Stop Value for at least one frame. This check
        // prevents the animation from stopping immediately if a Stop Value is given that is greater
        // than the current value, waiting instead until the moment the value passes the Stop Value
        // before stopping the animation.
        if (CanStop) {
          // If have just past Stop Value, clamp to exactly this value and stop animation.
          if (value <= StopValue.Value) {
            value = StopValue.Value;
            StopValue = null;
            CanStop = false;
            IsAnimating = false;
          }
        } else {
          CanStop = value > StopValue.Value;
        }
      }

      return value;
    }

    /// <summary>
    /// Optionally apply smoothing to a given value (if <see cref="Smooth"/> is enabled).
    /// </summary>
    private float TrySmooth(float value) {
      return Smooth ? (Mathf.Sin((value - 0.5f) * Mathf.PI) + 1f) / 2f : value;
    }

    /// <summary>Respond to <see cref="Slider"/> changing value.</summary>
    private void TryOnChange(float value) {
      // Skip trying to call Action/s if the change in value was in response to the user setting a
      // starting value for the Slider.
      if (SettingStartingValue) {
        return;
      }

      // If the change was not caused by setting a starting value, confirm an Action has been given
      // to call on value changes.
      if (OnChange == null) {
        // If no Action is defined, only show an error once, skipping further errors. This avoids
        // error spamming every frame the Slider's value is changed by the user.
        if (!NoOnSliderErrorShown) {
          Debug.LogErrorFormat(
              "No OnSlider Action set for {0}.{1}, so cannot inform other classes " +
                  "of new slider value of {2:N2}.",
              name,
              GetType(),
              value);
          NoOnSliderErrorShown = true;
        }
      } else {
        OnChange(value);
      }
    }
  }
}
