using System;
using System.Collections.Generic;
using CafeMap.Map;
using UnityEditor;
using UnityEngine;
using Object = UnityEngine.Object;

namespace CafeMap.Editor
{
    [InitializeOnLoad]
    public static class WorldPositionableSaver
    {
        private const string ppKey = "WorldPositionableSaverSerializationClipboard";

        static WorldPositionableSaver()
        {
            EditorApplication.playModeStateChanged += onChangePlayModeState;
        }

        private static void onChangePlayModeState(PlayModeStateChange playModeStateChange)
        {
            if (playModeStateChange == PlayModeStateChange.ExitingPlayMode)
            {
                save();
            } else if (playModeStateChange == PlayModeStateChange.EnteredEditMode)
            {
                load();
            }
        }

        private static void save()
        {

            string json = serializePositionables();
            EditorPrefs.SetString(ppKey, json);
        }

        private static void load()
        {
            if (!EditorPrefs.HasKey(ppKey))
            {
                return;
            }

            string json = EditorPrefs.GetString(ppKey);
            EditorPrefs.DeleteKey(ppKey);

            // It's faster to check for differences than to actually restore, so we do that first.
            if (json == serializePositionables())
            {
                return;
            }

            SerializedWorldPositionables positionables = new SerializedWorldPositionables();
            EditorJsonUtility.FromJsonOverwrite(json, positionables);

            var positionableInstances = new Dictionary<int, SerializedWorldPositionable>();
            foreach (var positionable in positionables.Positionables)
            {
                positionableInstances[positionable.InstanceID] = positionable;
            }

            foreach (var positionable in Object.FindObjectsOfType<WorldPositionable>())
            {
                SerializedWorldPositionable saved;
                if (positionableInstances.TryGetValue(positionable.GetInstanceID(), out saved))
                {
                    positionable.Latitude = saved.Latitude;
                    positionable.Longitude = saved.Longitude;
                    var transform = positionable.transform;
                    transform.rotation = saved.Rotation;
                    transform.localScale = saved.Scale;
                }
            }
        }

        private static string serializePositionables()
        {
            List<SerializedWorldPositionable> allSerialized = new List<SerializedWorldPositionable>();
            foreach (var positionable in Object.FindObjectsOfType<WorldPositionable>())
            {
                var serialized = new SerializedWorldPositionable();
                serialized.Latitude = positionable.Latitude.ToString();
                serialized.Longitude = positionable.Longitude.ToString();
                serialized.Rotation = positionable.transform.rotation;
                serialized.Scale = positionable.transform.localScale;
                serialized.InstanceID = positionable.GetInstanceID();
                allSerialized.Add(serialized);
            }

            return EditorJsonUtility.ToJson(new SerializedWorldPositionables
            {
                Positionables = allSerialized,
            });
        }

        [Serializable]
        public class SerializedWorldPositionable
        {
            public string Latitude;
            public string Longitude;
            public Quaternion Rotation;
            public Vector3 Scale;
            public int InstanceID;
        }

        [Serializable]
        public class SerializedWorldPositionables
        {
            public List<SerializedWorldPositionable> Positionables;
        }
    }
}
