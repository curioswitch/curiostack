using System;
using UnityEngine;
using UnityEngine.UI;

namespace CafeMap.Map
{
    public class OssLicenses : MonoBehaviour
    {
        [SerializeField] private GameObject licensesView;

        private void Awake()
        {
            GetComponentInChildren<Button>().onClick.AddListener(() =>
            {
                licensesView.SetActive(!licensesView.activeSelf);
            });
        }
    }
}
