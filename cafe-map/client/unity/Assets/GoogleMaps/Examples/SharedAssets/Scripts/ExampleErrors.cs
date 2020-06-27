using UnityEngine;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Class for generating and returning specific kinds of error messages that are frequently used
  /// in the Maps SDK for Unity example scenes.
  /// </summary>
  public static class ExampleErrors {
    /// <summary>
    /// Returns a <see cref="string"/> error message saying that a needed parameter was not defined
    /// on this <see cref="GameObject"/>.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="missingParameter">Null/undefined parameter.</param>
    /// <param name="missingParameterName">
    /// Name of <see cref="missingParameter"/> in calling script. Used in generating error message.
    /// </param>
    /// <param name="context">
    /// Optional reason why <see cref="missingParameter"/> was required. Used in generating error
    /// message, specifically to say that this script needs this parameter [reasonRequired]. By
    /// default this is set to the generic reason 'to operate'.
    /// </param>
    /// <typeparam name="T">
    /// The type of <see cref="missingParameter"/>. Used in generating error message.
    /// </typeparam>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string MissingParameter<T>(
        MonoBehaviour script, T missingParameter, string missingParameterName,
        string context = "to operate") {
      // Note: 'name' and 'GetType()' just give the name of the GameObject this script is on, and
      // the name of this script respectively.
      return string.Format(
          "No {0} defined for {1}.{2}, which needs a {3} {4}.",
          missingParameterName,
          script.name,
          script.GetType(),
          typeof(T),
          context);
    }

    /// <summary>
    /// Returns a <see cref="string"/> error message saying that an array of parameters needed by
    /// this <see cref="GameObject"/> had no elements in it.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="emptyArray">Empty array.</param>
    /// <param name="emptyArrayName">
    /// Name of <see cref="emptyArray"/> in calling script. Used in generating error message.
    /// </param>
    /// <param name="context">
    /// Optional reason why <see cref="emptyArray"/> should not have been empty. Used in generating
    /// error message, specifically to say that this script needs this parameter [reasonRequired].
    /// By default this is set to the generic reason 'to operate'.
    /// </param>
    /// <typeparam name="T">
    /// The type of elements contained in <see cref="emptyArray"/>. Used in generating error
    /// message.
    /// </typeparam>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string EmptyArray<T>(
        MonoBehaviour script, T[] emptyArray, string emptyArrayName,
        string context = "to operate") {
      return string.Format(
          "Empty {0} array defined for {1}.{2}, which needs a non-empty array of {3}s {4}.",
          emptyArrayName,
          script.name,
          script.GetType(),
          typeof(T),
          context);
    }

    /// <summary>
    /// Returns a <see cref="string"/> error message saying that an array of parameters needed by
    /// this <see cref="GameObject"/> contained at least one null element.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="array">Array containing null element (assumed to not be a null array).</param>
    /// <param name="arrayName">
    /// Name of <see cref="array"/> in calling script. Used in generating error message.
    /// </param>
    /// <param name="index">Index of null element in <see cref="array"/>.</param>
    /// <typeparam name="T">
    /// The type of elements contained in <see cref="array"/>. Used in generating error message.
    /// </typeparam>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string NullArrayElement<T>(
        MonoBehaviour script, T[] array, string arrayName, int index) {
      return string.Format(
          "Null {0} defined for {1}.{2}, specifically {3} {4} of {5}).",
          arrayName,
          script.name,
          script.GetType(),
          typeof(T),
          index,
          array.Length);
    }

    /// <summary>
    /// Return an error message saying that a given index was not valid for a given array.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="index">Invalid index.</param>
    /// <param name="array">Array <see cref="index"/> is for.</param>
    /// <param name="arrayName">
    /// Name of <see cref="array"/> in calling scripts. Used in generating error message.
    /// </param>
    /// <typeparam name="T">
    /// The type of elements contained in <see cref="array"/>. Used in generating error message.
    /// </typeparam>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string InvalidArrayIndex<T>(
        MonoBehaviour script, T[] array, string arrayName, int index) {
      return string.Format(
          "{0} index of {1} given to {2}.{3} for use in {4} array.\nValid indices " +
              "are in the range 0 to {5} based on {6} stored {7}s in {3}'s {4} array.",
          index < 0 ? "Negative" : "Invalid",
          index,
          script.name,
          script.GetType(),
          arrayName,
          array.Length - 1,
          array.Length,
          typeof(T));
    }

    /// <summary>
    /// Return an error message saying that no <see cref="Camera.main"/> was found in the scene -
    /// in other words, no <see cref="GameObject"/> was tagged as 'MainCamera'.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="context">
    /// Optional reason why a non-null value for <see cref="Camera.main"/> was required. Used in
    /// generating error message, specifically to say that this script needs a value for
    /// <see cref="Camera.main"/> [reasonRequired]. By default this is set to the generic reason
    /// 'to operate'.
    /// </param>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>v
    public static string NullMainCamera(MonoBehaviour script, string context = "to operate") {
      return string.Format(
          "No Camera.main found in the scene.\nPlease make sure at " +
              "least one in-scene Camera is tagged as \"MainCamera\" in order for {0}.{1} {2}.",
          script.name,
          script.GetType(),
          context);
    }

    /// <summary>
    /// Return an error message saying a given <see cref="float"/> value that needed to be positive,
    /// was not.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="value">Value that should have been positive.</param>
    /// <param name="valueName">
    /// Name of <see cref="value"/> in calling script. Used for generating error message.
    /// </param>
    /// <param name="context">
    /// Optional reason why a positive <see cref="value"/> was required. Used in generating error
    /// message, specifically to say that this script needs this value to be positive
    /// [reasonRequired]. By default this is set to the generic reason 'to operate.'
    /// </param>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string NotGreaterThanZero(
        MonoBehaviour script, float value, string valueName, string context = "to operate") {
      return NotGreaterThan(script, value, valueName, 0f, context);
    }

    /// <summary>
    /// Return an error message saying a given <see cref="float"/> value that needed to be greater
    /// than a given minimum, was not.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="value">
    /// Value that should have been greater than given <see cref="minimum"/>.
    /// </param>
    /// <param name="valueName">
    /// Name of <see cref="value"/> in calling script. Used for generating error message.
    /// </param>
    /// <param name="minimum">Exclusive minimum <see cref="value"/>.</param>
    /// <param name="minimumName">
    /// Optional name of <see cref="minimum"/> in calling script. Used for generating error message.
    /// If left null, error message will assume given <see cref="minimum"/> is a constant value and
    /// will refer to it as such.
    /// </param>
    /// <param name="context">
    /// Optional reason why a <see cref="value"/> larger than given <see cref="minimum"/> was
    /// required. Used in generating error message, specifically to say this this script needs this
    /// value to be greater than the given minimum [reasonRequired]. By default this is set to the
    /// generic reason 'to operate.'
    /// </param>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string NotGreaterThan(
        MonoBehaviour script, float value, string valueName, float minimum,
        string minimumName = null, string context = "to operate") {
      return string.Format(
          "{0} given to {1}.{2}, which needs a {3} greater than {4} {5}.",
          IsZero(value)
              ? string.Concat(valueName, " of zero")
              : string.Format(
                    "{0} {1} of {2}", IsZero(minimum) ? "Negative" : "Too small", valueName, value),
          script.name,
          script.GetType(),
          valueName,
          string.IsNullOrEmpty(minimumName)
              ? minimum.ToString()
              : string.Format("given {0} of {1}", minimumName, minimum),
          context);
    }

    /// <summary>
    /// Return a <see cref="string"/> error message saying a given <see cref="int"/> value that
    /// needed to be within a given range, was not.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="value">Value that should have been inside a given range.</param>
    /// <param name="valueName">
    /// Name of <see cref="value"/> in calling script. Used for generating error message.
    /// </param>
    /// <param name="minimum">Inclusive minimum <see cref="value"/>.</param>
    /// <param name="maximum">Inclusive maximum <see cref="value"/>.</param>
    /// <param name="context">
    /// Optional reason why the given <see cref="value"/> needed to be within the given
    /// <see cref="minimum"/> to <see cref="maximum"/> range. Used in generating error message,
    /// specifically to say this this script needs this value to be positive [reasonRequired]. By
    /// default this is set to the generic reason 'to operate.'
    /// </param>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string OutsideRange(
        MonoBehaviour script, int value, string valueName, int minimum, int maximum,
        string context = "to operate") {
      return string.Format(
          "{0} {1} of {2} given to {3}.{4}, which needs a valid {1} (within the " +
              "range of {5} to {6}) {7}.",
          value < minimum ? minimum == 0 ? "Negative" : "Too small"
                          : maximum == 0 ? "Positive" : "Too large",
          valueName,
          value,
          script.name,
          script.GetType(),
          minimum,
          maximum,
          context);
    }

    /// <summary>
    /// Return a <see cref="string"/> error message saying a given <see cref="float"/> value that
    /// needed to be within a given range, was not.
    /// </summary>
    /// <remarks>
    /// This method returns a <see cref="string"/> error message for use with <see cref="Debug"/>.
    /// <para>
    /// The error message is returned (instead of being shown using <see cref="Debug"/> within
    /// this <see cref="ExampleErrors"/> class), so that if the user double clicks on the error
    /// message in the Unity console, Unity will open the script that generated the error (the
    /// script calling this <see cref="ExampleErrors"/> class).
    /// </para></remarks>
    /// <param name="script">
    /// The script calling this code (can be added using the alias: <code>this</code>). Assumed to
    /// not be null.
    /// </param>
    /// <param name="value">Value that should have been inside a given range.</param>
    /// <param name="valueName">
    /// Name of <see cref="value"/> in calling script. Used for generating error message.
    /// </param>
    /// <param name="minimum">Inclusive minimum <see cref="value"/>.</param>
    /// <param name="maximum">Inclusive maximum <see cref="value"/>.</param>
    /// <param name="context">
    /// Optional reason why the given <see cref="value"/> needed to be within the given
    /// <see cref="minimum"/> to <see cref="maximum"/> range. Used in generating error message,
    /// specifically to say this this script needs this value to be positive [reasonRequired]. By
    /// default this is set to the generic reason 'to operate.'
    /// </param>
    /// <returns>Error message for displaying with <code>Debug.LogError</code>.</returns>
    public static string OutsideRange(
        MonoBehaviour script, float value, string valueName, float minimum, float maximum,
        string context = "to operate") {
      return string.Format(
          "{0} {1} of {2} given to {3}.{4}, which needs a valid {1} (within the " +
              "range of {5} to {6}) {7}.",
          value < minimum ? IsZero(minimum) ? "Negative" : "Too small"
                          : IsZero(maximum) ? "Positive" : "Too large",
          valueName,
          value,
          script.name,
          script.GetType(),
          minimum,
          maximum,
          context);
    }

    /// <summary>
    /// Return whether a given <see cref="float"/> is equal to zero (within range of
    /// <see cref="float"/> rounding errors).
    /// </summary>
    /// <param name="value"><see cref="float"/> to check.</param>
    /// <returns>
    /// Whether or not the absolute value of this <see cref="float"/> is less than
    /// <see cref="float.Epsilon"/>.
    /// </returns>
    private static bool IsZero(float value) {
      return Mathf.Abs(value) < float.Epsilon;
    }
  }
}
