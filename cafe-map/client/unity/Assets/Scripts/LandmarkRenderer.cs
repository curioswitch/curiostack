using System;
using Google.Maps;
using Google.Maps.Coord;
using Google.Maps.Event;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature;
using UnityEngine;

[RequireComponent(typeof(MapsService))]
public class LandmarkRenderer : MonoBehaviour
{
    // Start is called before the first frame update
    void Start()
    {
        var mapsService = GetComponent<MapsService>();
        mapsService.InitFloatingOrigin(new LatLng(35.4710404,139.6197189));
        
        mapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(args =>
        {
            switch (args.MapFeature.Metadata.Usage)
            {
                case StructureMetadata.UsageType.Bank:
                    break;
                case StructureMetadata.UsageType.Bar:
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
                case StructureMetadata.UsageType.Unspecified:
                default:
                    break;
            }
        });
        
        mapsService.LoadMap(ExampleDefaults.DefaultBounds, ExampleDefaults.DefaultGameObjectOptions);
    }

    // Update is called once per frame
    void Update()
    {
        
    }
}
