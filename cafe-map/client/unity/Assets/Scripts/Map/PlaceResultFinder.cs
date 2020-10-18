using System;
using System.Collections;
using System.Collections.Generic;
using CafeMap.Map;
using CafeMap.Player.Services;
using Cysharp.Threading.Tasks;
using Cysharp.Threading.Tasks.Triggers;
using Google.Maps;
using UnityEngine;
using UnityEngine.InputSystem;
using Zenject;

public class PlaceResultFinder : MonoBehaviour
{

    private PlacePin result;

    private void Update()
    {
        if (result != null)
        {
            result.Select();
            result = null;
            return;
        }

        if (Mouse.current.leftButton.wasPressedThisFrame)
        {
            Ray ray = Camera.main.ScreenPointToRay(Mouse.current.position.ReadValue());
            RaycastHit hit;

            if (Physics.Raycast(ray, out hit, 800))
            {
                result = hit.transform.gameObject.GetComponent<PlacePin>();
            }
        }
    }
}
