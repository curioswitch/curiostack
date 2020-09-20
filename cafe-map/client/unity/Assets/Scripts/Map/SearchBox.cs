using System;
using UnityEngine;
using UnityEngine.UI;

namespace CafeMap.Map
{
    public class SearchBox : MonoBehaviour
    {

        private InputField input;

        private void Start()
        {
            input = GetComponent<InputField>();
            
            input.onEndEdit.AddListener((text) =>
            {
                Debug.Log(text);
            });
        }
    }
}