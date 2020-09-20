using System.Collections;
using System.Collections.Generic;
using Google.Maps;
using UnityEngine;

public class KeysInitializer : MonoBehaviour
{

    private static string googleApiKey;

    public static string GoogleApiKey => googleApiKey;

    // Start is called before the first frame update
    void Awake()
    {
        googleApiKey = readSecret("google-api-key");
        foreach (var map in GameObject.FindObjectsOfType<MapsService>())
        {
            map.ApiKey = googleApiKey;
        }
    }

    private static string readSecret(string filename)
    {
        var file = Resources.Load<TextAsset>($"Secrets/{filename}");
        return file.text.Trim();
    }
}
