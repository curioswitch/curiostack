using System.Collections;
using System.Collections.Generic;
using UnityEngine;

[RequireComponent(typeof(DynamicMapsService))]
public class BuildingRenderer : MonoBehaviour
{
    // Start is called before the first frame update
    void Start()
    {
        DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();
        dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(args =>
        {
        });
    }

    // Update is called once per frame
    void Update()
    {
        
    }
}
