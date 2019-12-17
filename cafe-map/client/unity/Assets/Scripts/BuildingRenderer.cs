using System;
using System.Collections;
using System.Collections.Generic;
using Google.Maps.Feature;
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
            switch (args.MapFeature.Metadata.Usage)
            {
                case StructureMetadata.UsageType.Unspecified:
                    break;
                case StructureMetadata.UsageType.Bar:
                    break;
                case StructureMetadata.UsageType.Bank:
                    break;
                case StructureMetadata.UsageType.Lodging:
                    break;
                case StructureMetadata.UsageType.Cafe:
                    break;
                case StructureMetadata.UsageType.Restaurant:
                    break;
                case StructureMetadata.UsageType.EventVenue:
                    break;
                case StructureMetadata.UsageType.TouristDestination:
                    break;
                case StructureMetadata.UsageType.Shopping:
                    break;
                case StructureMetadata.UsageType.School:
                    break;
                default:
                    throw new ArgumentOutOfRangeException();
            }
        });
    }

    // Update is called once per frame
    void Update()
    {
        
    }
}
